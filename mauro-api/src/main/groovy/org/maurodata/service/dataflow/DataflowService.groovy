package org.maurodata.service.dataflow

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.micronaut.core.annotation.Nullable
import io.micronaut.http.HttpStatus
import io.micronaut.http.annotation.Body
import io.micronaut.http.server.multipart.MultipartBody
import jakarta.inject.Inject
import org.maurodata.ErrorHandler
import org.maurodata.domain.dataflow.DataElementComponent
import org.maurodata.domain.dataflow.DataFlow
import org.maurodata.domain.datamodel.DataClass
import org.maurodata.domain.datamodel.DataElement
import org.maurodata.domain.datamodel.DataModel
import org.maurodata.domain.folder.Folder
import org.maurodata.domain.model.AdministeredItem
import org.maurodata.domain.model.ModelItem
import org.maurodata.domain.security.Role
import org.maurodata.persistence.ContentsService
import org.maurodata.persistence.cache.ModelCacheableRepository
import org.maurodata.persistence.datamodel.DataModelContentRepository
import org.maurodata.plugin.MauroPluginService
import org.maurodata.plugin.exporter.ModelItemExporterPlugin
import org.maurodata.plugin.importer.ImportParameters
import org.maurodata.plugin.importer.ModelItemImporterPlugin
import org.maurodata.security.AccessControlService
import org.maurodata.service.core.AdministeredItemService
import org.maurodata.service.path.PathService
import org.maurodata.service.plugin.PluginService
import org.maurodata.util.PathStringUtils
import org.maurodata.utils.importer.ImporterUtils

@CompileStatic
@Slf4j
class DataflowService extends AdministeredItemService {

    AccessControlService accessControlService
    ModelCacheableRepository.FolderCacheableRepository folderRepository
    ModelCacheableRepository.DataModelCacheableRepository dataModelRepository
    MauroPluginService mauroPluginService
    PathService pathService
    DataModelContentRepository dataModelContentRepository
    ImporterUtils importerUtils

    @Inject
    ContentsService contentsService

    @Inject
    DataflowService(AccessControlService accessControlService, ModelCacheableRepository.FolderCacheableRepository folderRepository,
                    ModelCacheableRepository.DataModelCacheableRepository dataModelRepository, MauroPluginService mauroPluginService, PathService pathService,
                    DataModelContentRepository dataModelContentRepository, ImporterUtils importerUtils) {
        this.accessControlService = accessControlService
        this.folderRepository = folderRepository
        this.dataModelRepository = dataModelRepository
        this.mauroPluginService = mauroPluginService
        this.pathService = pathService
        this.dataModelContentRepository = dataModelContentRepository
        this.importerUtils = importerUtils
    }

    ModelItemExporterPlugin getModelItemExporterPlugin(String namespace, String name, String version) {
        ModelItemExporterPlugin mauroPlugin = mauroPluginService.getPlugin(ModelItemExporterPlugin, namespace, name, version)
        PluginService.handlePluginNotFound(mauroPlugin, namespace, name)
        mauroPlugin
    }

    List<ModelItem> importModelItem(Class aClazz, DataModel target, @Body MultipartBody body, String namespace, String name, @Nullable String version) {
        ModelItemImporterPlugin mauroPlugin = mauroPluginService.getPlugin(aClazz, namespace, name, version) as ModelItemImporterPlugin
        PluginService.handlePluginNotFound(mauroPlugin, namespace, name)

        ImportParameters importParameters = importerUtils.readFromMultipartFormBody(body as MultipartBody, mauroPlugin.importParametersClass())

        if (importParameters.folderId == null) {
            ErrorHandler.handleErrorOnNullObject(HttpStatus.NOT_FOUND, importParameters.folderId, "Please choose the folder into which the Model/s should be imported.")
        }
        if (importParameters.sourceDataModelId == null) {
            ErrorHandler.handleErrorOnNullObject(HttpStatus.NOT_FOUND, importParameters.sourceDataModelId,
                                                 "Please choose the source dataModel into which the DataFlow ModelItem/s should be imported.")
        }
        List<ModelItem> imported = (List<ModelItem>) mauroPlugin.importModelItem(importParameters)

        pathRepository.readParentItems(target)
        target.updatePath()

        DataModel source = dataModelRepository.loadWithContent(importParameters.sourceDataModelId)
        ErrorHandler.handleErrorOnNullObject(HttpStatus.BAD_REQUEST, source, "Datamodel with id $importParameters.sourceDataModelId not found")
        accessControlService.checkRole(Role.EDITOR, source)

        pathRepository.readParentItems(source)
        source.updatePath()

        Folder folder = folderRepository.readById(importParameters.folderId)
        ErrorHandler.handleErrorOnNullObject(HttpStatus.NOT_FOUND, folder, "Folder with id $importParameters.folderId not found")
        accessControlService.checkRole(Role.EDITOR, folder)

        pathRepository.readParentItems(folder)
        folder.updatePath()

        //read in all modelitems under target
        target = dataModelRepository.loadWithContent(target.id) as DataModel

        imported.each {imp ->
            imp.folder = folder
            (imp as DataFlow).source = source
            (imp as DataFlow).target = target
            (imp as DataFlow).dataClassComponents.each {
                it.sourceDataClasses = getImportDataClass(it.sourceDataClasses, source)
                it.targetDataClasses = getImportDataClass(it.targetDataClasses, target)
                it.dataElementComponents = findImportDataElements(it.dataElementComponents, source, target)
            }
            updateCreationProperties(imp)
        }
        imported
    }

    @Override
    AdministeredItem updatePaths(AdministeredItem dataFlow) {
        updateDerivedProperties(dataFlow)
        (dataFlow as DataFlow).dataClassComponents.each {
            updateDerivedProperties(it)
            it.sourceDataClasses.each {sourceDataClass ->
                updateDerivedProperties(sourceDataClass)
            }
            it.targetDataClasses.each {targetDataClass ->
                updateDerivedProperties(targetDataClass)
            }
            it.dataElementComponents.each {dataElementComponent ->
                updateDerivedProperties(dataElementComponent)
                dataElementComponent.sourceDataElements.each {sourceDataElement ->
                    updateDerivedProperties(sourceDataElement)
                }
                dataElementComponent.targetDataElements.each {targetDataElement ->
                    updateDerivedProperties(targetDataElement)
                }
            }
        }
        dataFlow
    }

    protected List<DataClass> getImportDataClass(List<DataClass> dataClasses, DataModel model) {
        return dataClasses.collect {dC ->
            String resourceLabel = PathStringUtils.getItemSubPath(dC.pathPrefix, dC.path.pathString)
            DataClass importDataClass = findImportDataClassByLabel(resourceLabel, model)
            pathRepository.readParentItems(importDataClass) //perhaps not necessary to cal the full path but doing it anyway
            importDataClass.updatePath()
            importDataClass
        }
    }

    protected DataClass findImportDataClassByLabel(String resourceLabel, DataModel model) {
        List<DataClass> importDataClasses = model.allDataClasses.findAll {it.label == resourceLabel} as List<DataClass>
        if (importDataClasses.isEmpty()) {
            ErrorHandler.handleError(HttpStatus.UNPROCESSABLE_ENTITY, "No dataclass found with label $resourceLabel in import target datamodel $model.id")
        }
        if (importDataClasses.size() > 1) {
            log.warn("Found more than 1 match in model: $model.id on label $resourceLabel. Returning first: ${importDataClasses.first().id}")
        }
        importDataClasses.first()
    }

    protected List<DataElementComponent> findImportDataElements(List<DataElementComponent> dataElementComponents, DataModel source, DataModel target) {
        dataElementComponents.collect {
            it.sourceDataElements = getImportDataElements(it.sourceDataElements, source)
            it.targetDataElements = getImportDataElements(it.targetDataElements, target)
        }
        dataElementComponents
    }

    protected List<DataElement> getImportDataElements(List<DataElement> dataElements, DataModel model) {
        return dataElements.collect {dE ->
            String resourceLabel = PathStringUtils.getItemSubPath(dE.pathPrefix, dE.path.pathString)
            List<DataElement> importElementsWithSameLabel = model.dataElements.findAll {
                it.label == resourceLabel
            }
            if (importElementsWithSameLabel.isEmpty()) {
                ErrorHandler.handleError(HttpStatus.UNPROCESSABLE_ENTITY, "No dataElement found with label $resourceLabel in import target datamodel $model.id")
            }
            if (importElementsWithSameLabel.size() > 1) {
                log.warn("Found more than 1 match in model: $model.id on label $resourceLabel. Returning first: ${importElementsWithSameLabel.first().id}")
            }
            importElementsWithSameLabel.first()
        }
    }

}

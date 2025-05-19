package uk.ac.ox.softeng.mauro.controller.datamodel

import uk.ac.ox.softeng.mauro.ErrorHandler
import uk.ac.ox.softeng.mauro.api.Paths
import uk.ac.ox.softeng.mauro.api.datamodel.DataClassApi
import uk.ac.ox.softeng.mauro.audit.Audit
import uk.ac.ox.softeng.mauro.controller.model.AdministeredItemController
import uk.ac.ox.softeng.mauro.domain.datamodel.DataClass
import uk.ac.ox.softeng.mauro.domain.datamodel.DataElement
import uk.ac.ox.softeng.mauro.domain.datamodel.DataModel
import uk.ac.ox.softeng.mauro.domain.datamodel.DataType
import uk.ac.ox.softeng.mauro.domain.model.AdministeredItem
import uk.ac.ox.softeng.mauro.domain.security.Role
import uk.ac.ox.softeng.mauro.persistence.cache.AdministeredItemCacheableRepository
import uk.ac.ox.softeng.mauro.persistence.cache.AdministeredItemCacheableRepository.DataClassCacheableRepository
import uk.ac.ox.softeng.mauro.persistence.cache.ModelCacheableRepository.DataModelCacheableRepository
import uk.ac.ox.softeng.mauro.persistence.datamodel.DataClassContentRepository
import uk.ac.ox.softeng.mauro.persistence.datamodel.DataModelContentRepository
import uk.ac.ox.softeng.mauro.web.ListResponse

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.NonNull
import io.micronaut.core.annotation.Nullable
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Delete
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Post
import io.micronaut.http.annotation.Put
import io.micronaut.security.annotation.Secured
import io.micronaut.security.rules.SecurityRule
import io.micronaut.transaction.annotation.Transactional
import jakarta.inject.Inject

@CompileStatic
@Controller
@Secured(SecurityRule.IS_ANONYMOUS)
class DataClassController extends AdministeredItemController<DataClass, DataModel> implements DataClassApi {

    DataClassCacheableRepository dataClassRepository

    @Inject
    DataModelCacheableRepository dataModelRepository

    DataModelContentRepository dataModelContentRepository
    DataClassContentRepository dataClassContentRepository

    @Inject
    AdministeredItemCacheableRepository.DataElementCacheableRepository dataElementCacheableRepository
    @Inject
    AdministeredItemCacheableRepository.DataTypeCacheableRepository dataTypeCacheableRepository

    DataClassController(DataClassCacheableRepository dataClassRepository, DataModelCacheableRepository dataModelRepository,
                        DataModelContentRepository dataModelContentRepository,
                        DataClassContentRepository dataClassContentRepository) {
        super(DataClass, dataClassRepository, dataModelRepository, dataModelContentRepository)
        this.dataModelRepository = dataModelRepository
        this.dataClassRepository = dataClassRepository
        this.dataModelContentRepository = dataModelContentRepository
        this.dataClassContentRepository = dataClassContentRepository
    }

    @Audit
    @Get(Paths.DATA_CLASS_ID)
    DataClass show(UUID dataModelId, UUID id) {
        super.show(id)
    }

    @Audit
    @Post(Paths.DATA_CLASS_LIST)
    DataClass create(UUID dataModelId, @Body @NonNull DataClass dataClass) {
        super.create(dataModelId, dataClass)
    }

    @Audit
    @Put(Paths.DATA_CLASS_ID)
    DataClass update(UUID dataModelId, UUID id, @Body @NonNull DataClass dataClass) {
        super.update(id, dataClass)
    }

    @Audit(
        parentDomainType = DataModel,
        parentIdParamName = 'dataModelId',
        deletedObjectDomainType = DataClass
    )
    @Delete(Paths.DATA_CLASS_ID)
    HttpResponse delete(UUID dataModelId, UUID id, @Body @Nullable DataClass dataClass) {
        super.delete(id, dataClass)
    }

    @Audit
    @Get(Paths.DATA_CLASS_LIST)
    ListResponse<DataClass> list(UUID dataModelId) {
        DataModel dataModel = dataModelRepository.readById(dataModelId)
        accessControlService.checkRole(Role.READER, dataModel)
        List<DataClass> classes = dataClassRepository.readAllByDataModelAndParentDataClassIsNull(dataModel)
        classes.each {
            updateDerivedProperties(it)
        }
        ListResponse.from(classes)
    }

    @Audit
    @Get(Paths.DATA_CLASS_CHILD_DATA_CLASS_ID)
    DataClass show(UUID dataModelId, UUID parentDataClassId, UUID id) {
        super.show(id)
    }

    @Audit
    @Post(Paths.DATA_CLASS_CHILD_DATA_CLASS_LIST)
    DataClass create(UUID dataModelId, UUID parentDataClassId, @Body @NonNull DataClass dataClass) {

        cleanBody(dataClass)
        DataModel dataModel = dataModelRepository.readById(dataModelId)
        accessControlService.checkRole(Role.EDITOR, dataModel)
        DataClass parentDataClass = dataClassRepository.readById(parentDataClassId)
        accessControlService.checkRole(Role.EDITOR, parentDataClass)
        dataClass.parentDataClass = parentDataClass
        createEntity(dataModel, dataClass)
        return dataClass
    }

    @Audit
    @Put(Paths.DATA_CLASS_CHILD_DATA_CLASS_ID)
    DataClass update(UUID dataModelId, UUID parentDataClassId, UUID id, @Body @NonNull DataClass dataClass) {
        super.update(id, dataClass)
    }

    @Audit(
        parentDomainType = DataClass,
        parentIdParamName = 'parentDataClassId',
        deletedObjectDomainType = DataClass
    )
    @Delete(Paths.DATA_CLASS_CHILD_DATA_CLASS_ID)
    HttpResponse delete(UUID dataModelId, UUID parentDataClassId, UUID id, @Body @Nullable DataClass dataClass) {
        super.delete(id, dataClass)
    }

    @Audit
    @Get(Paths.DATA_CLASS_CHILD_DATA_CLASS_LIST)
    ListResponse<DataClass> list(UUID dataModelId, UUID parentDataClassId) {
        DataClass parentDataClass = dataClassRepository.readById(parentDataClassId)
        accessControlService.checkRole(Role.READER, parentDataClass)
        ListResponse.from(dataClassRepository.readAllByParentDataClass_Id(parentDataClassId))

    }

    @Audit
    @Put(Paths.DATA_CLASS_EXTENDS)
    DataClass createExtension(UUID dataModelId, UUID id, UUID otherModelId, UUID otherClassId) {
        DataClass sourceDataClass = dataClassRepository.readById(id)
        accessControlService.checkRole(Role.EDITOR, sourceDataClass)
        DataClass targetDataClass = dataClassRepository.readById(otherClassId)
        dataClassRepository.createExtensionRelationship(sourceDataClass, targetDataClass)
        dataClassRepository.findById(id)
    }

    @Audit(
        parentDomainType = DataClass,
        parentIdParamName = 'id',
        deletedObjectDomainType = DataClass,
        description = 'Delete DataClass extends relationship'
    )
    @Delete(Paths.DATA_CLASS_EXTENDS)
    DataClass deleteExtension(UUID dataModelId, UUID id, UUID otherModelId, UUID otherClassId) {
        DataClass sourceDataClass = dataClassRepository.readById(id)
        accessControlService.checkRole(Role.EDITOR, sourceDataClass)
        DataClass targetDataClass = dataClassRepository.readById(otherClassId)
        dataClassRepository.deleteExtensionRelationship(sourceDataClass, targetDataClass)
        dataClassRepository.findById(id)
    }

    @Audit
    @Post(Paths.DATA_CLASS_COPY)
    @Transactional
    DataClass copy(UUID dataModelId, UUID otherModelId, UUID dataClassId) {
        DataModel dataModel = dataModelContentRepository.findWithContentById(dataModelId)
        accessControlService.checkRole(Role.EDITOR, dataModel)
        DataClass dataClass = dataClassContentRepository.readWithContentById(dataClassId)
        accessControlService.canDoRole(Role.EDITOR, dataClass)
        DataModel otherModel = dataModelRepository.readById(otherModelId)
        accessControlService.canDoRole(Role.READER, otherModel)
        //verify
        if (dataClass.dataModel.id != otherModel.id) {
            ErrorHandler.handleError(HttpStatus.NOT_FOUND, "Cannot find dataClass $dataClassId for dataModel $otherModelId")
        }

        DataClass copied = dataClass.clone()
        copied.parentDataClass = null //do not keep existing source structure if source is child
        copied.dataClasses = copyChildren(dataModel, copied)
        copied.dataElements = createCopyDataElementsAndDataTypes(copied, dataModel)

        DataClass savedCopy = createEntity(dataModel, copied)
        savedCopy.dataClasses = saveChildren(savedCopy)
        savedCopy.dataElements = saveDataElements(savedCopy)
        savedCopy

    }

    protected List<DataClass> copyChildren(DataModel dataModel, DataClass copied) {
        List<DataClass> children = copied.dataClasses.collect {child ->
            child.clone().tap {copiedChild ->
                copiedChild.updateCreationProperties()
                super.updateDerivedProperties(copiedChild)
                copiedChild.dataElements = createCopyDataElementsAndDataTypes(copiedChild, dataModel)
                copiedChild.parentDataClass = copied
                copiedChild.dataModel = dataModel
            }
        }
        children
    }

    protected List<DataElement> createCopyDataElementsAndDataTypes(DataClass copied, DataModel target) {
        copied.dataElements.collect {
            it.clone().tap {copiedDE ->
                if (copiedDE.dataType) {
                    //target model does not have existing dataType? copy new DataType in target model
                    copiedDE = setOrCreateNewDataType(target, it.dataType, copiedDE)
                }
                updateCreationProperties(copiedDE) as DataElement
                updatePaths(copiedDE)
                copiedDE.dataModel = target
                copiedDE.dataClass = copied
            }
        }
        copied.dataElements
    }

    AdministeredItem updatePaths(AdministeredItem item) {
        pathRepository.readParentItems(item)
        item.updatePath()
        item.updateBreadcrumbs()
        item
    }

    protected DataElement setOrCreateNewDataType(DataModel target, DataType dataType, DataElement copiedDE) {
        DataType equivalentInTargetModel = target.dataTypes.find {targetModelDataType -> targetModelDataType.label == dataType.label}
        if (equivalentInTargetModel) {
            copiedDE.dataType = equivalentInTargetModel
        } else {
            DataType copiedDataType = copiedDE.dataType.clone()
            copiedDataType.updateCreationProperties()
            copiedDataType = updatePaths(copiedDataType) as DataType
            copiedDataType.dataModel = target
            copiedDE.dataType = copiedDataType
        }
        copiedDE
    }


    protected List<DataClass> saveChildren(DataClass saved) {
        List<DataClass> savedChildren = dataClassRepository.saveAll(saved.dataClasses)
        savedChildren.each {savedChild ->
            savedChild.dataElements = saveDataElements(savedChild)
            savedChild.parentDataClass = saved
        }
        dataClassRepository.saveAll(savedChildren)
    }

    protected List<DataElement> saveDataElements(DataClass saved) {
        saved.dataElements.each {
            it.dataClass = saved
            if (!it.dataType.id) {
                dataTypeCacheableRepository.save(it.dataType)
            }
        }
        dataElementCacheableRepository.saveAll(saved.dataElements)
    }
}

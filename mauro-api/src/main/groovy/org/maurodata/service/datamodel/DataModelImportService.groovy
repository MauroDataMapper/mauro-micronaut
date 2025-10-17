package org.maurodata.service.datamodel

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.maurodata.domain.dataflow.DataFlow
import org.maurodata.domain.datamodel.DataClass
import org.maurodata.domain.datamodel.DataElement
import org.maurodata.domain.datamodel.DataModel
import org.maurodata.domain.datamodel.ModelType
import org.maurodata.domain.model.ModelItem
import org.maurodata.persistence.cache.AdministeredItemCacheableRepository
import org.maurodata.persistence.cache.ModelCacheableRepository
import org.maurodata.persistence.datamodel.DataModelContentRepository
import org.maurodata.persistence.model.PathRepository
import org.maurodata.security.AccessControlService
import org.maurodata.service.core.AdministeredItemService

import static org.maurodata.util.PathStringUtils.getPathFrom

@CompileStatic
@Singleton
@Slf4j
class DataModelImportService extends AdministeredItemService {

    DataModelContentRepository dataModelContentRepository
    ModelCacheableRepository.DataModelCacheableRepository dataModelCacheableRepository
    AdministeredItemCacheableRepository.DataClassComponentCacheableRepository dataClassComponentCacheableRepository
    AdministeredItemCacheableRepository.DataElementComponentCacheableRepository dataElementComponentCacheableRepository

    @Inject
    AccessControlService accessControlService

    DataModelImportService(ModelCacheableRepository.DataModelCacheableRepository dataModelCacheableRepository,
                           DataModelContentRepository dataModelContentRepository,
                           AdministeredItemCacheableRepository.DataClassComponentCacheableRepository dataClassComponentCacheableRepository,
                           AdministeredItemCacheableRepository.DataElementComponentCacheableRepository dataElementComponentCacheableRepository,
                           PathRepository pathRepository) {
        this.pathRepository = pathRepository
        this.dataModelCacheableRepository = dataModelCacheableRepository
        this.dataModelContentRepository = dataModelContentRepository
        this.dataClassComponentCacheableRepository = dataClassComponentCacheableRepository
        this.dataElementComponentCacheableRepository = dataElementComponentCacheableRepository
    }

    List<DataModel> saveImportedModels(List<DataModel> dataModels) {
        dataModels.collect {imp ->
            imp = preProcessDataFlows(imp)
            DataModel savedImported = saveModelContents(imp)
            savedImported
        }
    }

    DataModel preProcessDataFlows(DataModel imp) {
        List<DataFlow> validatedTargets = []
        imp.targetDataFlows.each {
            DataModel matchedSource = matchedModel(it.source, ModelType.SOURCE)
            if (!matchedSource) {
                log.warn("Import dataflow - no match found for source model: $it.source.id, with label : $it.source.label. Not importing dataflow")
            } else {
                it.source = matchedSource
                validatedTargets.add(it)
            }
        }
        imp.targetDataFlows = validatedTargets

        List<DataFlow> validatedSources = []
        imp.sourceDataFlows.each {
            DataModel matchedTarget = matchedModel(it.target, ModelType.TARGET)
            if (!matchedTarget) {
                log.warn("Import dataflow - no match found for source model: $it.target.id, with label : $it.target.label. Not importing dataflow")
            } else {
                it.target = matchedTarget
                validatedSources.add(it)
            }
        }
        imp.sourceDataFlows = validatedSources
        imp
    }

    List<DataModel> saveDataFlowModelItems(List<DataModel> saved) {
        saved.collect {savedImported ->
            saveDataFlowModelItems(savedImported)
        }
    }

    DataModel saveDataFlowModelItems(DataModel saveImported) {
        List<DataClass> savedImportedDataClasses = saveImported.allDataClasses.each {updateDerivedProperties(it)} as List<DataClass>
        List<DataElement> saveImportedDataElements = saveImported.dataElements.each {updateDerivedProperties(it)}

        saveImported.targetDataFlows.each {
            DataModel matchedSource = dataModelContentRepository.findWithContentById(it.source.id)
            List<DataClass> matchedSourceDataClasses = matchedSource.allDataClasses.each {
                updateDerivedProperties(it)
            } as List<DataClass>
            List<DataElement> matchedSourceDataElements = matchedSource.dataElements.each {
                updateDerivedProperties(it)
            }

            it.dataClassComponents.each {dataClassComponent ->
                updateDerivedProperties(dataClassComponent)
                dataClassComponent.targetDataClasses.each {impDataClass ->
                    updateDerivedProperties(impDataClass)
                    DataClass saveImportedDataClass = savedImportedDataClasses.find {
                        getPathFrom('dc', it.path.pathString) == getPathFrom('dc', impDataClass.path.pathString)
                    }
                    if (saveImportedDataClass) {
                        dataClassComponentCacheableRepository.addTargetDataClass(dataClassComponent.id, saveImportedDataClass.id)
                    }
                }
                dataClassComponent.sourceDataClasses.each {exportSourceDC ->
                    updateDerivedProperties(exportSourceDC)
                    DataClass matchedDC = matchedSourceDataClasses.find {
                        subPathsMatch(it, exportSourceDC)
                    }
                    if (matchedDC) {
                        dataClassComponentCacheableRepository.addSourceDataClass(dataClassComponent.id, matchedDC.id)
                    }
                }

                dataClassComponent.dataElementComponents.each {dEC ->
                    dEC.targetDataElements.each {impDataElement ->
                        updateDerivedProperties(impDataElement)
                        DataElement saveImportedDataElement =
                            saveImportedDataElements.find {getPathFrom('dc', it.path.pathString) == getPathFrom('dc', impDataElement.path.pathString)}
                        if (saveImportedDataElement) {
                            dataElementComponentCacheableRepository.addTargetDataElement(dEC.id, saveImportedDataElement.id)
                        }
                    }

                    dEC.sourceDataElements.each {exportSourceDE ->
                        updateDerivedProperties(exportSourceDE)
                        DataElement matchedSourceDE =
                            matchedSourceDataElements.find {
                                subPathsMatch(it, exportSourceDE)
                            }
                        if (matchedSourceDE) {
                            dataElementComponentCacheableRepository.addSourceDataElement(dEC.id, matchedSourceDE.id)
                        }
                    }
                }
            }
        }
        saveImported
    }

    DataModel saveModelContents(DataModel imp) {
        log.info '** about to saveWithContentBatched... **'
        imp = updateCreationProperties(imp) as DataModel
        imp.catalogueUser = accessControlService.getUser()
        DataModel savedImported = dataModelContentRepository.saveWithContent(imp)
        saveDataFlowModelItems(savedImported)

        log.info '** finished saveWithContentBatched **'
        savedImported
    }

    DataModel updateModelPaths(DataModel dataModel) {
        updatePaths(dataModel)
        dataModel.dataClasses.each {
            updatePaths(it)
            it.dataClasses.each {
                updatePaths(it)
            }
        }
        dataModel.dataElements.each {
            updatePaths(it)
        }
        dataModel.dataTypes.each {
            updatePaths(it)
        }
        dataModel.sourceDataFlows = updateDataFlowPaths(dataModel.sourceDataFlows)
        dataModel.targetDataFlows = updateDataFlowPaths(dataModel.targetDataFlows)
        dataModel
    }


    protected DataModel findByLabelAndBranchNameExcludeId(String label, String branchName, UUID id) {
        //dataflow branch name and label. Excluding self source datamodel from list
        List<DataModel> importedSources = dataModelCacheableRepository.findAllByLabelAndBranchName(label, branchName).findAll {
            it.id != id
        }
        if (importedSources.size() > 1) {
            log.warn("Multiple models found with label ${label}  and branchName ${branchName}. Returning 1st match")
        }
        importedSources.isEmpty() ? null : importedSources.first()
    }

    /**
     find matching model for modelToMatch
     */
    protected DataModel matchedModel(DataModel modelToMatch, ModelType modelType) {
        DataModel matchedModel = findByLabelAndBranchNameExcludeId(modelToMatch.label, modelToMatch.branchName, modelToMatch.id)
        if (canImportModel(modelToMatch, matchedModel, modelType)) {
            matchedModel
        } else {
            null
        }
    }

    protected boolean canImportModel(DataModel exportModel, DataModel matchedModel, ModelType modelType) {
        if (!matchedModel) {
            return false
        }
        exportModel = dataModelContentRepository.findWithContentById(exportModel.id)
        //get full model contents
        matchedModel = dataModelContentRepository.findWithContentById(matchedModel.id)
        List<DataClass> matchedModelDataClasses = matchedModel.allDataClasses as List<DataClass>
        List<DataClass> exportModelDataClasses =
            modelType == ModelType.SOURCE ? exportModel.sourceDataFlows.dataClassComponents.sourceDataClasses.flatten() as List<DataClass> :
            exportModel.targetDataFlows.dataClassComponents.targetDataClasses.flatten() as List<DataClass>

        boolean canImportDataClasses =
            exportModelDataClasses.isEmpty() ? true : exportModelDataClasses.every {matchedModelDataClasses.find {dataClass -> dataClass.label == it.label}}

        List<DataElement> matchedDataElements = matchedModel.dataElements as List<DataElement>

        List<DataElement> exportDataElements =
            modelType == ModelType.SOURCE ? exportModel.sourceDataFlows.dataClassComponents.dataElementComponents.sourceDataElements.flatten() as List<DataElement> :
            exportModel.targetDataFlows.dataClassComponents.dataElementComponents.targetDataElements.flatten() as List<DataElement>

        boolean canImportDataElements =
            exportDataElements.isEmpty() ? true : exportDataElements.every {matchedDataElements.find {dataElement -> dataElement.label == it.label}}

        return canImportDataClasses && canImportDataElements
    }


    protected List<DataFlow> updateDataFlowPaths(List<DataFlow> dataFlows) {
        return dataFlows.each {
            pathRepository.readParentItems(it)
            it.updatePath()
            updatePaths(it.source)
            updatePaths(it.target)
            it.dataClassComponents.each {dCC ->
                updatePaths(dCC)
                dCC.sourceDataClasses.each {dC -> updatePaths(dC)
                }
                dCC.targetDataClasses.each {tDC -> updatePaths(tDC)
                }
                dCC.dataElementComponents.each {dEC ->
                    updatePaths(dEC)
                    dEC.sourceDataElements.each {dE -> updatePaths(dE)
                    }
                    dEC.targetDataElements.each {tDE -> updatePaths(tDE)
                    }
                }
            }
        }
    }


    protected static boolean subPathsMatch(ModelItem it, ModelItem modelItemSource) {
        return getPathFrom(it.pathPrefix, it.path.pathString) ==
               getPathFrom(it.pathPrefix, modelItemSource.path.pathString)
    }
}

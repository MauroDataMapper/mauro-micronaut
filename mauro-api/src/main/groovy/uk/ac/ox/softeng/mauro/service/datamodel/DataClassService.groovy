package uk.ac.ox.softeng.mauro.service.datamodel

import uk.ac.ox.softeng.mauro.ErrorHandler
import uk.ac.ox.softeng.mauro.domain.datamodel.DataClass
import uk.ac.ox.softeng.mauro.domain.datamodel.DataElement
import uk.ac.ox.softeng.mauro.domain.datamodel.DataModel
import uk.ac.ox.softeng.mauro.domain.datamodel.DataType
import uk.ac.ox.softeng.mauro.domain.model.AdministeredItem
import uk.ac.ox.softeng.mauro.persistence.cache.AdministeredItemCacheableRepository
import uk.ac.ox.softeng.mauro.persistence.datamodel.DataClassContentRepository
import uk.ac.ox.softeng.mauro.persistence.model.PathRepository

import groovy.transform.CompileStatic
import io.micronaut.http.HttpStatus
import jakarta.inject.Inject

@CompileStatic
class DataClassService {

    PathRepository pathRepository
    DataClassContentRepository dataClassContentRepository
    AdministeredItemCacheableRepository.DataElementCacheableRepository dataElementCacheableRepository
    AdministeredItemCacheableRepository.DataTypeCacheableRepository dataTypeCacheableRepository

    @Inject
    DataClassService(PathRepository pathRepository, DataClassContentRepository dataClassContentRepository,
                     AdministeredItemCacheableRepository.DataElementCacheableRepository dataElementCacheableRepository,
                     AdministeredItemCacheableRepository.DataTypeCacheableRepository dataTypeCacheableRepository) {
        this.pathRepository = pathRepository
        this.dataClassContentRepository = dataClassContentRepository
        this.dataElementCacheableRepository = dataElementCacheableRepository
        this.dataTypeCacheableRepository = dataTypeCacheableRepository
    }

    List<DataClass> copyChildren(DataClass copied, DataModel parent) {
        List<DataClass> children = copied.dataClasses.collect {child ->
            child.clone().tap {copiedChild ->
                copiedChild.updateCreationProperties()
                updateDerivedProperties(copiedChild)
                copiedChild.parentDataClass = copied
                copiedChild.dataModel = dataModel
                dataClassContentRepository.saveWithContent(copiedChild)
            }
        }
        children
    }


    DataClass copyDataElementsAndDataTypes(DataClass savedCopy, DataModel target) {
        copyDataElementsAndDataTypes([savedCopy], target).first()
    }

    List<DataClass> copyDataElementsAndDataTypes(List<DataClass> savedChildren, DataModel target) {
        savedChildren.collect {child ->
            copyDataElementsAndDataTypes(child.dataElements, child, target)
        }
        savedChildren
    }

    List<DataElement> copyDataElementsAndDataTypes(List<DataElement> dataElements, DataClass child, DataModel target) {
        dataElements.collect {
            it.clone().tap {copiedDE ->
                if (copiedDE.dataType) {
                    //target model does not have existing dataType? copy new DataType in target model
                    copiedDE = setOrCreateNewDataType(target, it.dataType, copiedDE)
                }
                updateCreationProperties(copiedDE)
                updateDerivedProperties(copiedDE)
                copiedDE.dataModel = target
                copiedDE.dataClass = child
            }
        }
        dataElementCacheableRepository.saveAll(dataElements)
    }

    DataElement setOrCreateNewDataType(DataModel target, DataType dataType, DataElement copiedDE) {
        DataType equivalentInTargetModel = target.dataTypes.find {targetModelDataType -> targetModelDataType.label == dataType.label}
        if (equivalentInTargetModel) {
            copiedDE.dataType = equivalentInTargetModel
        } else {
            DataType copiedDataType = copiedDE.dataType.clone()
            copiedDataType.updateCreationProperties()
            copiedDataType = updateDerivedProperties(copiedDataType) as DataType
            copiedDataType.dataModel = target
            DataType savedDataType = dataTypeCacheableRepository.save(copiedDataType)
            copiedDE.dataType = savedDataType
        }
        copiedDE
    }

    AdministeredItem updateDerivedProperties(AdministeredItem item) {
        pathRepository.readParentItems(item)
        item.updatePath()
        item.updateBreadcrumbs()
        item
    }

    void deleteDanglingReferenceTypes(DataClass dataClassToDelete) {
        List<DataType> dataTypes = dataTypeCacheableRepository.findAllByReferenceClass(dataClassToDelete).unique()
        dataTypes.each {
            List<DataElement> dataElementReferenced = dataElementCacheableRepository.readAllByDataType(it)
            if (dataElementReferenced.isEmpty()) {
                dataTypeCacheableRepository.delete(it)
            } else {
                ErrorHandler.handleError(HttpStatus.UNPROCESSABLE_ENTITY, "Cannot delete Data Class has associations - check dataElements")
            }
        }
    }

    protected void updateCreationProperties(AdministeredItem item) {
        item.id = null
        item.version = null
        item.dateCreated = null
        item.lastUpdated = null
    }
}

package uk.ac.ox.softeng.mauro.service.datamodel

import uk.ac.ox.softeng.mauro.ErrorHandler
import uk.ac.ox.softeng.mauro.domain.datamodel.DataClass
import uk.ac.ox.softeng.mauro.domain.datamodel.DataElement
import uk.ac.ox.softeng.mauro.domain.datamodel.DataModel
import uk.ac.ox.softeng.mauro.domain.datamodel.DataType
import uk.ac.ox.softeng.mauro.domain.model.AdministeredItem
import uk.ac.ox.softeng.mauro.persistence.cache.AdministeredItemCacheableRepository
import uk.ac.ox.softeng.mauro.persistence.cache.AdministeredItemCacheableRepository.DataClassCacheableRepository
import uk.ac.ox.softeng.mauro.persistence.datamodel.DataClassContentRepository
import uk.ac.ox.softeng.mauro.persistence.model.PathRepository

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.micronaut.http.HttpStatus
import jakarta.inject.Inject

@CompileStatic
@Slf4j
class DataClassService {

    PathRepository pathRepository
    DataClassContentRepository dataClassContentRepository
    AdministeredItemCacheableRepository.DataElementCacheableRepository dataElementCacheableRepository
    AdministeredItemCacheableRepository.DataTypeCacheableRepository dataTypeCacheableRepository
    DataClassCacheableRepository dataClassCacheableRepository

    @Inject
    DataClassService(PathRepository pathRepository, DataClassContentRepository dataClassContentRepository,
                     AdministeredItemCacheableRepository.DataElementCacheableRepository dataElementCacheableRepository,
                     AdministeredItemCacheableRepository.DataTypeCacheableRepository dataTypeCacheableRepository,
                     DataClassCacheableRepository dataClassCacheableRepository) {
        this.pathRepository = pathRepository
        this.dataClassContentRepository = dataClassContentRepository
        this.dataElementCacheableRepository = dataElementCacheableRepository
        this.dataTypeCacheableRepository = dataTypeCacheableRepository
        this.dataClassCacheableRepository = dataClassCacheableRepository
    }

    DataClass copyReferenceTypes(DataClass savedCopy, DataModel target) {
        //cloned so these are old
        List<DataType> copiedTargetDataTypes = []
        savedCopy.referenceTypes.each {
            DataType targetDataType = findDataTypeByLabelInTarget(it, target)
            if (!targetDataType) {
                copiedTargetDataTypes.add(createAndSave(it, target, savedCopy))
            } else {
                copiedTargetDataTypes.add(targetDataType)
            }
        }
        savedCopy.referenceTypes = dataTypeCacheableRepository.updateAll(copiedTargetDataTypes)
        savedCopy
    }

    List<DataClass> copyChildren(DataClass copied, List<DataClass> children, DataModel target) {
        List<DataClass> copiedChildren = []
        children.each {child ->
            child.clone().tap {copiedChild ->
                copiedChild.updateCreationProperties()
                updateDerivedProperties(copiedChild)
                copiedChild.parentDataClass = copied
                copiedChild.dataModel = target
                copiedChild = copyReferenceTypes(copiedChild, target)
                copiedChild.dataElements = copyDataElementsAndDataTypes(copiedChild.dataElements, target)
                dataElementCacheableRepository.saveAll(copiedChild.dataElements)
                copiedChildren.add(copiedChild)
            }
        }
        copied.dataClasses = dataClassCacheableRepository.saveAll(copiedChildren)
    }

    List<DataElement> copyDataElementsAndDataTypes(List<DataElement> dataElements, DataModel target) {
        List<DataElement> copiedDataElements = []
        dataElements.each {dataElement ->
            dataElement.clone().tap {copiedDE ->
                if (copiedDE.dataType) {
                    //target model does not have existing dataType? copy new DataType in target model
                    copiedDE.dataType = setOrCreateNewDataType(target, copiedDE.dataType)
                }
                updateCreationProperties(copiedDE)
                updateDerivedProperties(copiedDE)
                copiedDE.dataModel = target
                copiedDE.dataClass = dataClass
                copiedDataElements.add(copiedDE)
            }
        }
        dataElementCacheableRepository.saveAll(copiedDataElements)
    }

    protected DataType setOrCreateNewDataType(DataModel target, DataType dataType) {
        DataType targetDataType = findDataTypeByLabelInTarget(dataType, target)
        if (!targetDataType) {
            targetDataType = createAndSave(dataType, target, null)
        }
        targetDataType
    }


    protected DataType createAndSave(DataType source, DataModel target, DataClass savedCopy) {
        DataType copiedDataType = source.clone()
        copiedDataType.updateCreationProperties()
        copiedDataType = updateDerivedProperties(copiedDataType) as DataType
        copiedDataType.referenceClass = savedCopy ?: null
        copiedDataType.dataModel = target
        log.info("Saving new datatype with label $copiedDataType.label to model $target.id: ")
        DataType saved = dataTypeCacheableRepository.save(copiedDataType)
        saved
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

    protected static DataType findDataTypeByLabelInTarget(DataType dataType, DataModel target) {
        target.dataTypes.find {targetModelDataType -> targetModelDataType.label == dataType.label}
    }

    protected AdministeredItem updateDerivedProperties(AdministeredItem item) {
        pathRepository.readParentItems(item)
        item.updatePath()
        item.updateBreadcrumbs()
        item
    }

    protected void updateCreationProperties(AdministeredItem item) {
        item.id = null
        item.version = null
        item.dateCreated = null
        item.lastUpdated = null
    }
}

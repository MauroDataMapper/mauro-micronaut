package org.maurodata.service.datamodel


import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import jakarta.inject.Inject
import org.maurodata.domain.datamodel.DataClass
import org.maurodata.domain.datamodel.DataElement
import org.maurodata.domain.datamodel.DataModel
import org.maurodata.domain.datamodel.DataType
import org.maurodata.domain.model.AdministeredItem
import org.maurodata.persistence.cache.AdministeredItemCacheableRepository
import org.maurodata.persistence.model.PathRepository
import org.maurodata.service.core.AdministeredItemService

@CompileStatic
@Slf4j
class DataClassService extends AdministeredItemService{

    AdministeredItemCacheableRepository.DataElementCacheableRepository dataElementCacheableRepository
    AdministeredItemCacheableRepository.DataTypeCacheableRepository dataTypeCacheableRepository
    AdministeredItemCacheableRepository.DataClassCacheableRepository dataClassCacheableRepository
    DataTypeService dataTypeService

    @Inject
    DataClassService(PathRepository pathRepository,
                     AdministeredItemCacheableRepository.DataElementCacheableRepository dataElementCacheableRepository,
                     AdministeredItemCacheableRepository.DataTypeCacheableRepository dataTypeCacheableRepository,
                     AdministeredItemCacheableRepository.DataClassCacheableRepository dataClassCacheableRepository, DataTypeService dataTypeService) {
        this.pathRepository = pathRepository
        this.dataElementCacheableRepository = dataElementCacheableRepository
        this.dataTypeCacheableRepository = dataTypeCacheableRepository
        this.dataClassCacheableRepository = dataClassCacheableRepository
        this.dataTypeService = dataTypeService
    }

    DataClass copyReferenceTypes(DataClass savedCopy, DataModel target) {
        //cloned so these are old
        List<DataType> copiedTargetDataTypes = []
        savedCopy.referenceTypes.each {
            DataType targetDataType = dataTypeService.findInModel(it, target)
            if (!targetDataType) {
                copiedTargetDataTypes.add(dataTypeService.createAndSave(it, target, savedCopy))
            } else {
                copiedTargetDataTypes.add(targetDataType)
            }
        }
        savedCopy.referenceTypes = dataTypeCacheableRepository.updateAll(copiedTargetDataTypes) as List<DataType>
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
                //dataElementCacheableRepository.saveAll(copiedChild.dataElements)
                copiedChildren.add(copiedChild)
            }
        }
        //copied.dataClasses = dataClassCacheableRepository.saveAll(copiedChildren)
    }

    List<DataElement> copyDataElementsAndDataTypes(List<DataElement> dataElements, DataModel target) {
        List<DataElement> copiedDataElements = []
        dataElements.each {dataElement ->
            dataElement.clone().tap {copiedDE ->
                if (copiedDE.dataType) {
                    //target model does not have existing dataType? copy new DataType in target model
                    copiedDE.dataType = setOrCreateNewDataType(target, copiedDE.dataType)
                }
                updateCreationProperties(copiedDE as AdministeredItem)
                updateDerivedProperties(copiedDE)
                copiedDE.dataModel = target
                copiedDE.dataClass = dataClass
                copiedDataElements.add(copiedDE)
            }
        }
        dataElementCacheableRepository.saveAll(copiedDataElements)
    }

    protected DataType setOrCreateNewDataType(DataModel target, DataType dataType) {
        DataType targetDataType = dataTypeService.findInModel(dataType, target)
        if (!targetDataType) {
            targetDataType = dataTypeService.createAndSave(dataType, target, null)
        }
        targetDataType
    }

}

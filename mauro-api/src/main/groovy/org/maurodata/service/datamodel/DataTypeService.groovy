package org.maurodata.service.datamodel

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.micronaut.core.annotation.NonNull
import io.micronaut.http.HttpStatus
import io.micronaut.transaction.annotation.Transactional
import jakarta.inject.Inject
import org.maurodata.ErrorHandler
import org.maurodata.domain.classifier.Classifier
import org.maurodata.domain.datamodel.DataClass
import org.maurodata.domain.datamodel.DataModel
import org.maurodata.domain.datamodel.DataType
import org.maurodata.domain.datamodel.EnumerationValue
import org.maurodata.domain.model.AdministeredItem
import org.maurodata.domain.model.Item
import org.maurodata.domain.model.Model
import org.maurodata.domain.model.Path
import org.maurodata.domain.security.Role
import org.maurodata.persistence.cache.AdministeredItemCacheableRepository
import org.maurodata.persistence.cache.AdministeredItemCacheableRepository.ClassifierCacheableRepository
import org.maurodata.persistence.cache.ModelCacheableRepository
import org.maurodata.security.AccessControlService
import jakarta.inject.Singleton
import org.maurodata.service.core.AdministeredItemService
import org.maurodata.persistence.service.RepositoryService

@CompileStatic
@Singleton
@Slf4j
class DataTypeService extends AdministeredItemService {
    AdministeredItemCacheableRepository.DataTypeCacheableRepository dataTypeRepository
    AdministeredItemCacheableRepository.DataClassCacheableRepository dataClassRepository
    AdministeredItemCacheableRepository.EnumerationValueCacheableRepository enumerationValueRepository
    AccessControlService accessControlService
    RepositoryService repositoryService

    @Inject
    DataTypeService(AdministeredItemCacheableRepository.DataTypeCacheableRepository dataTypeRepository,
                    AdministeredItemCacheableRepository.DataClassCacheableRepository dataClassRepository,
                    AdministeredItemCacheableRepository.EnumerationValueCacheableRepository enumerationValueRepository,
                    AccessControlService accessControlService,
                    RepositoryService repositoryService) {
        this.dataTypeRepository = dataTypeRepository
        this.dataClassRepository = dataClassRepository
        this.enumerationValueRepository = enumerationValueRepository
        this.accessControlService = accessControlService
        this.repositoryService = repositoryService
    }

    @Transactional
    DataType createDataType(DataType cleanItem, AdministeredItem parent, List<EnumerationValue> enumerationValues) {
        cleanItem = validateDataType(cleanItem, parent)

        if (cleanItem.isReferenceType() && cleanItem.referenceClass.id == parent.id) {
            ErrorHandler.handleError(HttpStatus.UNPROCESSABLE_ENTITY, "Data class element shouldn't reference it")
        }

        if (cleanItem.isModelType()) {
            ModelCacheableRepository modelRepository = repositoryService.getModelRepository(cleanItem.modelResourceDomainType)
            Model modelResource = modelRepository.readById(cleanItem.modelResourceId)
            ErrorHandler.handleErrorOnNullObject(
                HttpStatus.NOT_FOUND,
                modelResource,
                "Item not found : $cleanItem.modelResourceId, $cleanItem.modelResourceDomainType"
            )

            pathRepository.readParentItems(modelResource)
            pathRepository.readParentItems(parent)

            Path pathToModelResource = modelResource.getPathToEdge()
            Path pathToDataModel = parent.getPathToEdge()
            Item resourceVersionedFolder = pathToModelResource.findAncestorNodeItem(cleanItem.modelResourceId, "VersionedFolder")
            Item dataModelVersionedFolder = resourceVersionedFolder != null ?
                pathToDataModel.findAncestorNodeItem(parent.id, "VersionedFolder") : null

            if (resourceVersionedFolder == null || dataModelVersionedFolder == null ||
                resourceVersionedFolder.id != dataModelVersionedFolder.id) {
                validateModelResource(cleanItem, modelResource)
            }
        }

        if (!cleanItem.label) {
            if (cleanItem.isReferenceType()) {
                cleanItem.label = "Reference to ${cleanItem.referenceClass.label}"
            } else if (cleanItem.modelResourceId) {
                AdministeredItemCacheableRepository resourceRepository =
                    repositoryService.getAdministeredItemRepository(cleanItem.modelResourceDomainType)
                AdministeredItem resource = resourceRepository.readById(cleanItem.modelResourceId)
                cleanItem.label = "Reference to ${resource.label}"
            }
        }

        cleanItem.updateCreationProperties()
        cleanItem.catalogueUser = accessControlService.getUser()
        cleanItem.parent = parent
        updateDerivedProperties(cleanItem)
        DataType created = dataTypeRepository.save(cleanItem)
        created.classifiers = validateClassifiers(created)
        created.classifiers.each { Classifier classifier ->
            classifierRepository(classifier.domainType).addAdministeredItem(created, classifier)
        }

        if (enumerationValues) {
            enumerationValues.each { EnumerationValue enumValue ->
                enumValue.enumerationType = created
                enumerationValueRepository.save(enumValue)
            }
        }
        created
    }

    private void validateModelResource(DataType dataType, Model modelResource) {
        accessControlService.checkRole(Role.READER, modelResource)
        if (!modelResource.finalised) {
            ErrorHandler.handleError(
                HttpStatus.UNPROCESSABLE_ENTITY,
                "Model resource is not finalised: $dataType.modelResourceId, $dataType.modelResourceDomainType"
            )
        }
    }

    private List<Classifier> validateClassifiers(DataType dataType) {
        if (!dataType.classifiers) {
            return []
        }
        dataType.classifiers.collect { Classifier classifier ->
            Classifier retrieved = classifierRepository(classifier.domainType).readById(classifier.id)
            ErrorHandler.handleErrorOnNullObject(HttpStatus.NOT_FOUND, retrieved, " Item with id: $classifier.id not found ")
            accessControlService.checkRole(Role.EDITOR, retrieved)
            retrieved
        }
    }

    private ClassifierCacheableRepository classifierRepository(String domainType) {
        repositoryService.getAdministeredItemRepository(domainType) as ClassifierCacheableRepository
    }

    DataType validateDataType(DataType dataType, AdministeredItem parent) {
        if (dataType.isReferenceType()) {
            dataType.referenceClass = validatedReferenceClass(dataType, parent)
        }
        DataModelHelper.validateModelTypeFields(dataType)
        dataType
    }

    DataType getReferenceClassProperties(DataType dataType) {
        if (dataType.isReferenceType()) {
            dataType.referenceClass = dataClassRepository.readById(dataType.referenceClass?.id)
        }
        dataType
    }

    DataType getEnumerationValues(@NonNull DataType dataType) {
        if (dataType.domainType == DataType.DataTypeKind.ENUMERATION_TYPE.stringValue) {
            dataType.enumerationValues = enumerationValueRepository.readAllByEnumerationType_Id(dataType.id)
        }
        dataType
    }

    protected DataClass validatedReferenceClass(DataType dataType, AdministeredItem parent) {
        UUID referenceClassId = dataType.referenceClass?.id
        if (!referenceClassId) {
            ErrorHandler.handleError(HttpStatus.UNPROCESSABLE_ENTITY, "Error -ReferenceType datatype requires referenceClass")
        }
        DataClass referenceClass = getReferenceDataClass(referenceClassId)
        accessControlService.checkRole(Role.READER, referenceClass)
        if (referenceClass.dataModel.id != parent.id) {
            ErrorHandler.handleError(HttpStatus.UNPROCESSABLE_ENTITY, "DataClass $referenceClass.id assigned to DataType must belong to same datamodel")
        }

        if (referenceClass.id == parent.id) {
            ErrorHandler.handleError(HttpStatus.UNPROCESSABLE_ENTITY, "Data class reference should not be parent")
        }

        DataType sameLabelInModel = dataTypeRepository.findAllByParent(parent).find {
            it.domainType == DataType.DataTypeKind.REFERENCE_TYPE.stringValue && it.label == dataType.label
        }
        if (sameLabelInModel) {
            ErrorHandler.handleError(HttpStatus.UNPROCESSABLE_ENTITY, "Label $dataType.label exists for ReferenceType")
        }
        referenceClass
    }

    DataType findInModel(DataType dataType, DataModel target) {
        target.dataTypes.find {targetModelDataType -> targetModelDataType.label == dataType.label}
    }


    DataType createAndSave(DataType source, DataModel target, DataClass referenceClass) {
        DataType copiedDataType = source.clone()
        copiedDataType.updateCreationProperties()
        copiedDataType = super.updateDerivedProperties(copiedDataType) as DataType
        copiedDataType.referenceClass = referenceClass ?: null
        copiedDataType.dataModel = target
        log.info("Saving new datatype with label $copiedDataType.label to model $target.id: ")
        dataTypeRepository.save(copiedDataType)
    }

     DataType readDataType(UUID dataTypeId) {
        dataTypeRepository.readById(dataTypeId)
    }


    protected DataClass getReferenceDataClass(@NonNull UUID dataClassId) {
        DataClass referenceClass = dataClassRepository.findById(dataClassId)
        ErrorHandler.handleErrorOnNullObject(HttpStatus.UNPROCESSABLE_ENTITY, referenceClass, "Cannot find reference class ")
        referenceClass
    }
}

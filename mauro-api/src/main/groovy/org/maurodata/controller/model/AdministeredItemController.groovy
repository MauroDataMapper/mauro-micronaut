package org.maurodata.controller.model

import org.maurodata.domain.datamodel.DataClass
import org.maurodata.domain.datamodel.DataType

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.NonNull
import io.micronaut.core.annotation.Nullable
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import io.micronaut.http.annotation.Body
import io.micronaut.http.exceptions.HttpStatusException
import io.micronaut.security.annotation.Secured
import io.micronaut.security.rules.SecurityRule
import io.micronaut.transaction.annotation.Transactional
import jakarta.inject.Inject
import org.maurodata.ErrorHandler
import org.maurodata.api.model.AdministeredItemApi
import org.maurodata.domain.classifier.Classifier
import org.maurodata.domain.model.AdministeredItem
import org.maurodata.domain.security.Role
import org.maurodata.persistence.cache.AdministeredItemCacheableRepository
import org.maurodata.persistence.model.AdministeredItemContentRepository
import org.maurodata.persistence.model.AdministeredItemRepository
import org.maurodata.persistence.model.PathRepository
import org.maurodata.persistence.service.RepositoryService
import org.maurodata.web.ListResponse
import org.maurodata.web.PaginationParams

@CompileStatic
@Secured(SecurityRule.IS_ANONYMOUS)
abstract class AdministeredItemController<I extends AdministeredItem, P extends AdministeredItem> extends ItemController<I> implements AdministeredItemApi<I, P> {

    RepositoryService repositoryService
    /**
     * Properties disallowed in a simple update request.
     */
    List<String> getDisallowedProperties() {
        super.getDisallowedProperties() + ['path', 'parent', 'owner']
    }

    Class<I> itemClass

    AdministeredItemRepository<I> administeredItemRepository

    AdministeredItemRepository<P> parentItemRepository

    AdministeredItemContentRepository administeredItemContentRepository

    @Inject
    PathRepository pathRepository


    AdministeredItemController(Class<I> itemClass, AdministeredItemRepository<I> administeredItemRepository, AdministeredItemRepository<P> parentItemRepository,
                               AdministeredItemContentRepository administeredItemContentRepository) {
        super(administeredItemRepository)
        this.itemClass = itemClass
        this.administeredItemRepository = administeredItemRepository
        this.parentItemRepository = parentItemRepository
        this.administeredItemContentRepository = administeredItemContentRepository
        this.administeredItemContentRepository.administeredItemRepository = administeredItemRepository
    }



    I show(UUID id) {
        I item = administeredItemRepository.findById(id)
        ErrorHandler.handleErrorOnNullObject(HttpStatus.NOT_FOUND, item, "Item with id ${id.toString()} not found")
        accessControlService.checkRole(Role.READER, item)

        updateDerivedProperties(item)
        item
    }

    @Transactional
    I create(UUID parentId, @Body @NonNull I item) {
        I cleanItem = cleanBody(item)
        P parent = validate(cleanItem, parentId)

        I created = createEntity(parent, item)
        created = validateAndAddClassifiers(created)
        created
    }

    protected P validate(I item, UUID parentId) {
        cleanBody(item)
        P parent = parentItemRepository.readById(parentId)
        accessControlService.checkRole(Role.EDITOR, parent)
        parent
    }

    protected AdministeredItemCacheableRepository.ClassifierCacheableRepository getClassifierCacheableRepository(String domainType) {
        getAdministeredItemRepository(domainType) as AdministeredItemCacheableRepository.ClassifierCacheableRepository
    }

    protected AdministeredItemCacheableRepository.DataClassCacheableRepository getDataClassCacheableRepository(String domainType) {
        getAdministeredItemRepository(domainType) as AdministeredItemCacheableRepository.DataClassCacheableRepository
    }

    protected I createEntity(@NonNull P parent, @NonNull I cleanItem) {
        updateCreationProperties(cleanItem)

        cleanItem.parent = parent

        updateDerivedProperties(cleanItem)
        administeredItemRepository.save(cleanItem)
    }

    I update(UUID id, @Body @NonNull I item) {
        cleanBody(item, false)
        I existing = administeredItemRepository.readById(id)

        if (existing == null) {
            throw new HttpStatusException(HttpStatus.NOT_FOUND, "Object not found")
        }

        accessControlService.checkRole(Role.EDITOR, existing)

        I updated = updateEntity(existing, item)

        updated = updateClassifiers(updated)
        updated
    }

    protected I updateEntity(@NonNull I existing, @NonNull I cleanItem) {
        boolean hasChanged = updateProperties(existing, cleanItem)
        updateDerivedProperties(existing)

        if (hasChanged) {
            administeredItemRepository.update(existing)
        } else {
            existing
        }
    }


    @Transactional
    HttpResponse delete(@NonNull UUID id, @Body @Nullable I item) {
        I itemToDelete = (I) administeredItemContentRepository.readWithContentById(id)
        if (item == null) {item = itemToDelete}

        deletePre(itemToDelete)

        if (itemToDelete == null) {
            throw new HttpStatusException(HttpStatus.NOT_FOUND, "Object not found for deletion")
        }

        accessControlService.checkRole(Role.EDITOR, item)

        if (item?.version) itemToDelete.version = item.version

        Long deleted = administeredItemContentRepository.deleteWithContent(itemToDelete)
        if (deleted) {
            HttpResponse.status(HttpStatus.NO_CONTENT)
        } else {
            throw new HttpStatusException(HttpStatus.NOT_FOUND, 'Not found for deletion')
        }
    }

    ListResponse<I> list(UUID parentId, @Nullable PaginationParams params = new PaginationParams()) {
        List<I> items = listItems(parentId)
        ListResponse.from(items, params)
    }

    ListResponse<I> listAll() {
        List<I> items = itemRepository.readAll()
        items = items.findAll { accessControlService.canDoRole(Role.READER, it) }
        items.each {
            pathRepository.readParentItems(it)
            it.updatePath()
        }
        ListResponse.from(items)
    }

    I updateDerivedProperties(I item) {
        pathRepository.readParentItems(item)
        item.updatePath()
        item.updateBreadcrumbs()

        AvailableActions.updateAvailableActions(item, accessControlService)

        item
    }

    protected I updateClassifiers(I updated) {
        updated.classifiers = validateClassifiers(updated)
        updated.classifiers.each {
            if (!getClassifierCacheableRepository(it.domainType).findByAdministeredItemAndClassifier(updated.domainType, updated.id, it.id)) {
                getClassifierCacheableRepository(it.domainType).addAdministeredItem(updated, it)
            }
        }
        updated
    }

    protected I validateAndAddClassifiers(I item) {
        item.classifiers = validateClassifiers(item)
        item.classifiers.each {
            getClassifierCacheableRepository(it.domainType).addAdministeredItem(item, it)
        }
        item
    }

    protected List<Classifier> validateClassifiers(I item) {
        List<Classifier> classifierList = []
        if (item.classifiers) {
            classifierList = item.classifiers.collect {
                Classifier retrieved = getClassifierCacheableRepository(it.domainType).readById(it.id)
                ErrorHandler.handleErrorOnNullObject(HttpStatus.NOT_FOUND, retrieved, " Item with id:  $it.id  not found ")
                accessControlService.checkRole(Role.EDITOR, retrieved)
                retrieved
            }
        }
        classifierList
    }

    protected List<I> listItems(UUID parentId) {
        P parent = parentItemRepository.readById(parentId)
        if (!parent) return null
        accessControlService.checkRole(Role.READER, parent)
        List<I> items = administeredItemRepository.readAllByParent(parent)
        items.each {
            updateDerivedProperties(it as I)
        }
    }

    // Any precondition checks

    protected void deletePre(AdministeredItem administeredItemToDelete) {
        if (itemClass.simpleName == 'DataClass') {
            deleteDataClassPre((DataClass) administeredItemToDelete)
        }
    }

    // Precondition check for if this AdministeredItem is a DataClass
    protected void deleteDataClassPre(DataClass dataClassToDelete) {
        // Dataclass must not be referenced either as an extension, or in a datatype
        if (dataClassToDelete.referenceTypes != null && !dataClassToDelete.referenceTypes.isEmpty()) {
            StringBuilder str = new StringBuilder()
            dataClassToDelete.referenceTypes.forEach {DataType dataType ->
                pathRepository.readParentItems(dataType)
                dataType.updatePath()
                str.append(dataType.getPath().toString())
                str.append(" , ")
            }
            throw new HttpStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "DataClass ${dataClassToDelete.id} is referenced by " + str)
        }

        if (dataClassToDelete.extendedBy != null && !dataClassToDelete.extendedBy.isEmpty()) {
            StringBuilder str = new StringBuilder()
            dataClassToDelete.extendedBy.forEach {DataClass extendedByDataClass ->
                pathRepository.readParentItems(extendedByDataClass)
                extendedByDataClass.updatePath()
                str.append(extendedByDataClass.getPath().toString())
                str.append(" , ")
            }
            throw new HttpStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "DataClass ${dataClassToDelete.id} is extended by " + str)
        }
    }


}

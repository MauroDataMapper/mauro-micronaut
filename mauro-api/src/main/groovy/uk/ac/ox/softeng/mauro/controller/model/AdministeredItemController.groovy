package uk.ac.ox.softeng.mauro.controller.model

import uk.ac.ox.softeng.mauro.ErrorHandler

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.NonNull
import io.micronaut.core.annotation.Nullable
import io.micronaut.http.HttpStatus
import io.micronaut.http.annotation.Body
import io.micronaut.http.exceptions.HttpStatusException
import io.micronaut.security.annotation.Secured
import io.micronaut.security.rules.SecurityRule
import io.micronaut.transaction.annotation.Transactional
import jakarta.inject.Inject
import uk.ac.ox.softeng.mauro.domain.classifier.Classifier
import uk.ac.ox.softeng.mauro.domain.model.AdministeredItem
import uk.ac.ox.softeng.mauro.domain.security.Role
import uk.ac.ox.softeng.mauro.persistence.cache.AdministeredItemCacheableRepository
import uk.ac.ox.softeng.mauro.persistence.model.AdministeredItemContentRepository
import uk.ac.ox.softeng.mauro.persistence.model.AdministeredItemRepository
import uk.ac.ox.softeng.mauro.persistence.model.PathRepository
import uk.ac.ox.softeng.mauro.persistence.service.RepositoryService
import uk.ac.ox.softeng.mauro.web.ListResponse

@CompileStatic
@Secured(SecurityRule.IS_ANONYMOUS)
abstract class AdministeredItemController<I extends AdministeredItem, P extends AdministeredItem> extends ItemController<I> {

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


    AdministeredItemController(Class<I> itemClass, AdministeredItemRepository<I> administeredItemRepository, AdministeredItemRepository<P> parentItemRepository, AdministeredItemContentRepository administeredItemContentRepository) {
        super(administeredItemRepository)
        this.itemClass = itemClass
        this.administeredItemRepository = administeredItemRepository
        this.parentItemRepository = parentItemRepository
        this.administeredItemContentRepository = administeredItemContentRepository
        this.administeredItemContentRepository.administeredItemRepository = administeredItemRepository
    }

    I show(UUID id) {
        I item = administeredItemRepository.findById(id)
        if (!item) return null

        accessControlService.checkRole(Role.READER, item)

        updateDerivedProperties(item)
        item
    }

    @Transactional
    I create(UUID parentId, @Body @NonNull I item) {
        cleanBody(item)

        P parent = parentItemRepository.readById(parentId)

        accessControlService.checkRole(Role.EDITOR, parent)

        I created = createEntity(parent, item)
        created.classifiers = validateClassifiers(created)
        created.classifiers.each {
            getClassifierCacheableRepository(it.domainType).addAdministeredItem(item, it)
        }
        created
    }

    protected AdministeredItemCacheableRepository.ClassifierCacheableRepository getClassifierCacheableRepository(String domainType) {
        getAdministeredItemRepository(domainType) as AdministeredItemCacheableRepository.ClassifierCacheableRepository
    }

    protected I createEntity(@NonNull P parent, @NonNull I cleanItem) {
        updateCreationProperties(cleanItem)

        cleanItem.parent = parent

        updateDerivedProperties(cleanItem)
        administeredItemRepository.save(cleanItem)
    }

    I update(UUID id, @Body @NonNull I item) {
        cleanBody(item)
        I existing = administeredItemRepository.readById(id)

        accessControlService.checkRole(Role.EDITOR, existing)

        I updated = updateEntity(existing, item)
        updated.classifiers =  validateClassifiers(updated)
        updated.classifiers.each {
            if (!getClassifierCacheableRepository(it.domainType).findByAdministeredItemAndClassifier(updated.domainType, updated.id, it.id)) {
               getClassifierCacheableRepository(it.domainType).addAdministeredItem(updated, it)
            }
        }
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
    HttpStatus delete(@NonNull UUID id, @Body @Nullable I item) {
        I itemToDelete = (I) administeredItemContentRepository.readWithContentById(id)

        accessControlService.checkRole(Role.EDITOR, item)

        if (item?.version) itemToDelete.version = item.version

        Long deleted = administeredItemContentRepository.deleteWithContent(itemToDelete)
        if (deleted) {
            HttpStatus.NO_CONTENT
        } else {
            throw new HttpStatusException(HttpStatus.NOT_FOUND, 'Not found for deletion')
        }
    }

    ListResponse<I> list(UUID parentId) {
        P parent = parentItemRepository.readById(parentId)
        if (!parent) return null
        accessControlService.checkRole(Role.READER, parent)
        List<I> items = administeredItemRepository.readAllByParent(parent)
        items.each {
            updateDerivedProperties(it)
        }
        ListResponse.from(items)
    }

    protected I updateDerivedProperties(I item) {
        pathRepository.readParentItems(item)
        item.updatePath()

        item
    }

    protected List<Classifier> validateClassifiers(I item) {
        List<Classifier> classifierList = []
        if (item.classifiers) {
            classifierList = item.classifiers.collect {
                Classifier retrieved = getClassifierCacheableRepository(it.domainType).readById(it.id)
                ErrorHandler.handleError(HttpStatus.NOT_FOUND, retrieved, " Item with id:  $it.id  not found ")
                accessControlService.checkRole(Role.EDITOR, retrieved)
                retrieved
            }
        }
        classifierList
    }

}

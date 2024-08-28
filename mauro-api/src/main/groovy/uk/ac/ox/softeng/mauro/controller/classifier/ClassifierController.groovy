package uk.ac.ox.softeng.mauro.controller.classifier

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.micronaut.core.annotation.NonNull
import io.micronaut.core.annotation.Nullable
import io.micronaut.http.HttpStatus
import io.micronaut.http.annotation.*
import io.micronaut.http.exceptions.HttpStatusException
import io.micronaut.security.annotation.Secured
import io.micronaut.security.rules.SecurityRule
import jakarta.inject.Inject
import jakarta.transaction.Transactional
import uk.ac.ox.softeng.mauro.controller.model.AdministeredItemController
import uk.ac.ox.softeng.mauro.controller.terminology.Paths
import uk.ac.ox.softeng.mauro.domain.classifier.ClassificationScheme
import uk.ac.ox.softeng.mauro.domain.classifier.Classifier
import uk.ac.ox.softeng.mauro.domain.model.AdministeredItem
import uk.ac.ox.softeng.mauro.domain.security.Role
import uk.ac.ox.softeng.mauro.persistence.cache.AdministeredItemCacheableRepository
import uk.ac.ox.softeng.mauro.persistence.cache.ModelCacheableRepository
import uk.ac.ox.softeng.mauro.persistence.classifier.ClassificationSchemeContentRepository
import uk.ac.ox.softeng.mauro.persistence.classifier.ClassificationSchemeRepository
import uk.ac.ox.softeng.mauro.persistence.classifier.ClassifierRepository
import uk.ac.ox.softeng.mauro.web.ListResponse

@Slf4j
@CompileStatic
@Controller
@Secured(SecurityRule.IS_ANONYMOUS)
class ClassifierController extends AdministeredItemController<Classifier, ClassificationScheme> {

    AdministeredItemCacheableRepository.ClassifierCacheableRepository classifierCacheableRepository

    @Inject
    ModelCacheableRepository.ClassificationSchemeCacheableRepository classificationSchemeCacheableRepository

    @Inject
    ClassificationSchemeRepository classificationSchemeRepository

    @Inject
    ClassifierRepository classifierRepository

    ClassifierController(AdministeredItemCacheableRepository.ClassifierCacheableRepository classifierCacheableRepository,
                         ModelCacheableRepository.ClassificationSchemeCacheableRepository classificationSchemeCacheableRepository,
                         ClassificationSchemeContentRepository classificationSchemeContentRepository) {
        super(Classifier, classifierCacheableRepository, classificationSchemeCacheableRepository, classificationSchemeContentRepository)
        this.classifierCacheableRepository = classifierCacheableRepository
        this.classificationSchemeCacheableRepository = classificationSchemeCacheableRepository
    }

    @Get(Paths.CLASSIFIERS_ROUTE_ID)
    Classifier show(@NonNull UUID classificationSchemeId, @NonNull UUID id) {
        super.show(id)
    }

    @Post(Paths.CLASSIFIERS_ROUTE)
    @Transactional
    Classifier create(@NonNull UUID classificationSchemeId, @Body @NonNull Classifier classifier) {
        super.create(classificationSchemeId, classifier)
    }

    @Put(Paths.CLASSIFIERS_ROUTE_ID)
    @Transactional
    Classifier update(@NonNull UUID classificationSchemeId, @NonNull UUID id, @Body @NonNull Classifier classifier) {
        super.update(id, classifier)
    }

    @Delete(Paths.CLASSIFIERS_ROUTE_ID)
    @Transactional
    HttpStatus delete(@NonNull UUID classificationSchemeId, @NonNull UUID id, @Body @Nullable Classifier classifier) {
        super.delete(id, classifier, classificationSchemeId)
    }

    @Get(Paths.CLASSIFIERS_ROUTE)
    ListResponse<Classifier> list(UUID classificationSchemeId) {
        super.list(classificationSchemeId)
    }

    @Get(Paths.CHILD_CLASSIFIERS_ID_ROUTE)
    Classifier showChildClassifier(@NonNull UUID classificationSchemeId,@NonNull UUID parentClassifierId, @NonNull UUID childClassifierId) {
        show(childClassifierId)
    }

    @Get(Paths.CHILD_CLASSIFIERS_ROUTE)
    ListResponse<Classifier> getChildClassifiers(@NonNull UUID classificationSchemeId, @NonNull UUID parentClassifierId ) {
        Classifier parent = classifierRepository.readById(parentClassifierId)
        handleError(HttpStatus.NOT_FOUND,parent, "Parent classifier $parentClassifierId not found")
        accessControlService.checkRole(Role.READER, parent)
        ListResponse.from(classifierRepository.readAllByParentClassifier_Id(parent.id))
    }

    /**
     * Create child classifier
     * @param classificationSchemeId
     * @param id              parent ClassifierId
     * @param classifier      child
     * @return
     */
    @Post(Paths.CHILD_CLASSIFIERS_ROUTE)
    @Transactional
    Classifier create(@NonNull UUID classificationSchemeId, @NonNull UUID parentClassifierId, @Body @NonNull Classifier classifier) {
        cleanBody(classifier)
        ClassificationScheme classificationScheme = classificationSchemeRepository.readById(classificationSchemeId)
        handleError(HttpStatus.NOT_FOUND,classificationScheme, "Classification Scheme $classificationSchemeId not found")
        accessControlService.checkRole(Role.EDITOR, classificationScheme)
        Classifier parentClassifier = classifierCacheableRepository.readById(parentClassifierId)
        handleError(HttpStatus.NOT_FOUND, parentClassifier, "Parent Classifier $parentClassifierId not found")
        accessControlService.checkRole(Role.EDITOR, parentClassifier)
        classifier.parentClassifier = parentClassifier
        return createEntity(classificationScheme, classifier)
    }

    @Put(Paths.CHILD_CLASSIFIERS_ID_ROUTE)
    @Transactional
    Classifier update(@NonNull UUID classificationSchemeId, @NonNull UUID parentClassifierId, @NonNull UUID childClassifierId, @Body @NonNull Classifier classifier) {
        super.update(childClassifierId, classifier)
    }

    @Transactional
    @Delete(Paths.CHILD_CLASSIFIERS_ID_ROUTE)
    HttpStatus delete(@NonNull UUID classificationSchemeId, @NonNull UUID parentClassifierId, @NonNull UUID childClassifierId, @Body @Nullable Classifier classifier) {
//        accessControlService.checkRole(Role.EDITOR, readAdministeredItem(ClassificationScheme.class.simpleName, classificationSchemeId))
//        Classifier parentClassifier = classifierCacheableRepository.readById(parentClassifierId)
//        accessControlService.checkRole(Role.EDITOR, parentClassifier)
//        Classifier childClassifier = classifierCacheableRepository.readById(childClassifierId)
//        accessControlService.checkRole(Role.EDITOR, childClassifier)
        super.delete(childClassifierId, classifier, parentClassifierId)
    }

    @Get(Paths.CHILD_CLASSIFIERS_ROUTE)
    ListResponse<Classifier> list(@NonNull UUID classificationSchemeId, @NonNull UUID parentClassifierId) {
        Classifier parentClassifier = classifierCacheableRepository.readById(parentClassifierId)
        accessControlService.checkRole(Role.READER, parentClassifier)
        ListResponse.from(classifierCacheableRepository.readAllByParentClassifier_Id(parentClassifierId))
    }

    /**
     * Associate Classifier to administeredItem
     */
    @Put(Paths.ADMINISTERED_ITEM_CLASSIFIER_ID_ROUTE)
    @Transactional
    Classifier createAdministeredItemClassifier(@NonNull String administeredItemDomainType, @NonNull UUID administeredItemId, @NonNull UUID id) {
        AdministeredItem administeredItem = readAdministeredItem(administeredItemDomainType, administeredItemId)
        accessControlService.checkRole(Role.EDITOR, administeredItem)
        Classifier classifier = administeredItemRepository.findById(id)
        if (!classifier) return null
        accessControlService.checkRole(Role.READER, classifier)
        classifierRepository.addAdministeredItem(administeredItem, classifier.id)
        classifier
    }

    /**
     * Get Classifier for AdministeredItem
     */
    @Get(Paths.ADMINISTERED_ITEM_CLASSIFIER_ID_ROUTE)
    Classifier getAdministeredItemClassifier(@NonNull String administeredItemDomainType, @NonNull UUID administeredItemId, @NonNull UUID id) {
        accessControlService.checkRole(Role.READER, readAdministeredItem(Classifier.class.simpleName, id))
        AdministeredItem administeredItem = readAdministeredItem(administeredItemDomainType, administeredItemId)
        accessControlService.checkRole(Role.EDITOR, administeredItem)
        Classifier classifier = classifierRepository.findByAdministeredItemAndClassifier(administeredItem.domainType, administeredItemId, id)
        classifier
    }

    /**
     * Get AdministeredItem classifiers
     */
    @Get(Paths.ADMINISTERED_ITEM_CLASSIFIER_ROUTE)
    ListResponse<Classifier> findAllAdministeredItemClassifiers(@NonNull String administeredItemDomainType, @NonNull UUID administeredItemId) {
        AdministeredItem administeredItem = findAdministeredItem(administeredItemDomainType, administeredItemId)
        accessControlService.checkRole(Role.EDITOR, readAdministeredItem(administeredItem.domainType, administeredItemId))
        ListResponse.from(classifierRepository.findAllForAdministeredItem(administeredItem.domainType, administeredItemId))
    }

    @Delete(Paths.ADMINISTERED_ITEM_CLASSIFIER_ID_ROUTE)
    @Transactional
    HttpStatus delete(@NonNull String administeredItemDomainType, @NonNull UUID administeredItemId, @NonNull UUID id){
        AdministeredItem administeredItem = findAdministeredItem(administeredItemDomainType, administeredItemId)
        accessControlService.checkRole(Role.EDITOR, readAdministeredItem(administeredItem.domainType, administeredItemId))
        Classifier classifierToDelete = classifierRepository.readById(id)
        handleError(HttpStatus.NOT_FOUND, classifierToDelete, "Classifier with id $id not found")
        accessControlService.checkRole(Role.EDITOR, classifierToDelete)
        Long deleted = classifierRepository.deleteAdministeredItemClassifier(administeredItem, classifierToDelete.id)
        if (deleted) {
            HttpStatus.NO_CONTENT
        } else {
            throw new HttpStatusException(HttpStatus.NOT_FOUND, 'Not found for deletion')
        }
    }
}

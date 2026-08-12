package org.maurodata.controller.classifier

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import org.maurodata.ErrorHandler
import org.maurodata.api.classifier.ClassifierApi
import org.maurodata.audit.Audit
import org.maurodata.domain.facet.EditType
import org.maurodata.web.PaginationParams

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.micronaut.core.annotation.NonNull
import io.micronaut.core.annotation.Nullable
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import io.micronaut.http.annotation.*
import io.micronaut.http.exceptions.HttpStatusException
import io.micronaut.security.annotation.Secured
import io.micronaut.security.rules.SecurityRule
import jakarta.transaction.Transactional
import org.maurodata.controller.model.AdministeredItemController
import org.maurodata.api.Paths
import org.maurodata.domain.classifier.ClassificationScheme
import org.maurodata.domain.classifier.Classifier
import org.maurodata.domain.model.AdministeredItem
import org.maurodata.domain.security.Role
import org.maurodata.persistence.cache.AdministeredItemCacheableRepository
import org.maurodata.persistence.cache.ModelCacheableRepository

import org.maurodata.web.ListResponse

@Slf4j
@CompileStatic
@Controller
@Secured(SecurityRule.IS_ANONYMOUS)
class ClassifierController extends AdministeredItemController<Classifier, ClassificationScheme> implements ClassifierApi {

    AdministeredItemCacheableRepository.ClassifierCacheableRepository classifierCacheableRepository

    ModelCacheableRepository.ClassificationSchemeCacheableRepository classificationSchemeCacheableRepository


    ClassifierController(AdministeredItemCacheableRepository.ClassifierCacheableRepository classifierCacheableRepository,
                         ModelCacheableRepository.ClassificationSchemeCacheableRepository classificationSchemeCacheableRepository) {
        super(Classifier, classifierCacheableRepository, classificationSchemeCacheableRepository)
        this.classifierCacheableRepository = classifierCacheableRepository
        this.classificationSchemeCacheableRepository = classificationSchemeCacheableRepository
    }

    @Audit
    @Operation(operationId = 'showClassifier', summary = "Get a classifier", description = "Returns a classifier.")
    @Get(Paths.CLASSIFIERS_ROUTE_ID)
    Classifier show(@NonNull UUID classificationSchemeId, @NonNull UUID id) {
        super.show(id)
    }

    @Audit
    @Operation(operationId = 'createClassifier', summary = "Create a classifier", description = "Creates a classifier.")
    @Post(Paths.CLASSIFIERS_ROUTE)
    @Transactional
    Classifier create(@NonNull UUID classificationSchemeId, @Body @NonNull Classifier classifier) {
        super.create(classificationSchemeId, classifier)
    }

    @Audit
    @Operation(operationId = 'updateClassifier', summary = "Update a classifier", description = "Updates a classifier.")
    @Put(Paths.CLASSIFIERS_ROUTE_ID)
    @Transactional
    Classifier update(@NonNull UUID classificationSchemeId, @NonNull UUID id, @Body @NonNull Classifier classifier) {
        super.update(id, classifier)
    }

    @Audit(
            parentDomainType = ClassificationScheme,
            parentIdParamName = 'classificationSchemeId',
            deletedObjectDomainType = Classifier
    )
    @ApiResponse(responseCode = "204", description = "No content - deleted successfully")
    @Operation(summary = "Delete a classifier", description = "Deletes a classifier.")
    @Delete(Paths.CLASSIFIERS_ROUTE_ID)
    @Transactional
    HttpResponse delete(@NonNull UUID classificationSchemeId, @NonNull UUID id, @Body @Nullable Classifier classifier) {
        super.delete(id, classifier)
    }

    @Audit
    @Operation(operationId = 'listClassifier', summary = "List the classifiers", description = "Returns the classifiers.")
    @Get(Paths.CLASSIFIERS_ROUTE_PAGED)
    ListResponse<Classifier> list(UUID classificationSchemeId, @Nullable PaginationParams params = new PaginationParams()) {

        super.list(classificationSchemeId, params)
    }

    @Audit
    @Operation(operationId = 'showClassifierChild', summary = "Get a classifier", description = "Returns a classifier.")
    @Get(Paths.CHILD_CLASSIFIERS_ID_ROUTE)
    Classifier showChildClassifier(@NonNull UUID classificationSchemeId, @NonNull UUID parentClassifierId, @NonNull UUID childClassifierId) {
        show(childClassifierId)
    }

    /**
     * Create child classifier
     * @param classificationSchemeId
     * @param id parent ClassifierId
     * @param classifier child
     * @return
     */
    @Audit
    @Operation(summary = "Create a classifier", description = "Creates a classifier. You must have edit privileges on the item in question.")
    @Post(Paths.CHILD_CLASSIFIERS_ROUTE)
    @Transactional
    Classifier create(@NonNull UUID classificationSchemeId, @NonNull UUID parentClassifierId, @Body @NonNull Classifier classifier) {
        cleanBody(classifier)
        ClassificationScheme classificationScheme = classificationSchemeCacheableRepository.readById(classificationSchemeId)
        ErrorHandler.handleErrorOnNullObject(HttpStatus.NOT_FOUND, classificationScheme, "Classification Scheme $classificationSchemeId not found")
        accessControlService.checkRole(Role.EDITOR, classificationScheme)
        Classifier parentClassifier = classifierCacheableRepository.readById(parentClassifierId)
        ErrorHandler.handleErrorOnNullObject(HttpStatus.NOT_FOUND, parentClassifier, "Parent Classifier $parentClassifierId not found")
        accessControlService.checkRole(Role.EDITOR, parentClassifier)
        classifier.parentClassifier = parentClassifier
        return createEntity(classificationScheme, classifier)
    }

    @Audit
    @Operation(operationId = 'updateClassifierChild', summary = "Update a classifier", description = "Updates a classifier.")
    @Put(Paths.CHILD_CLASSIFIERS_ID_ROUTE)
    @Transactional
    Classifier update(@NonNull UUID classificationSchemeId, @NonNull UUID parentClassifierId, @NonNull UUID childClassifierId, @Body @NonNull Classifier classifier) {
        super.update(childClassifierId, classifier)
    }

    @Transactional
    @Audit(
            parentDomainType = Classifier,
            parentIdParamName = 'parentClassifierId',
            deletedObjectDomainType = Classifier
    )
    @ApiResponse(responseCode = "204", description = "No content - deleted successfully")
    @Operation(summary = "Delete a classifier", description = "Deletes a classifier.")
    @Delete(Paths.CHILD_CLASSIFIERS_ID_ROUTE)
    HttpResponse delete(@NonNull UUID classificationSchemeId, @NonNull UUID parentClassifierId, @NonNull UUID childClassifierId, @Body @Nullable Classifier classifier) {
        super.delete(childClassifierId, classifier)
    }

    @Audit
    @Operation(operationId = 'listClassifierChildPaged', summary = "List the classifiers", description = "Returns the classifiers. You must have read privileges on the item in question.")
    @Get(Paths.CHILD_CLASSIFIERS_ROUTE_PAGED)
    ListResponse<Classifier> list(@NonNull UUID classificationSchemeId, @NonNull UUID parentClassifierId, @Nullable PaginationParams params = new PaginationParams()) {
        Classifier parentClassifier = classifierCacheableRepository.readById(parentClassifierId)
        accessControlService.checkRole(Role.READER, parentClassifier)
        ListResponse.from(classifierCacheableRepository.readAllByParentClassifier_Id(parentClassifierId), params)
    }

    /**
     * Associate Classifier to administeredItem
     */
    @Audit(title = EditType.UPDATE, description = "Classify element")
    @Operation(summary = "Update a classifier", description = "Updates a classifier. You must have read or edit privileges on the item in question, depending on the action.")
    @Put(Paths.ADMINISTERED_ITEM_CLASSIFIER_ID_ROUTE)
    @Transactional
    Classifier createAdministeredItemClassifier(@NonNull String administeredItemDomainType, @NonNull UUID administeredItemId, @NonNull UUID id) {
        AdministeredItem administeredItem = readAdministeredItem(administeredItemDomainType, administeredItemId)
        accessControlService.checkRole(Role.EDITOR, administeredItem)
        Classifier classifier = administeredItemRepository.findById(id)
        if (!classifier) return null
        accessControlService.checkRole(Role.READER, classifier)
        classifierCacheableRepository.addAdministeredItem(administeredItem, classifier)
        classifier
    }

    /**
     * Get Classifier for AdministeredItem
     */
    @Audit
    @Operation(summary = "Get a classifier", description = "Returns a classifier. You must have read privileges on the item in question.")
    @Get(Paths.ADMINISTERED_ITEM_CLASSIFIER_ID_ROUTE)
    Classifier getAdministeredItemClassifier(@NonNull String administeredItemDomainType, @NonNull UUID administeredItemId, @NonNull UUID id) {
        accessControlService.checkRole(Role.READER, readAdministeredItem(Classifier.class.simpleName, id))
        AdministeredItem administeredItem = readAdministeredItem(administeredItemDomainType, administeredItemId)
        accessControlService.checkRole(Role.READER, administeredItem)
        Classifier classifier = classifierCacheableRepository.findByAdministeredItemAndClassifier(administeredItem.domainType, administeredItemId, id)
        classifier
    }

    /**
     * Get AdministeredItem classifiers
     */
    @Audit
    @Operation(summary = "List the classifiers", description = "Returns the classifiers. You must have read privileges on the item in question.")
    @Get(Paths.ADMINISTERED_ITEM_CLASSIFIER_ROUTE_PAGED)
    ListResponse<Classifier> findAllAdministeredItemClassifiers(@NonNull String administeredItemDomainType, @NonNull UUID administeredItemId, @Nullable PaginationParams params = new PaginationParams()) {

        AdministeredItem administeredItem = findAdministeredItem(administeredItemDomainType, administeredItemId)
        accessControlService.checkRole(Role.READER, readAdministeredItem(administeredItem.domainType, administeredItemId))
        ListResponse.from(classifierCacheableRepository.findAllForAdministeredItem(administeredItem), params)
    }

    @Audit(
            parentDomainType = Classifier,
            parentIdParamName = 'id',
            title = EditType.DELETE,
            description = 'Unclassify item'
    )
    @ApiResponse(responseCode = "204", description = "No content - deleted successfully")
    @Operation(summary = "Delete a classifier", description = "Deletes a classifier. You must have edit privileges on the item in question.")
    @Delete(Paths.ADMINISTERED_ITEM_CLASSIFIER_ID_ROUTE)
    @Transactional
    HttpResponse delete(@NonNull String administeredItemDomainType, @NonNull UUID administeredItemId, @NonNull UUID id) {
        AdministeredItem administeredItem = findAdministeredItem(administeredItemDomainType, administeredItemId)
        accessControlService.checkRole(Role.EDITOR, readAdministeredItem(administeredItem.domainType, administeredItemId))
        Classifier classifierToDelete = classifierCacheableRepository.readById(id)
        ErrorHandler.handleErrorOnNullObject(HttpStatus.NOT_FOUND, classifierToDelete, "Classifier with id $id not found")
        accessControlService.checkRole(Role.EDITOR, classifierToDelete)
        Long deleted = classifierCacheableRepository.deleteJoinAdministeredItemToClassifier(administeredItem, classifierToDelete.id)
        if (deleted) {
            HttpResponse.status(HttpStatus.NO_CONTENT)
        } else {
            throw new HttpStatusException(HttpStatus.NOT_FOUND, 'Not found for deletion')
        }
    }


    @Operation(summary = "List the classifiers", description = "Returns the classifiers.")
    @Get(Paths.ALL_CLASSIFIERS_ROUTE_PAGED)
    ListResponse<Classifier> listAllClassifiers(@Nullable PaginationParams params = new PaginationParams()) {
        super.listAll(params)

    }
}

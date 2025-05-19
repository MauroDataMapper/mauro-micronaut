package uk.ac.ox.softeng.mauro.controller.facet

import uk.ac.ox.softeng.mauro.api.Paths
import uk.ac.ox.softeng.mauro.api.facet.AnnotationApi
import uk.ac.ox.softeng.mauro.audit.Audit

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.NonNull
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import io.micronaut.http.annotation.*
import io.micronaut.http.exceptions.HttpStatusException
import io.micronaut.security.annotation.Secured
import io.micronaut.security.rules.SecurityRule
import io.micronaut.transaction.annotation.Transactional
import jakarta.inject.Inject
import uk.ac.ox.softeng.mauro.domain.facet.Annotation
import uk.ac.ox.softeng.mauro.domain.model.AdministeredItem
import uk.ac.ox.softeng.mauro.domain.security.Role
import uk.ac.ox.softeng.mauro.persistence.cache.FacetCacheableRepository
import uk.ac.ox.softeng.mauro.persistence.facet.AnnotationRepository
import uk.ac.ox.softeng.mauro.web.ListResponse

@CompileStatic
@Controller
@Secured(SecurityRule.IS_ANONYMOUS)
class AnnotationController extends FacetController<Annotation> implements AnnotationApi {

    @Inject
    AnnotationRepository annotationRepositoryUncached
    /**
     * Properties disallowed in a simple update request.
     */
    List<String> getDisallowedProperties() {
        super.getDisallowedProperties() + ['multiFacetAwareItemDomainType', 'multiFacetAwareItemId']
    }

    FacetCacheableRepository.AnnotationCacheableRepository annotationRepository

    AnnotationController(FacetCacheableRepository.AnnotationCacheableRepository annotationRepository) {
        super(annotationRepository)
        this.annotationRepository = annotationRepository
    }

    /**
     * Get annotation list
     * @param domainType
     * @param domainId
     * @return
     */
    @Audit
    @Get(Paths.ANNOTATION_LIST)
    ListResponse<Annotation> list(@NonNull String domainType, @NonNull UUID domainId) {
        AdministeredItem administeredItem = findAdministeredItem(domainType, domainId)
        accessControlService.checkRole(Role.READER, administeredItem)
        ListResponse.from(!administeredItem.annotations ? [] : administeredItem.annotations)
    }

    @Audit
    @Get(Paths.ANNOTATION_ID)
    Annotation show(@NonNull String domainType, @NonNull UUID domainId, @NonNull UUID id) {
        accessControlService.checkRole(Role.READER, readAdministeredItem(domainType, domainId))
        Annotation validAnnotation = super.validateAndGet(domainType, domainId, id) as Annotation
        Annotation nested = showNestedItem(id, validAnnotation)
        nested
    }

    @Audit
    @Post(Paths.ANNOTATION_LIST)
    Annotation create(@NonNull String domainType, @NonNull UUID domainId, @Body @NonNull Annotation annotation) {
        super.create(domainType, domainId, annotation) as Annotation
    }

    /**
     * Create Child Annotation
     * @param domainType
     * @param domainId
     * @param annotationId parent annotationId
     * @param childAnnotation child annotation to create
     * @return newly created child
     */
    @Audit
    @Post(Paths.ANNOTATION_CHILD_LIST)
    Annotation create(@NonNull String domainType, @NonNull UUID domainId, @NonNull UUID annotationId, @Body @NonNull Annotation childAnnotation) {
        accessControlService.checkRole(Role.EDITOR, readAdministeredItem(domainType, domainId))
        super.cleanBody(childAnnotation)
        Annotation parent = super.validateAndGet(domainType, domainId, annotationId) as Annotation
        if (!parent || parent.parentAnnotationId ) throw new HttpStatusException(HttpStatus.BAD_REQUEST, "Parent Annotation not found or has parent $annotationId")
        childAnnotation.parentAnnotationId = parent.id
        childAnnotation.multiFacetAwareItemId = parent.multiFacetAwareItemId
        childAnnotation.multiFacetAwareItemDomainType = parent.multiFacetAwareItemDomainType
        updateCreationProperties(childAnnotation)
        Annotation saved = annotationRepository.save(childAnnotation)
        saved
    }

    /**
     * Get child annotation
     * @param id parentId
     * @param childId note: if childId = parent, the nested parent is returned
     * @return 'Child' annotation
     */
    @Audit
    @Get(Paths.ANNOTATION_CHILD_ID)
    Annotation getChildAnnotation(@NonNull String domainType, @NonNull UUID domainId, @NonNull UUID annotationId,
                                  @NonNull UUID id) {
        show(domainType, domainId, id)
    }

    @Audit(deletedObjectDomainType = Annotation)
    @Delete(Paths.ANNOTATION_ID)
    @Transactional
    HttpResponse delete(@NonNull String domainType, @NonNull UUID domainId, @NonNull UUID id) {
        accessControlService.checkRole(Role.EDITOR, readAdministeredItem(domainType, domainId))
        Annotation annotationToDelete = super.validateAndGet(domainType, domainId, id) as Annotation
        if (!annotationToDelete.parentAnnotationId) {
            Set<Annotation> annotationSet = annotationRepositoryUncached.findAllChildrenById(id)
            annotationRepository.deleteAll(annotationSet)
        }
        super.delete(id)
    }

    @Audit(deletedObjectDomainType = Annotation)
    @Delete(Paths.ANNOTATION_CHILD_ID)
    @Transactional
    HttpResponse delete(@NonNull String domainType, @NonNull UUID domainId, @NonNull UUID annotationId,
                      @NonNull UUID id) {
        accessControlService.checkRole(Role.EDITOR, readAdministeredItem(domainType, domainId))
        super.validateAndGet(domainType, domainId, annotationId) as Annotation
        Annotation child = super.validateAndGet(domainType, domainId, id) as Annotation
        if (!child.parentAnnotationId) {
            throw new HttpStatusException(HttpStatus.BAD_REQUEST, "Child Annotation has no parent annotation")
        }
        super.delete(id)
    }

    private Annotation showNestedItem(UUID id, Annotation annotation) {
        if (annotation) {
            if (!annotation.parentAnnotationId) {
                AdministeredItem administeredItem = findAdministeredItem(annotation.multiFacetAwareItemDomainType,
                        annotation.multiFacetAwareItemId)
                List<Annotation> annotations = administeredItem.annotations
                if (!annotations.isEmpty()) {
                    annotation = annotations.find { it -> it.id == id }
                }
            }
        }
        annotation
    }

}

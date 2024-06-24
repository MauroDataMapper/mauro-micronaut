package uk.ac.ox.softeng.mauro.controller.facet

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.NonNull
import io.micronaut.core.annotation.Nullable
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
class AnnotationController extends FacetController<Annotation> {

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
    @Get('/{domainType}/{domainId}/annotations')
    ListResponse<Annotation> list(@NonNull String domainType, @NonNull UUID domainId) {
        AdministeredItem administeredItem = findAdministeredItem(domainType, domainId)
        accessControlService.checkRole(Role.READER, administeredItem)
        ListResponse.from(administeredItem.annotations)
    }

    @Get('/{domainType}/{domainId}/annotations/{id}')
    Annotation show(@NonNull String domainType, @NonNull UUID domainId, @NonNull UUID id) {
        accessControlService.checkRole(Role.READER, readAdministeredItem(domainType, domainId))
        Annotation validAnnotation = super.validateAndGet(domainType, domainId, id) as Annotation
        Annotation nested = showNestedItem(id, validAnnotation)
        nested
    }

    @Post('/{domainType}/{domainId}/annotations')
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
    @Post('/{domainType}/{domainId}/annotations/{annotationId}/annotations')
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
    @Get('/{domainType}/{domainId}/annotations/{id}/annotations/{childId}')
    Annotation getChildAnnotation(@NonNull String domainType, @NonNull UUID domainId, @NonNull UUID id,
                                  @NonNull UUID childId) {
        show(domainType, domainId, childId)
    }

    @Delete('/{domainType}/{domainId}/annotations/{id}')
    @Transactional
    HttpStatus delete(@NonNull String domainType, @NonNull UUID domainId, @NonNull UUID id,
                      @Body @Nullable Annotation annotation) {
        accessControlService.checkRole(Role.EDITOR, readAdministeredItem(domainType, domainId))
        Annotation annotationToDelete = super.validateAndGet(domainType, domainId, id) as Annotation
        if (!annotationToDelete.parentAnnotationId) {
            Set<Annotation> annotationSet = annotationRepositoryUncached.findAllChildrenById(id)
            annotationRepository.deleteAll(annotationSet)
        }
        super.delete(id, annotation)
    }

    @Delete('/{domainType}/{domainId}/annotations/{id}/annotations/{childId}')
    @Transactional
    HttpStatus delete(@NonNull String domainType, @NonNull UUID domainId, @NonNull UUID id,
                      @NonNull UUID childId, @Body @Nullable Annotation annotation) {
        accessControlService.checkRole(Role.EDITOR, readAdministeredItem(domainType, domainId))
        super.validateAndGet(domainType, domainId, id) as Annotation
        Annotation child = super.validateAndGet(domainType, domainId, childId) as Annotation
        if (!child.parentAnnotationId) {
            throw new HttpStatusException(HttpStatus.BAD_REQUEST, "Child Annotation has no parent annotation")
        }
        super.delete(childId, annotation)
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

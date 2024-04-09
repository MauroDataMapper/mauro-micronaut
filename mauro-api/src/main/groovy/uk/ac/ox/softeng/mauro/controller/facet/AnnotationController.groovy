package uk.ac.ox.softeng.mauro.controller.facet

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.NonNull
import io.micronaut.core.annotation.Nullable
import io.micronaut.http.HttpStatus
import io.micronaut.http.annotation.*
import io.micronaut.http.exceptions.HttpStatusException
import io.micronaut.transaction.annotation.Transactional
import jakarta.inject.Inject
import uk.ac.ox.softeng.mauro.domain.facet.Annotation
import uk.ac.ox.softeng.mauro.domain.model.AdministeredItem
import uk.ac.ox.softeng.mauro.persistence.cache.FacetCacheableRepository
import uk.ac.ox.softeng.mauro.persistence.facet.AnnotationRepository
import uk.ac.ox.softeng.mauro.web.ListResponse

@CompileStatic
@Controller
class AnnotationController extends FacetController<Annotation> {

    @Inject
    AnnotationRepository annotationRepository
    /**
     * Properties disallowed in a simple update request.
     */
    List<String> getDisallowedProperties() {
        super.getDisallowedProperties() + ['multiFacetAwareItemDomainType', 'multiFacetAwareItemId']
    }

    FacetCacheableRepository.AnnotationCacheableRepository annotationCacheableRepository

    AnnotationController(FacetCacheableRepository.AnnotationCacheableRepository annotationCacheableRepository) {
        super(annotationCacheableRepository)
        this.annotationCacheableRepository = annotationCacheableRepository
    }

    /**
     * Get annotation list -excludes annotations with parent_annotation_id
     * @param domainType
     * @param domainId
     * @return
     */
    @Get('/{domainType}/{domainId}/annotations')
    ListResponse<Annotation> list(@NonNull String domainType, @NonNull UUID domainId) {
        AdministeredItem administeredItem = findAdministeredItem(domainType, domainId)
        ListResponse.from(administeredItem.annotations)
    }

    @Get('/{domainType}/{domainId}/annotations/{id}')
    Annotation show(@NonNull String domainType, @NonNull UUID domainId, @NonNull UUID id) {
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
        super.cleanBody(childAnnotation)
        Annotation parent = super.validateAndGet(domainType, domainId, annotationId) as Annotation
        if (!parent) throw new HttpStatusException(HttpStatus.BAD_REQUEST, 'Parent Annotation not found: $annotationId')
        childAnnotation.parentAnnotationId = parent.id
        childAnnotation.multiFacetAwareItemId = parent.multiFacetAwareItemId
        childAnnotation.multiFacetAwareItemDomainType = parent.multiFacetAwareItemDomainType
        updateCreationProperties(childAnnotation)
        Annotation saved = annotationCacheableRepository.save(childAnnotation)
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
        Annotation annotationToDelete = super.validateAndGet(domainType, domainId, id) as Annotation
        if (!annotationToDelete.parentAnnotationId) {
            Set<Annotation> annotationSet = annotationRepository.findAllChildrenById(id)
            annotationCacheableRepository.deleteAll(annotationSet)
        }
        super.delete(id, annotation)
    }

    @Delete('/{domainType}/{domainId}/annotations/{id}/annotations/{childId}')
    @Transactional
    HttpStatus delete(@NonNull String domainType, @NonNull UUID domainId, @NonNull UUID id,
                      @NonNull UUID childId, @Body @Nullable Annotation annotation) {
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

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
@Controller('/{domainType}/{domainId}/annotations')
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
    @Get
    ListResponse<Annotation> list(@NonNull String domainType, @NonNull UUID domainId) {
        AdministeredItem administeredItem = findAdministeredItem(domainType, domainId)
        ListResponse.from(administeredItem.annotations)
    }

    @Get('/{id}')
    Annotation show(UUID id) {
        Annotation annotation = super.show(id) as Annotation
        if (annotation) {
            if (!annotation.parentAnnotationId) {
                AdministeredItem administeredItem = findAdministeredItem(annotation.multiFacetAwareItemDomainType,
                        annotation.multiFacetAwareItemId)
                List<Annotation> annotations = administeredItem.annotations
                annotation = annotations.find { it -> it.id == id }
            }
        }
        annotation
    }


    @Post
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
    @Post('/{annotationId}/annotations')
    Annotation create(@NonNull String domainType, @NonNull UUID domainId, @NonNull UUID annotationId, @Body @NonNull Annotation childAnnotation) {
        super.cleanBody(childAnnotation)
        Annotation annotation = annotationRepository.readById(annotationId)
        if (!annotation)
            throw new HttpStatusException(HttpStatus.BAD_REQUEST, 'Parent Annotation not found: $annotationId')
        childAnnotation.parentAnnotationId = annotation.id
        childAnnotation.multiFacetAwareItemId = annotation.multiFacetAwareItemId
        childAnnotation.multiFacetAwareItemDomainType = annotation.multiFacetAwareItemDomainType
        updateCreationProperties(childAnnotation)
        Annotation saved = annotationCacheableRepository.save(childAnnotation)
        saved
    }


    /**
     * Get child annotation
     * @param id parentId
     * @param childId  note: if childId =parent, the nested parent is returned
     * @return 'Child' annotation
     */
    @Get('/{id}/annotations/{childId}')
    Annotation getChildAnnotation(@NonNull UUID id, @NonNull UUID childId) {
        show(childId)
    }

    @Delete('/{id}')
    @Transactional
    HttpStatus delete(UUID id, @Body @Nullable Annotation annotation) {
        Annotation annotationToDelete = annotationCacheableRepository.findById(id)
        if (!annotationToDelete.parentAnnotationId) {
            Set<Annotation> annotationSet = annotationRepository.findAllChildrenById(id)
            annotationCacheableRepository.deleteAll(annotationSet)
        }
        super.delete(id, annotation)
    }

    @Delete('/{id}/annotations/{childId}')
    @Transactional
    HttpStatus delete(UUID id, @NonNull UUID childId, @Body @Nullable Annotation annotation) {
        Annotation child = annotationCacheableRepository.findById(childId)
        if (!child.parentAnnotationId){
            throw new HttpStatusException(HttpStatus.BAD_REQUEST, "Child Annotation has no parent annotation")
        }
        super.delete(childId, annotation)
    }

}

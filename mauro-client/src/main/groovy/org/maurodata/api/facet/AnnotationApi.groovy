package org.maurodata.api.facet

import org.maurodata.api.MauroApi
import org.maurodata.api.Paths
import org.maurodata.domain.facet.Annotation
import org.maurodata.web.ListResponse

import io.micronaut.core.annotation.NonNull
import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Delete
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Post

@MauroApi
interface AnnotationApi extends FacetApi<Annotation> {

    /**
     * Get annotation list
     * @param domainType
     * @param domainId
     * @return
     */
    @Get(Paths.ANNOTATION_LIST)
    ListResponse<Annotation> list(@NonNull String domainType, @NonNull UUID domainId)

    @Get(Paths.ANNOTATION_ID)
    Annotation show(@NonNull String domainType, @NonNull UUID domainId, @NonNull UUID id)

    @Post(Paths.ANNOTATION_LIST)
    Annotation create(@NonNull String domainType, @NonNull UUID domainId, @Body @NonNull Annotation annotation)

    /**
     * Create Child Annotation
     * @param domainType
     * @param domainId
     * @param annotationId parent annotationId
     * @param childAnnotation child annotation to create
     * @return newly created child
     */
    @Post(Paths.ANNOTATION_CHILD_LIST)
    Annotation create(@NonNull String domainType, @NonNull UUID domainId, @NonNull UUID annotationId, @Body @NonNull Annotation childAnnotation)

    /**
     * Get child annotation
     * @param id parentId
     * @param childId note: if childId = parent, the nested parent is returned
     * @return 'Child' annotation
     */
    @Get(Paths.ANNOTATION_CHILD_ID)
    Annotation getChildAnnotation(@NonNull String domainType, @NonNull UUID domainId, @NonNull UUID annotationId,
                                  @NonNull UUID id)

    @Delete(Paths.ANNOTATION_ID)
    HttpResponse delete(@NonNull String domainType, @NonNull UUID domainId, @NonNull UUID id)

    @Delete(Paths.ANNOTATION_CHILD_ID)
    HttpResponse delete(@NonNull String domainType, @NonNull UUID domainId, @NonNull UUID annotationId,
                      @NonNull UUID id)
}

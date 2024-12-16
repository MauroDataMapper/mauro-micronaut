package uk.ac.ox.softeng.mauro.api.facet

import uk.ac.ox.softeng.mauro.domain.facet.Annotation
import uk.ac.ox.softeng.mauro.web.ListResponse

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.NonNull
import io.micronaut.core.annotation.Nullable
import io.micronaut.http.HttpStatus
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Delete
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Header
import io.micronaut.http.annotation.Post
import io.micronaut.http.client.annotation.Client

@CompileStatic
@Client('${micronaut.http.services.mauro.url}')
@Header(name='apiKey', value = '${micronaut.http.services.mauro.apikey}')
interface AnnotationApi extends FacetApi<Annotation> {

    /**
     * Get annotation list
     * @param domainType
     * @param domainId
     * @return
     */
    @Get('/{domainType}/{domainId}/annotations')
    ListResponse<Annotation> list(@NonNull String domainType, @NonNull UUID domainId)

    @Get('/{domainType}/{domainId}/annotations/{id}')
    Annotation show(@NonNull String domainType, @NonNull UUID domainId, @NonNull UUID id)

    @Post('/{domainType}/{domainId}/annotations')
    Annotation create(@NonNull String domainType, @NonNull UUID domainId, @Body @NonNull Annotation annotation)

    /**
     * Create Child Annotation
     * @param domainType
     * @param domainId
     * @param annotationId parent annotationId
     * @param childAnnotation child annotation to create
     * @return newly created child
     */
    @Post('/{domainType}/{domainId}/annotations/{annotationId}/annotations')
    Annotation create(@NonNull String domainType, @NonNull UUID domainId, @NonNull UUID annotationId, @Body @NonNull Annotation childAnnotation)

    /**
     * Get child annotation
     * @param id parentId
     * @param childId note: if childId = parent, the nested parent is returned
     * @return 'Child' annotation
     */
    @Get('/{domainType}/{domainId}/annotations/{id}/annotations/{childId}')
    Annotation getChildAnnotation(@NonNull String domainType, @NonNull UUID domainId, @NonNull UUID id,
                                  @NonNull UUID childId)

    @Delete('/{domainType}/{domainId}/annotations/{id}')
    HttpStatus delete(@NonNull String domainType, @NonNull UUID domainId, @NonNull UUID id,
                      @Body @Nullable Annotation annotation)

    @Delete('/{domainType}/{domainId}/annotations/{id}/annotations/{childId}')
    HttpStatus delete(@NonNull String domainType, @NonNull UUID domainId, @NonNull UUID id,
                      @NonNull UUID childId, @Body @Nullable Annotation annotation)
}

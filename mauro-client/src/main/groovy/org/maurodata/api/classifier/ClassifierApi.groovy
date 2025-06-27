package org.maurodata.api.classifier

import org.maurodata.api.MauroApi
import org.maurodata.api.Paths
import org.maurodata.api.model.AdministeredItemApi
import org.maurodata.domain.classifier.ClassificationScheme
import org.maurodata.domain.classifier.Classifier
import org.maurodata.web.ListResponse
import org.maurodata.web.PaginationParams

import io.micronaut.core.annotation.NonNull
import io.micronaut.core.annotation.Nullable
import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Delete
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Post
import io.micronaut.http.annotation.Put

@MauroApi
interface ClassifierApi extends AdministeredItemApi<Classifier, ClassificationScheme> {

    @Get(Paths.CLASSIFIERS_ROUTE_ID)
    Classifier show(@NonNull UUID classificationSchemeId, @NonNull UUID id)

    @Post(Paths.CLASSIFIERS_ROUTE)
    Classifier create(@NonNull UUID classificationSchemeId, @Body @NonNull Classifier classifier)

    @Put(Paths.CLASSIFIERS_ROUTE_ID)
    Classifier update(@NonNull UUID classificationSchemeId, @NonNull UUID id, @Body @NonNull Classifier classifier)

    @Delete(Paths.CLASSIFIERS_ROUTE_ID)
    HttpResponse delete(@NonNull UUID classificationSchemeId, @NonNull UUID id, @Body @Nullable Classifier classifier)

    @Get(Paths.CLASSIFIERS_ROUTE)
    ListResponse<Classifier> list(UUID classificationSchemeId)

    @Get(Paths.ALL_CLASSIFIERS_ROUTE)
    ListResponse<Classifier> listAllClassifiers()

    @Get(Paths.CHILD_CLASSIFIERS_ID_ROUTE)
    Classifier showChildClassifier(@NonNull UUID classificationSchemeId,@NonNull UUID parentClassifierId, @NonNull UUID childClassifierId)

    /**
     * Create child classifier
     * @param classificationSchemeId
     * @param id parent ClassifierId
     * @param classifier child
     * @return
     */
    @Post(Paths.CHILD_CLASSIFIERS_ROUTE)
    Classifier create(@NonNull UUID classificationSchemeId, @NonNull UUID parentClassifierId, @Body @NonNull Classifier classifier)

    @Put(Paths.CHILD_CLASSIFIERS_ID_ROUTE)
    Classifier update(@NonNull UUID classificationSchemeId, @NonNull UUID parentClassifierId, @NonNull UUID childClassifierId, @Body @NonNull Classifier classifier)

    @Delete(Paths.CHILD_CLASSIFIERS_ID_ROUTE)
    HttpResponse delete(@NonNull UUID classificationSchemeId, @NonNull UUID parentClassifierId, @NonNull UUID childClassifierId, @Body @Nullable Classifier classifier)

    @Get(Paths.CHILD_CLASSIFIERS_ROUTE)
    ListResponse<Classifier> list(@NonNull UUID classificationSchemeId, @NonNull UUID parentClassifierId)

    @Get(Paths.CHILD_CLASSIFIERS_ROUTE_PAGED)
    ListResponse<Classifier> list(@NonNull UUID classificationSchemeId, @NonNull UUID parentClassifierId, @Nullable PaginationParams params)

    /**
     * Associate Classifier to administeredItem
     */
    @Put(Paths.ADMINISTERED_ITEM_CLASSIFIER_ID_ROUTE)
    Classifier createAdministeredItemClassifier(@NonNull String administeredItemDomainType, @NonNull UUID administeredItemId, @NonNull UUID id)

    /**
     * Get Classifier for AdministeredItem
     */
    @Get(Paths.ADMINISTERED_ITEM_CLASSIFIER_ID_ROUTE)
    Classifier getAdministeredItemClassifier(@NonNull String administeredItemDomainType, @NonNull UUID administeredItemId, @NonNull UUID id)

    /**
     * Get AdministeredItem classifiers
     */
    @Get(Paths.ADMINISTERED_ITEM_CLASSIFIER_ROUTE)
    ListResponse<Classifier> findAllAdministeredItemClassifiers(@NonNull String administeredItemDomainType, @NonNull UUID administeredItemId)

    @Get(Paths.ADMINISTERED_ITEM_CLASSIFIER_ROUTE_PAGED)
    ListResponse<Classifier> findAllAdministeredItemClassifiers(@NonNull String administeredItemDomainType, @NonNull UUID administeredItemId,
                                                                @Nullable PaginationParams params)

    @Delete(Paths.ADMINISTERED_ITEM_CLASSIFIER_ID_ROUTE)
    HttpResponse delete(@NonNull String administeredItemDomainType, @NonNull UUID administeredItemId, @NonNull UUID id)
}

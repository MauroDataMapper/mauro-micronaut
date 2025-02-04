package uk.ac.ox.softeng.mauro.domain.facet.federation.converter

import uk.ac.ox.softeng.mauro.domain.authority.Authority
import uk.ac.ox.softeng.mauro.domain.facet.federation.PublishedModel
import uk.ac.ox.softeng.mauro.domain.facet.federation.SubscribedCatalogueType

import java.time.Instant

trait SubscribedCatalogueConverter {

    abstract boolean handles(SubscribedCatalogueType type)

    abstract Tuple2<Authority, List<PublishedModel>> toPublishedModels(Map<String, Object> stringObjectMap)

    abstract Tuple2<Instant, List<PublishedModel>> publishedModelsNewerVersions(Map<String, Object> stringObjectMap)

}

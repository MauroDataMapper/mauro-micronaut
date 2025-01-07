package uk.ac.ox.softeng.mauro.domain.federation.converter

import uk.ac.ox.softeng.mauro.domain.authority.Authority
import uk.ac.ox.softeng.mauro.domain.federation.PublishedModel
import uk.ac.ox.softeng.mauro.domain.federation.SubscribedCatalogueType

trait SubscribedCatalogueConverter {

    abstract boolean handles(SubscribedCatalogueType type)

    abstract Tuple2<Authority, List<PublishedModel>> toPublishedModels(Map<String, Object> stringObjectMap)

}

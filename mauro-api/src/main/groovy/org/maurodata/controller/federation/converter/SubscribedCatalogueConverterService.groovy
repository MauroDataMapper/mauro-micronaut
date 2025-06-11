package org.maurodata.controller.federation.converter

import org.maurodata.domain.facet.federation.SubscribedCatalogueType

import jakarta.inject.Inject
import jakarta.inject.Singleton

@Singleton
class SubscribedCatalogueConverterService {

    @Inject
    Set<SubscribedCatalogueConverter> subscribedCatalogueConverters


    SubscribedCatalogueConverter getSubscribedCatalogueConverter(SubscribedCatalogueType type) {
        subscribedCatalogueConverters.find {it.handles(type)}
    }

    int getNumberOfConverters(){
        subscribedCatalogueConverters.size()
    }
    @Override
     String toString() {
        return "SubscribedCatalogueConverterService{" +
               "subscribedCatalogueConverters=" + subscribedCatalogueConverters +
               '}';
    }
}


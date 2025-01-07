package uk.ac.ox.softeng.mauro.domain.federation.converter

import uk.ac.ox.softeng.mauro.domain.federation.SubscribedCatalogueType

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


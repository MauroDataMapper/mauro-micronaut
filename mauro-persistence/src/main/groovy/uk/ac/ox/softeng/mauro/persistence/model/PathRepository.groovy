package uk.ac.ox.softeng.mauro.persistence.model

import uk.ac.ox.softeng.mauro.domain.model.AdministeredItem

import io.micronaut.core.annotation.NonNull
import jakarta.inject.Inject
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

class PathRepository {

    @Inject
    List<AdministeredItemRepository> administeredItemRepositories

    Mono<List<AdministeredItem>> readParentItems(AdministeredItem item) {
        getRepository(item).readById(item.id).expand {AdministeredItem it ->
            if (it.parent) getRepository(it.parent).readById(it.parent.id)
            else Flux.empty()
        }.collectList().map {List<AdministeredItem> parents ->
            parents.eachWithIndex {AdministeredItem item, Integer i ->
                item.parent = parents[i+1]
            }
        }
    }

    @NonNull
    AdministeredItemRepository getRepository(AdministeredItem item) {
        administeredItemRepositories.find {it.handles(item.class)}
    }
}

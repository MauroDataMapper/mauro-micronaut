package uk.ac.ox.softeng.mauro.persistence.model

import uk.ac.ox.softeng.mauro.domain.model.AdministeredItem
import uk.ac.ox.softeng.mauro.persistence.cache.CacheableAdministeredItemRepository
import uk.ac.ox.softeng.mauro.persistence.cache.CacheableItemRepository

import groovy.transform.CompileStatic
import io.micronaut.context.annotation.Bean
import io.micronaut.core.annotation.NonNull
import jakarta.inject.Inject
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Bean
@CompileStatic
class PathRepository {

    @Inject
    List<AdministeredItemRepository> administeredItemRepositories

    @Inject
    List<CacheableAdministeredItemRepository> cacheableRepositories

    Mono<List<AdministeredItem>> readParentItems(AdministeredItem item) {
        Mono.just(item).expand {AdministeredItem it ->
            if (it.parent) getRepository(it.parent).readById(it.parent.id)
            else Flux.empty()
        }.collectList().map {List<AdministeredItem> parents ->
            parents.eachWithIndex {AdministeredItem it, Integer i ->
                if (parents[i+1]) it.parent = parents[i+1]
            }
        }
    }

//    @NonNull
//    AdministeredItemRepository getRepository(AdministeredItem item) {
//        administeredItemRepositories.find {it.handles(item.class)}
//    }

    @NonNull
    AdministeredItemRepository getRepository(AdministeredItem item) {
        cacheableRepositories.find {it.handles(item.class)} ?: administeredItemRepositories.find {it.handles(item.class)}
    }
}

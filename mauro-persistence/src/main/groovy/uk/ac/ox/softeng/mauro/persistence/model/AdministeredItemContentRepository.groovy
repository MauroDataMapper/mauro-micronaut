package uk.ac.ox.softeng.mauro.persistence.model

import uk.ac.ox.softeng.mauro.domain.model.AdministeredItem

import io.micronaut.context.annotation.Bean
import io.micronaut.core.annotation.NonNull
import reactor.core.publisher.Mono

@Bean
class AdministeredItemContentRepository<I extends AdministeredItem> {

    Mono<Long> deleteWithContent(@NonNull I item) {
        // todo
    }
}

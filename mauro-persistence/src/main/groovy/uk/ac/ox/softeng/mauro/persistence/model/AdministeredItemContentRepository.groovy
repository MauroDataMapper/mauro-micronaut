package uk.ac.ox.softeng.mauro.persistence.model

import uk.ac.ox.softeng.mauro.domain.model.AdministeredItem

import groovy.transform.CompileStatic
import io.micronaut.context.annotation.Bean
import io.micronaut.core.annotation.NonNull
import reactor.core.publisher.Mono

@CompileStatic
@Bean
class AdministeredItemContentRepository<I extends AdministeredItem> {

    Mono<Long> deleteWithContent(@NonNull I item) {
        // todo
    }
}

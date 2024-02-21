package uk.ac.ox.softeng.mauro.persistence.model

import groovy.transform.CompileStatic
import io.micronaut.context.annotation.Bean
import io.micronaut.core.annotation.NonNull
import uk.ac.ox.softeng.mauro.domain.model.AdministeredItem

@CompileStatic
@Bean
class AdministeredItemContentRepository<I extends AdministeredItem> {

    Long deleteWithContent(@NonNull I item) {
        // todo
    }
}

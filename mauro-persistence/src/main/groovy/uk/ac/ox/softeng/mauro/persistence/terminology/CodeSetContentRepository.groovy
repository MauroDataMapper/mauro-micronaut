package uk.ac.ox.softeng.mauro.persistence.terminology

import groovy.transform.CompileStatic
import io.micronaut.context.annotation.Bean
import jakarta.inject.Inject
import uk.ac.ox.softeng.mauro.domain.terminology.CodeSet
import uk.ac.ox.softeng.mauro.persistence.model.ModelContentRepository

@CompileStatic
@Bean
class CodeSetContentRepository extends ModelContentRepository<CodeSet> {

    @Inject
    CodeSetRepository codeSetRepository

    CodeSet findWithAssociations(UUID id) {
        codeSetRepository.findById(id)
    }

}

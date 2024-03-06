package uk.ac.ox.softeng.mauro.persistence.terminology

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.micronaut.context.annotation.Bean
import io.micronaut.core.annotation.NonNull
import jakarta.inject.Inject
import uk.ac.ox.softeng.mauro.domain.model.AdministeredItem
import uk.ac.ox.softeng.mauro.domain.terminology.CodeSet
import uk.ac.ox.softeng.mauro.persistence.model.ModelContentRepository

@CompileStatic
@Bean
@Slf4j
class CodeSetContentRepository extends ModelContentRepository<CodeSet> {

    @Inject
    CodeSetRepository codeSetRepository


    @Override
    CodeSet readWithContentById(UUID id) {
        CodeSet codeSet = codeSetRepository.readById(id)
        codeSet.terms = codeSetRepository.getTerms(id)
        log.debug("CSCR:readWithContentById $id: found number of associated terms: {}", codeSet.terms.size())
        codeSet
    }

    @Override
    Long deleteWithContent(@NonNull AdministeredItem administeredItem) {
        codeSetRepository.removeTermAssociations((administeredItem as CodeSet).id)
        Long result = codeSetRepository.delete(administeredItem as CodeSet)
        log.debug("CSCR: removed $result codeSet $administeredItem.id")
        result
    }
}
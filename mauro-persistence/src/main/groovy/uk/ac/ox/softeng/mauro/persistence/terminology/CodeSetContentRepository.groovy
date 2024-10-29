package uk.ac.ox.softeng.mauro.persistence.terminology

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.micronaut.core.annotation.NonNull
import jakarta.inject.Inject
import jakarta.inject.Singleton
import uk.ac.ox.softeng.mauro.domain.datamodel.DataClass
import uk.ac.ox.softeng.mauro.domain.datamodel.DataModel
import uk.ac.ox.softeng.mauro.domain.model.AdministeredItem
import uk.ac.ox.softeng.mauro.domain.terminology.CodeSet
import uk.ac.ox.softeng.mauro.persistence.model.ModelContentRepository

@CompileStatic
@Singleton
@Slf4j
class CodeSetContentRepository extends ModelContentRepository<CodeSet> {

    @Inject
    CodeSetRepository codeSetRepository


    @Override
    CodeSet readWithContentById(UUID id) {
        CodeSet codeSet = codeSetRepository.readById(id)
        if (!codeSet){
            return null
        }
        codeSet.terms = codeSetRepository.getTerms(id)
        log.debug("CSCR:readWithContentById $id: found number of associated terms: {}", codeSet.terms.size())
        codeSet
    }

    @Override
    CodeSet findWithContentById(UUID id) {
        CodeSet codeSet = codeSetRepository.findById(id)
        codeSet.terms = codeSetRepository.getTerms(id)
        codeSet
    }

    // TODO overridden code not invalidating cache
    @Override
    Long deleteWithContent(@NonNull AdministeredItem administeredItem) {
        codeSetRepository.removeTermAssociations((administeredItem as CodeSet).id)
        Long result = super.deleteWithContent(administeredItem)
        result
    }

    @Override
    CodeSet saveWithContent(@NonNull CodeSet codeSet) {
        (CodeSet) super.saveWithContent(codeSet)
    }
}
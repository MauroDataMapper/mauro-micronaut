package org.maurodata.persistence.terminology

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.micronaut.core.annotation.NonNull
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.maurodata.domain.datamodel.DataClass
import org.maurodata.domain.datamodel.DataModel
import org.maurodata.domain.model.AdministeredItem
import org.maurodata.domain.terminology.CodeSet
import org.maurodata.persistence.model.ModelContentRepository

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

    @Override
    Boolean handles(String domainType) {
        return codeSetRepository.handles(domainType)
    }

    @Override
    Boolean handles(Class clazz) {
        return codeSetRepository.handles(clazz)
    }
}
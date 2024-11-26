package uk.ac.ox.softeng.mauro.controller.facet

import uk.ac.ox.softeng.mauro.domain.facet.Rule
import uk.ac.ox.softeng.mauro.domain.facet.RuleRepresentation
import uk.ac.ox.softeng.mauro.domain.model.AdministeredItem
import uk.ac.ox.softeng.mauro.domain.security.Role
import uk.ac.ox.softeng.mauro.persistence.cache.FacetCacheableRepository
import uk.ac.ox.softeng.mauro.persistence.cache.ItemCacheableRepository
import uk.ac.ox.softeng.mauro.persistence.facet.RuleRepresentationRepository
import uk.ac.ox.softeng.mauro.web.ListResponse

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.NonNull
import io.micronaut.core.annotation.Nullable
import io.micronaut.http.HttpStatus
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Delete
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Post
import io.micronaut.http.annotation.Put
import io.micronaut.security.annotation.Secured
import io.micronaut.security.rules.SecurityRule
import io.micronaut.transaction.annotation.Transactional
import jakarta.inject.Inject

@CompileStatic
@Controller('/{domainType}/{domainId}/rules')
@Secured(SecurityRule.IS_ANONYMOUS)
class RuleController extends FacetController<Rule> {
    FacetCacheableRepository.RuleCacheableRepository ruleRepository

    @Inject
    RuleRepresentationRepository ruleRepresentationRepositoryUncached

    @Inject
    ItemCacheableRepository.RuleRepresentationCacheableRepository ruleRepresentationCacheableRepository

    /**
     * Properties disallowed in a simple update request.
     */
    List<String> getDisallowedProperties() {
        super.getDisallowedProperties() + ['multiFacetAwareItemDomainType', 'multiFacetAwareItemId']
    }

    RuleController(FacetCacheableRepository.RuleCacheableRepository ruleRepository) {
        super(ruleRepository)
        this.ruleRepository = ruleRepository
    }

    @Get
    ListResponse<Rule> list(String domainType, UUID domainId) {
        AdministeredItem administeredItem = findAdministeredItem(domainType, domainId)
        accessControlService.checkRole(Role.READER, administeredItem)
        ListResponse.from(!administeredItem.rules ? [] : administeredItem.rules)
    }

    @Get('/{id}')
    Rule show(UUID id) {
        Rule rule = super.show(id) as Rule
        if (rule) {
            AdministeredItem administeredItem = findAdministeredItem(rule.multiFacetAwareItemDomainType,
                    rule.multiFacetAwareItemId)
            accessControlService.checkRole(Role.READER, administeredItem)
            List<Rule> ruleList = administeredItem.rules
            Rule ruleWithReports = ruleList.find { it -> it.id == id }
            ruleWithReports
        }
    }

    @Post
    Rule create(@NonNull String domainType, @NonNull UUID domainId, @Body @NonNull Rule rule) {
        super.create(domainType, domainId, rule)
    }

    @Put('/{id}')
    Rule update(@NonNull UUID id, @Body @NonNull Rule rule) {
        super.update(id, rule)
    }

    @Delete('/{id}')
    @Transactional
    @Override
    HttpStatus delete(@NonNull UUID id, @Body @Nullable Rule rule) {
        accessControlService.checkRole(Role.READER, readAdministeredItemForFacet(ruleRepository.readById(id)))
        deleteAnyAssociatedRepresentations(id)
        super.delete(id, rule)
    }

    private void deleteAnyAssociatedRepresentations(UUID ruleId) {
        List<RuleRepresentation> savedRuleRepresentations = ruleRepresentationRepositoryUncached.findAllByRuleId(ruleId)
        if (savedRuleRepresentations) {
            ruleRepresentationCacheableRepository.deleteAll(savedRuleRepresentations)
        }
    }
}
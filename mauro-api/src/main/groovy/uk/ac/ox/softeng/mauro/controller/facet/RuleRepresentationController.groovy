package uk.ac.ox.softeng.mauro.controller.facet

import io.micronaut.http.HttpResponse
import uk.ac.ox.softeng.mauro.api.Paths
import uk.ac.ox.softeng.mauro.api.facet.RuleRepresentationApi
import uk.ac.ox.softeng.mauro.controller.model.ItemController
import uk.ac.ox.softeng.mauro.domain.facet.Facet
import uk.ac.ox.softeng.mauro.domain.facet.Rule
import uk.ac.ox.softeng.mauro.domain.facet.RuleRepresentation
import uk.ac.ox.softeng.mauro.domain.model.AdministeredItem
import uk.ac.ox.softeng.mauro.domain.model.Item
import uk.ac.ox.softeng.mauro.domain.security.Role
import uk.ac.ox.softeng.mauro.persistence.cache.FacetCacheableRepository
import uk.ac.ox.softeng.mauro.persistence.cache.ItemCacheableRepository
import uk.ac.ox.softeng.mauro.persistence.facet.RuleRepresentationRepository
import uk.ac.ox.softeng.mauro.web.ListResponse

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.micronaut.http.HttpStatus
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Delete
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Post
import io.micronaut.http.annotation.Put
import io.micronaut.http.exceptions.HttpStatusException
import io.micronaut.security.annotation.Secured
import io.micronaut.security.rules.SecurityRule
import io.micronaut.transaction.annotation.Transactional
import jakarta.inject.Inject
import io.micronaut.core.annotation.NonNull

@CompileStatic
@Slf4j
@Controller('/{domainType}/{domainId}/rules/{ruleId}/representations')
@Secured(SecurityRule.IS_ANONYMOUS)
class RuleRepresentationController extends ItemController<RuleRepresentation> implements RuleRepresentationApi {

    @Inject
    RuleRepresentationRepository ruleRepresentationRepository

    @Inject
    FacetCacheableRepository.RuleCacheableRepository ruleCacheableRepository

    ItemCacheableRepository.RuleRepresentationCacheableRepository ruleRepresentationCacheableRepository

    /**
     * Properties disallowed in a simple update request.
     */
    List<String> getDisallowedProperties() {
        super.getDisallowedProperties()
    }

    RuleRepresentationController(ItemCacheableRepository.RuleRepresentationCacheableRepository ruleRepresentationCacheableRepository) {
        super(ruleRepresentationCacheableRepository)
        this.ruleRepresentationCacheableRepository = ruleRepresentationCacheableRepository
    }

    @Post
    RuleRepresentation create(@NonNull String domainType, @NonNull UUID domainId, @NonNull UUID ruleId,
                                 @Body RuleRepresentation ruleRepresentation) {
        super.cleanBody(ruleRepresentation)
        Rule rule = validateAndGet(domainType, domainId, ruleId)
        accessControlService.checkRole(Role.EDITOR, readAdministeredItemForFacet(rule))
        ruleRepresentation.ruleId = rule.id
        updateCreationProperties(ruleRepresentation)
        ruleRepresentationCacheableRepository.save(ruleRepresentation)
    }

    @Get(Paths.RULE_REPRESENTATIONS_ID)
    RuleRepresentation show(@NonNull String domainType, @NonNull UUID domainId, @NonNull UUID ruleId,
                               @NonNull UUID id) {
        Rule rule = validateAndGet(domainType, domainId, ruleId)
        accessControlService.checkRole(Role.READER, readAdministeredItemForFacet(rule))
        ruleRepresentationCacheableRepository.findById(id)
    }


    @Get
    ListResponse<Rule> list(@NonNull String domainType, @NonNull UUID domainId, @NonNull UUID ruleId) {
        Rule rule = validateAndGet(domainType, domainId, ruleId)
        accessControlService.checkRole(Role.READER, readAdministeredItemForFacet(rule))
        List<RuleRepresentation> ruleRepresentationList = ruleRepresentationRepository.findAllByRuleId(ruleId)
        ListResponse.from(ruleRepresentationList)
    }

    @Get('/{id}')
    RuleRepresentation get(@NonNull String domainType, @NonNull UUID domainId, @NonNull UUID ruleId,
                              @NonNull UUID id) {
        Rule rule = validateAndGet(domainType, domainId, ruleId)
        accessControlService.checkRole(Role.READER, readAdministeredItemForFacet(rule))
        ruleRepresentationCacheableRepository.findById(id)
    }

    @Put('/{id}')
    RuleRepresentation update(@NonNull String domainType, @NonNull UUID domainId, @NonNull UUID ruleId,
                                 @NonNull UUID id, @Body @NonNull RuleRepresentation ruleRepresentation) {
        super.cleanBody(ruleRepresentation)
        RuleRepresentation existing = ruleRepresentationCacheableRepository.readById(id)
        throwNotFoundException(existing, id)
        Rule rule = validateAndGet(domainType, domainId, ruleId)
        accessControlService.checkRole(Role.EDITOR, readAdministeredItemForFacet(rule))
        RuleRepresentation updated = updateEntity(existing, ruleRepresentation, rule)
        updated
    }

    @Delete('/{id}')
    @Transactional
    HttpResponse delete(@NonNull String domainType, @NonNull UUID domainId, @NonNull UUID ruleId,
                        @NonNull UUID id) {
        Rule rule = validateAndGet(domainType, domainId, ruleId)
        accessControlService.checkRole(Role.EDITOR, readAdministeredItemForFacet(rule))
        RuleRepresentation ruleRepresentation = ruleRepresentationCacheableRepository.readById(id)
        if (!ruleRepresentation) {
            throwNotFoundException(ruleRepresentation, id)
        }
        ruleRepresentationCacheableRepository.delete(ruleRepresentation)
        HttpResponse.status(HttpStatus.NO_CONTENT)
    }

    private RuleRepresentation updateEntity(RuleRepresentation existing, RuleRepresentation cleaned,
                                               Rule rule) {
        boolean hasChanged = updateProperties(existing, cleaned)
        if (hasChanged) {
            ruleRepresentationCacheableRepository.update(existing)
        } else {
            existing
        }
    }

    private Rule validateAndGet(String domainType, UUID domainId, UUID ruleId) {
        Rule rule = ruleCacheableRepository.readById(ruleId)
        if (!rule) {
            throwNotFoundException(null, ruleId)
        }
        if (rule.multiFacetAwareItemId != domainId || !ruleCacheableRepository.handles(domainType)) {
            throwNotFoundException(rule, ruleId)
        }
        rule
    }

    private static void throwNotFoundException(Item item, UUID id) {
        if (!item) {
            throw new HttpStatusException(HttpStatus.NOT_FOUND, "Item $id not found ")
        }
    }

    protected AdministeredItem readAdministeredItemForFacet(Facet facet) {
        readAdministeredItem(facet.multiFacetAwareItemDomainType, facet.multiFacetAwareItemId)
    }
}
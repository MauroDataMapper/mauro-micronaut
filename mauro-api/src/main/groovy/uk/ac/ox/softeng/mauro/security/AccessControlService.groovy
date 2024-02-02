package uk.ac.ox.softeng.mauro.security

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.micronaut.security.authentication.AuthorizationException
import io.micronaut.security.utils.SecurityService
import jakarta.inject.Inject
import jakarta.inject.Singleton
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import uk.ac.ox.softeng.mauro.domain.model.AdministeredItem
import uk.ac.ox.softeng.mauro.domain.model.Model
import uk.ac.ox.softeng.mauro.domain.security.SecurableResource
import uk.ac.ox.softeng.mauro.domain.security.SecurableResourceGroupRole
import uk.ac.ox.softeng.mauro.domain.security.UserGroup
import uk.ac.ox.softeng.mauro.exception.MauroException
import uk.ac.ox.softeng.mauro.persistence.model.PathRepository
import uk.ac.ox.softeng.mauro.persistence.security.SecurableResourceGroupRoleRepository
import uk.ac.ox.softeng.mauro.persistence.security.UserGroupRepository

@CompileStatic
@Singleton
@Slf4j
class AccessControlService {

    @Inject
    SecurityService securityService

    @Inject
    PathRepository pathRepository

    @Inject
    SecurableResourceGroupRoleRepository securableResourceGroupRoleRepository

    @Inject
    UserGroupRepository userGroupRepository

    Mono<Void> canRead(Model model) {
        log.debug 'AccessControlService :: canRead'
        if (model.readableByEveryone) return Mono.empty()
        if (model.readableByAuthenticatedUsers && isUserAuthenticated()) return Mono.empty()

        Mono.zip(userGroups(userId()), pathRepository.readParentItems(model)).map {
            List<UserGroup> userGroups = it.getT1()
            List<Model> models = it.getT2() as List<Model>

            Flux.fromIterable(models).flatMap {
                hasReadRole(userGroups, it)
            }.filter {it}.switchIfEmpty(Mono.error(new AuthorizationException(null))).take(1).then {
                Mono.empty()
            }
        } as Mono<Void>
    }

    private Mono<Boolean> hasReadRole(List<UserGroup> userGroups, Model model) {
        log.debug 'AccessControlService :: hasReadRole'
        securableResourceGroupRoleRepository.readAllBySecurableResourceDomainTypeAndSecurableResourceId(model.domainType, model.id).map { SecurableResourceGroupRole securableResourceGroupRole ->
            securableResourceGroupRole.userGroup.id in userGroups.id
        }.filter {it}.switchIfEmpty { Mono.just(false)}.next()
    }

    Boolean isUserAuthenticated() {
        securityService.authenticated && securityService.authentication.get().attributes.id instanceof UUID
    }

    UUID userId() {
        (UUID) securityService.authentication.get().attributes.id
    }

    Mono<List<UserGroup>> userGroups(UUID userId) {
        userGroupRepository.readAllByCatalogueUserId(userId).collectList()
    }

    Mono<List<SecurableResourceGroupRole>> securableResourceGroupRoles(String domainType, UUID id) {
        securableResourceGroupRoleRepository.readAllBySecurableResourceDomainTypeAndSecurableResourceId(domainType, id).collectList()
    }
}

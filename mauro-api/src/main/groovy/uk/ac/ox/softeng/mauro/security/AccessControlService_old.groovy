//package uk.ac.ox.softeng.mauro.security
//
//import groovy.transform.CompileStatic
//import groovy.util.logging.Slf4j
//import io.micronaut.security.authentication.Authentication
//import io.micronaut.security.authentication.AuthorizationException
//import io.micronaut.security.utils.SecurityService
//import jakarta.inject.Inject
//import jakarta.inject.Singleton
//import reactor.core.publisher.Flux
//import reactor.core.publisher.Mono
//import uk.ac.ox.softeng.mauro.domain.model.Model
//import uk.ac.ox.softeng.mauro.domain.security.Role
//import uk.ac.ox.softeng.mauro.domain.security.SecurableResourceGroupRole
//import uk.ac.ox.softeng.mauro.domain.security.UserGroup
//import uk.ac.ox.softeng.mauro.persistence.model.PathRepository
//import uk.ac.ox.softeng.mauro.persistence.security.SecurableResourceGroupRoleRepository
//import uk.ac.ox.softeng.mauro.persistence.security.UserGroupRepository
//
//@CompileStatic
//@Singleton
//@Slf4j
//class AccessControlService {
//
//    @Inject
//    SecurityService securityService
//
//    @Inject
//    PathRepository pathRepository
//
//    @Inject
//    SecurableResourceGroupRoleRepository securableResourceGroupRoleRepository
//
//    @Inject
//    UserGroupRepository userGroupRepository
//
//    Mono<Void> canRead(Model model) {
//        canDoRole(Role.READER, model)
//    }
//
//    Mono<Void> canReview(Model model) {
//        canDoRole(Role.REVIEWER, model)
//    }
//
//    Mono<Void> canAuthor(Model model) {
//        canDoRole(Role.AUTHOR, model)
//    }
//
//    Mono<Void> canEdit(Model model) {
//        canDoRole(Role.EDITOR, model)
//    }
//
//    Mono<Void> canContainerAdmin(Model model) {
//        canDoRole(Role.CONTAINER_ADMIN, model)
//    }
//
//    Mono<Void> canDoRole(Role role, Model model) {
//        if (model.readableByEveryone) return Mono.empty()
//        if (model.readableByAuthenticatedUsers && isUserAuthenticated()) return Mono.empty()
//
//        Mono.zip(userGroups(userId()), pathRepository.readParentItems(model)).flatMap {
//            List<UserGroup> userGroups = it.getT1()
//            List<Model> models = it.getT2() as List<Model>
//            Flux.fromIterable(models).flatMap {
//                canDoRoleOnResource(role, userGroups, it)
//            }.filter { it }.switchIfEmpty(Mono.error(new AuthorizationException(userAuthentication()))).take(1).then(
//                    Mono.empty()
//            )
//        } as Mono<Void>
//    }
//
//    private Mono<Boolean> canDoRoleOnResource(Role role, List<UserGroup> userGroups, Model model) {
//        securableResourceGroupRoleRepository.readAllBySecurableResourceDomainTypeAndSecurableResourceId(model.domainType, model.id).map { SecurableResourceGroupRole securableResourceGroupRole ->
//            role <= securableResourceGroupRole.role &&
//                    securableResourceGroupRole.userGroup.id in userGroups.id
//        }.filter { it }.switchIfEmpty(Mono.just(false)).next()
//    }
//
//    Boolean isUserAuthenticated() {
//        securityService.authenticated && userAuthentication().attributes.id instanceof UUID
//    }
//
//    UUID userId() {
//        (UUID) userAuthentication().attributes.id
//    }
//
//    Authentication userAuthentication() {
//        securityService.authentication.get()
//    }
//
//    Mono<List<UserGroup>> userGroups(UUID userId) {
//        userGroupRepository.readAllByCatalogueUserId(userId).collectList()
//    }
//
//    Mono<List<SecurableResourceGroupRole>> securableResourceGroupRoles(String domainType, UUID id) {
//        securableResourceGroupRoleRepository.readAllBySecurableResourceDomainTypeAndSecurableResourceId(domainType, id).collectList()
//    }
//}

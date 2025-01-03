package uk.ac.ox.softeng.mauro.controller.security

import uk.ac.ox.softeng.mauro.api.Paths
import uk.ac.ox.softeng.mauro.api.security.UserGroupApi

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.micronaut.core.annotation.NonNull
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Post
import io.micronaut.security.annotation.Secured
import io.micronaut.security.rules.SecurityRule
import io.micronaut.transaction.annotation.Transactional
import uk.ac.ox.softeng.mauro.controller.model.ItemController
import uk.ac.ox.softeng.mauro.domain.security.UserGroup
import uk.ac.ox.softeng.mauro.persistence.cache.ItemCacheableRepository

@CompileStatic
@Slf4j
@Controller
@Secured(SecurityRule.IS_ANONYMOUS)
class UserGroupController extends ItemController<UserGroup> implements UserGroupApi {

    ItemCacheableRepository.UserGroupCacheableRepository userGroupRepository

    UserGroupController(ItemCacheableRepository.UserGroupCacheableRepository userGroupRepository) {
        super(userGroupRepository)
        this.userGroupRepository = userGroupRepository
    }

    @Transactional
    @Post(Paths.USER_GROUP_LIST)
    UserGroup create(@Body @NonNull UserGroup userGroup) {
        accessControlService.checkAdministrator()
        userGroupRepository.save(userGroup)
    }
}

package org.maurodata.controller.path

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.security.annotation.Secured
import io.micronaut.security.rules.SecurityRule
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.maurodata.api.Paths
import org.maurodata.api.path.PathApi
import org.maurodata.audit.Audit
import org.maurodata.controller.model.AdministeredItemController
import org.maurodata.domain.model.AdministeredItem
import org.maurodata.domain.security.Role
import org.maurodata.persistence.cache.AdministeredItemCacheableRepository

@Slf4j
@Singleton
@Controller
@Secured(SecurityRule.IS_ANONYMOUS)
@CompileStatic
class PathController<I extends AdministeredItem, P extends AdministeredItem> extends AdministeredItemController<I, P> implements PathApi<I> {

    @Inject
    List<AdministeredItemCacheableRepository> administeredItemRepositories

    @Audit
    @Override
    @Get(Paths.RESOURCE_BY_PATH)
    AdministeredItem getResourceByPath(String domainType, String path) {
        AdministeredItem item = findAdministeredItem(domainType, path) as AdministeredItem
        accessControlService.checkRole(Role.READER, item)
        updateDerivedProperties(item as I)
        item
    }

    @Audit
    @Override
    @Get(Paths.RESOURCE_BY_PATH_FROM_RESOURCE)
    AdministeredItem getResourceByPathFromResource(String domainType, UUID domainId, String path) {
        AdministeredItemCacheableRepository administeredItemRepository = getAdministeredItemRepository(domainType)
        AdministeredItem fromModel = administeredItemRepository.findById(domainId) as AdministeredItem
        accessControlService.checkRole(Role.READER, fromModel)
        List<AdministeredItem> items = administeredItemRepositories.collect {
            it.findByPathIdentifier(path)
        }.findAll {item ->
            item != null && item?.owner?.id == fromModel.id
        }
        if (!items.isEmpty()) {
            if (1 != items.size()) {
                log.warn("${Paths.RESOURCE_BY_PATH_FROM_RESOURCE}: expected 1 match: found ${items.size()}. Picking 1st ")
            }
            AdministeredItem result = items.first()
            accessControlService.checkRole(Role.READER, result)
            println(" $result.path, $result.pathIdentifier, $result.pathModelIdentifier")
            updateDerivedProperties(result as I)
            println(" $result.path, $result.pathIdentifier, $result.pathModelIdentifier")
        }
    }
}

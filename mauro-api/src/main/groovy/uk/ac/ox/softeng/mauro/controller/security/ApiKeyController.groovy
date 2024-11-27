package uk.ac.ox.softeng.mauro.controller.security

import uk.ac.ox.softeng.mauro.controller.model.ItemController
import uk.ac.ox.softeng.mauro.domain.security.ApiKey
import uk.ac.ox.softeng.mauro.domain.security.Role
import uk.ac.ox.softeng.mauro.persistence.cache.ItemCacheableRepository
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
import io.micronaut.security.annotation.Secured
import io.micronaut.security.rules.SecurityRule

@CompileStatic
@Slf4j
@Controller('/catalogueUsers/{userId}/apiKeys')
@Secured(SecurityRule.IS_AUTHENTICATED)
class ApiKeyController extends ItemController<ApiKey> {

    ItemCacheableRepository.ApiKeyCacheableRepository apiKeyCacheableRepository

    ApiKeyController(ItemCacheableRepository.ApiKeyCacheableRepository apiKeyCacheableRepository) {
        super(apiKeyCacheableRepository)
        this.apiKeyCacheableRepository = apiKeyCacheableRepository
    }


    @Get()
    ListResponse<ApiKey> index(UUID userId) {
        accessControlService.checkRoleForUser(Role.READER, userId)
        List<ApiKey> apiKeys = apiKeyCacheableRepository.readByCatalogueUserId(userId)
        ListResponse.from(apiKeys?: [])
    }

    @Post
    ApiKey create(UUID userId, @Body ApiKey apiKey) {
        accessControlService.checkRoleForUser(Role.EDITOR, userId)
        cleanBody(apiKey)
        updateCreationProperties(apiKey)
        apiKey.catalogueUserId = userId
        if(apiKey.disabled == null) {
            apiKey.disabled = false
        }
        apiKeyCacheableRepository.save(apiKey)
    }

    @Get('/{apiKeyId}')
    ApiKey show(UUID userId, UUID apiKeyId) {
        accessControlService.checkRoleForUser(Role.READER, userId)
        List<ApiKey> apiKeys = apiKeyCacheableRepository.readByCatalogueUserId(userId)
        apiKeys.find { it.id == apiKeyId }
    }

    @Delete('/{apiKeyId}')
    HttpStatus delete(UUID userId, UUID apiKeyId) {
        accessControlService.checkRoleForUser(Role.EDITOR, userId)
        List<ApiKey> apiKeys = apiKeyCacheableRepository.readByCatalogueUserId(userId)
        ApiKey apiKey = apiKeys.find { it.id == apiKeyId }
        apiKeyCacheableRepository.delete(apiKey)
        return HttpStatus.NO_CONTENT
    }

    @Put('/{apiKeyId}/enable')
    ApiKey enable(UUID userId, UUID apiKeyId) {
        accessControlService.checkRoleForUser(Role.EDITOR, userId)
        List<ApiKey> apiKeys = apiKeyCacheableRepository.readByCatalogueUserId(userId)
        ApiKey apiKey = apiKeys.find { it.id == apiKeyId }
        apiKey.disabled = false
        apiKeyCacheableRepository.update(apiKey)
    }

    @Put('/{apiKeyId}/disable')
    ApiKey disable(UUID userId, UUID apiKeyId) {
        accessControlService.checkRoleForUser(Role.EDITOR, userId)
        List<ApiKey> apiKeys = apiKeyCacheableRepository.readByCatalogueUserId(userId)
        ApiKey apiKey = apiKeys.find { it.id == apiKeyId }
        apiKey.disabled = true
        apiKeyCacheableRepository.update(apiKey)
    }

    @Put('/{apiKeyId}/refresh/{expireInDays}')
    ApiKey disable(UUID userId, UUID apiKeyId, long expireInDays) {
        accessControlService.checkRoleForUser(Role.EDITOR, userId)
        List<ApiKey> apiKeys = apiKeyCacheableRepository.readByCatalogueUserId(userId)
        ApiKey apiKey = apiKeys.find { it.id == apiKeyId }
        apiKey.setExpiresInDays(expireInDays)
        apiKeyCacheableRepository.update(apiKey)
    }


}

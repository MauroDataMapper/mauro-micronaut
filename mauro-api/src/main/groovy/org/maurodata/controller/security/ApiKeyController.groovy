package org.maurodata.controller.security

import org.maurodata.api.Paths
import org.maurodata.api.security.ApiKeyApi
import org.maurodata.audit.Audit
import org.maurodata.controller.model.ItemController
import org.maurodata.domain.security.ApiKey
import org.maurodata.domain.security.Role
import org.maurodata.persistence.cache.ItemCacheableRepository
import org.maurodata.web.ListResponse

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
@Controller
@Secured(SecurityRule.IS_AUTHENTICATED)
class ApiKeyController extends ItemController<ApiKey> implements ApiKeyApi {

    ItemCacheableRepository.ApiKeyCacheableRepository apiKeyCacheableRepository

    ApiKeyController(ItemCacheableRepository.ApiKeyCacheableRepository apiKeyCacheableRepository) {
        super(apiKeyCacheableRepository)
        this.apiKeyCacheableRepository = apiKeyCacheableRepository
    }


    @Audit
    @Get(Paths.API_KEY_LIST)
    ListResponse<ApiKey> index(UUID userId) {
        accessControlService.checkAdminOrUser(userId)
        List<ApiKey> apiKeys = apiKeyCacheableRepository.readByCatalogueUserId(userId)
        ListResponse.from(apiKeys?: [])
    }

    @Audit(level = Audit.AuditLevel.FILE_ONLY)
    @Post(Paths.API_KEY_LIST)
    ApiKey create(UUID userId, @Body ApiKey apiKey) {
        accessControlService.checkAdminOrUser(userId)
        cleanBody(apiKey)
        updateCreationProperties(apiKey)
        apiKey.catalogueUserId = userId
        if(apiKey.disabled == null) {
            apiKey.disabled = false
        }
        apiKey.updateExpiryDate()
        setStableId(apiKey)
        apiKeyCacheableRepository.save(apiKey)
    }

    @Audit
    @Get(Paths.API_KEY_ID)
    ApiKey show(UUID userId, UUID apiKeyId) {
        accessControlService.checkAdminOrUser(userId)
        List<ApiKey> apiKeys = apiKeyCacheableRepository.readByCatalogueUserId(userId)
        apiKeys.find { it.id == apiKeyId }
    }

    @Audit(level = Audit.AuditLevel.FILE_ONLY)
    @Delete(Paths.API_KEY_ID)
    HttpStatus delete(UUID userId, UUID apiKeyId) {
        accessControlService.checkAdminOrUser(userId)
        List<ApiKey> apiKeys = apiKeyCacheableRepository.readByCatalogueUserId(userId)
        ApiKey apiKey = apiKeys.find { it.id == apiKeyId }
        apiKeyCacheableRepository.delete(apiKey)
        return HttpStatus.NO_CONTENT
    }

    @Audit(level = Audit.AuditLevel.FILE_ONLY)
    @Put(Paths.API_KEY_ENABLE)
    ApiKey enable(UUID userId, UUID apiKeyId) {
        accessControlService.checkAdminOrUser(userId)
        List<ApiKey> apiKeys = apiKeyCacheableRepository.readByCatalogueUserId(userId)
        ApiKey apiKey = apiKeys.find { it.id == apiKeyId }
        apiKey.disabled = false
        apiKeyCacheableRepository.update(apiKey)
    }

    @Audit(level = Audit.AuditLevel.FILE_ONLY)
    @Put(Paths.API_KEY_DISABLE)
    ApiKey disable(UUID userId, UUID apiKeyId) {
        accessControlService.checkAdminOrUser(userId)
        List<ApiKey> apiKeys = apiKeyCacheableRepository.readByCatalogueUserId(userId)
        ApiKey apiKey = apiKeys.find { it.id == apiKeyId }
        apiKey.disabled = true
        apiKeyCacheableRepository.update(apiKey)
    }

    @Audit(level = Audit.AuditLevel.FILE_ONLY)
    @Put(Paths.API_KEY_REFRESH)
    ApiKey refresh(UUID userId, UUID apiKeyId, long expireInDays) {
        accessControlService.checkAdminOrUser(userId)
        List<ApiKey> apiKeys = apiKeyCacheableRepository.readByCatalogueUserId(userId)
        ApiKey apiKey = apiKeys.find { it.id == apiKeyId }
        apiKey.expiresInDays = expireInDays
        apiKey.updateExpiryDate()
        apiKeyCacheableRepository.update(apiKey)
    }


}

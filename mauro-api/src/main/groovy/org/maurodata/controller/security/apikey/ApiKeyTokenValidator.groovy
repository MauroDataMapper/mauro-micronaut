package org.maurodata.controller.security.apikey

import org.maurodata.domain.security.ApiKey
import org.maurodata.domain.security.CatalogueUser
import org.maurodata.persistence.cache.ItemCacheableRepository

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.micronaut.core.async.publisher.Publishers
import io.micronaut.http.HttpRequest
import io.micronaut.security.authentication.Authentication
import io.micronaut.security.token.validator.TokenValidator
import jakarta.inject.Singleton
import org.reactivestreams.Publisher

@CompileStatic
@Slf4j
@Singleton
class ApiKeyTokenValidator implements TokenValidator<HttpRequest<?>>  {

    private final ItemCacheableRepository.ApiKeyCacheableRepository apiKeyCacheableRepository

    private final ItemCacheableRepository.CatalogueUserCacheableRepository catalogueUserCacheableRepository

    ApiKeyTokenValidator(ItemCacheableRepository.ApiKeyCacheableRepository apiKeyCacheableRepository,
                         ItemCacheableRepository.CatalogueUserCacheableRepository catalogueUserCacheableRepository) {
        this.apiKeyCacheableRepository = apiKeyCacheableRepository
        this.catalogueUserCacheableRepository = catalogueUserCacheableRepository
    }

    @Override
    Publisher<Authentication> validateToken(String token, HttpRequest<?> request) {
        ApiKey apiKey = apiKeyCacheableRepository.readById(UUID.fromString(token))
        if(!apiKey) {
            return Publishers.empty()
        }
        if(apiKey.disabled || apiKey.expired) {
            log.warn("API Key is either disabled or expired")
            return Publishers.empty()
        }
        CatalogueUser catalogueUser = catalogueUserCacheableRepository.findById(apiKey.catalogueUserId)
        if(!catalogueUser) {
            return Publishers.empty()
        }
        Publishers.just(Authentication.build(catalogueUser.emailAddress, [id: catalogueUser.id] as Map<String, Object>))

    }
}
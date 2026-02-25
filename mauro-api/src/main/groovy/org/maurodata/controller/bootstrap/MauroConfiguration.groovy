package org.maurodata.controller.bootstrap


import groovy.transform.CompileStatic
import io.micronaut.context.annotation.ConfigurationProperties
import io.micronaut.core.annotation.Nullable
import jakarta.validation.constraints.NotBlank

import java.time.Instant

@CompileStatic
@ConfigurationProperties("mauro")
class MauroConfiguration {

    List<CatalogueUserConfig> users
    List<UserGroupConfig> groups
    List<ApiKeyConfig> apiKeys
    List<ApiPropertyConfig> apiProperties
    List<OAuthConfig> oauths
    @Nullable LegacyOAuthConfig oauth

    static class CatalogueUserConfig {

        @NotBlank
        String email
        @NotBlank
        String firstName
        @NotBlank
        String lastName
        @NotBlank
        String tempPassword
    }

    static class UserGroupConfig {

        @NotBlank
        String name
        String description
        Boolean isAdmin
        List<String> members
    }

    static class ApiKeyConfig {

        // create unique index "idx_api_key_catalogue_user_id_name_unique" on  security.api_key (catalogue_user_id, name);
        @NotBlank
        String name
        @NotBlank
        String email
        UUID key
        Boolean refreshable
        Instant expiry
    }

    static class ApiPropertyConfig {
        @NotBlank
        String key
        @NotBlank
        String value

        @Nullable
        Boolean publiclyVisible

        @Nullable
        @NotBlank
        String category
    }

    static class OAuthConfig {
        @NotBlank
        String oauthProvider
        @Nullable
        Boolean createUser
        @Nullable
        Boolean requireVerifiedEmail
        @Nullable
        Map<String, String> tokenCustomValidation
        @NotBlank
        String appLabel
        @Nullable
        @NotBlank
        String appImageUrl
        @Nullable
        @NotBlank
        String appLoginSuccess
        @Nullable
        @NotBlank
        List<String> uiRedirectUrls
    }

    @ConfigurationProperties("oauth")
    static class LegacyOAuthConfig {

        @Nullable @NotBlank String id
        @Nullable String label
        @Nullable Boolean standardProvider
        @Nullable String authorizationEndpoint
        @Nullable String imageUrl
        @Nullable String loginSuccess
        @Nullable Boolean createUser
        @Nullable List<String> uiRedirectUrls
    }
}

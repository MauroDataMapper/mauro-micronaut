package org.maurodata.controller.bootstrap

import groovy.beans.Bindable
import io.micronaut.context.annotation.ConfigurationProperties
import jakarta.validation.constraints.NotBlank

import java.time.Instant


@ConfigurationProperties("mauro")
class MauroConfiguration {

    List<CatalogueUserConfig> users
    List<UserGroupConfig> groups
    List<ApiKeyConfig> apiKeys

    public static class CatalogueUserConfig {

        @NotBlank
        String email
        @NotBlank
        String firstName
        @NotBlank
        String lastName
        @NotBlank
        String tempPassword
    }

    public static class UserGroupConfig {

        @NotBlank
        String name
        String description
        Boolean isAdmin
        List<String> members
    }

    public static class ApiKeyConfig {

        // create unique index "idx_api_key_catalogue_user_id_name_unique" on  security.api_key (catalogue_user_id, name);
        @NotBlank
        String name
        @NotBlank
        String email
        UUID key
        Boolean refreshable
        Instant expiry
    }
}

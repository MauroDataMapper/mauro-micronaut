package org.maurodata.domain.security


import org.maurodata.domain.model.Item
import org.maurodata.domain.model.ItemUtils

import com.fasterxml.jackson.annotation.JsonAlias
import groovy.transform.AutoClone
import groovy.transform.CompileStatic
import groovy.transform.MapConstructor
import io.micronaut.data.annotation.Index
import io.micronaut.data.annotation.Indexes
import io.micronaut.data.annotation.MappedEntity
import io.micronaut.data.annotation.MappedProperty
import jakarta.persistence.Id
import jakarta.persistence.Transient

import java.time.Instant
import java.time.temporal.ChronoUnit

@CompileStatic
@MappedEntity(schema = 'security')
@AutoClone
@Indexes([@Index(columns = ['catalogueUser', 'name'], unique = true)])
@MapConstructor(includeSuperFields = true, includeSuperProperties = true, noArg = true)
class ApiKey extends Item {

    @Id
    UUID id
    //TODO: Rename this to 'label'
    String name

    Instant expiryDate

    Boolean refreshable = true
    Boolean disabled = false

    @Transient
    Long expiresInDays

    /**
     * The owner of this API Key object.
     */
    @JsonAlias(['catalogue_user_id'])
    @MappedProperty('catalogue_user_id')
    UUID catalogueUserId

    void updateExpiryDate() {
        if (expiresInDays) {
            expiryDate = Instant.now().plus(expiresInDays, ChronoUnit.DAYS)
            expiresInDays = null
        }
    }

    // TODO - this is included for UI compatability - can remove it after a change to the UI
    @Transient
    UUID getApiKey() {
        id
    }

    @Transient
    boolean getExpired() {
        Instant.now() > expiryDate
    }

    /****
     * Methods for building a tree-like DSL
     */
    static ApiKey build(
        Map args,
        @DelegatesTo(value = ApiKey, strategy = Closure.DELEGATE_FIRST) Closure closure = {}) {
        new ApiKey(args).tap(closure)
    }

    static ApiKey build(@DelegatesTo(value = ApiKey, strategy = Closure.DELEGATE_FIRST) Closure closure = {}) {
        build [:], closure
    }

    String name(String name) {
        this.name = name
        this.name
    }

    Instant expiryDate(Instant expiryDate) {
        this.expiryDate = expiryDate
        expiryDate
    }

    Instant expiryDate(String expiryDate) {
        this.expiryDate = Instant.parse(expiryDate)
        this.expiryDate
    }

    Instant expiresInDays(long expiresInDays) {
        setExpiresInDays(expiresInDays)
        this.expiryDate
    }


    Boolean refreshable(Boolean refreshable) {
        this.refreshable = refreshable
        refreshable
    }

    Boolean disabled(Boolean disabled) {
        this.disabled = disabled
        disabled
    }

    UUID catalogueUserId(UUID catalogueUserId) {
        this.catalogueUserId = catalogueUserId
        catalogueUserId
    }

    @Override
    void copyInto(Item into) {
        super.copyInto(into)
        ApiKey intoApiKey = (ApiKey) into
        intoApiKey.id = ItemUtils.copyItem(this.id, intoApiKey.id)
        intoApiKey.name = ItemUtils.copyItem(this.name, intoApiKey.name)
        intoApiKey.expiryDate = ItemUtils.copyItem(this.expiryDate, intoApiKey.expiryDate)
        intoApiKey.refreshable = ItemUtils.copyItem(this.refreshable, intoApiKey.refreshable)
        intoApiKey.disabled = ItemUtils.copyItem(this.disabled, intoApiKey.disabled)
        intoApiKey.expiresInDays = ItemUtils.copyItem(this.expiresInDays, intoApiKey.expiresInDays)
        intoApiKey.catalogueUserId = ItemUtils.copyItem(this.catalogueUserId, intoApiKey.catalogueUserId)
    }

    @Override
    Item shallowCopy() {
        ApiKey apiKeyShallowCopy = new ApiKey()
        this.copyInto(apiKeyShallowCopy)
        return apiKeyShallowCopy
    }
}

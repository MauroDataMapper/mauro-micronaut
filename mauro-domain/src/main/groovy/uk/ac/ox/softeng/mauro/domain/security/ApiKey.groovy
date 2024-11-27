package uk.ac.ox.softeng.mauro.domain.security

import uk.ac.ox.softeng.mauro.domain.model.Item

import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.annotation.JsonProperty
import groovy.transform.AutoClone
import groovy.transform.CompileStatic
import groovy.transform.MapConstructor
import io.micronaut.core.annotation.Nullable
import io.micronaut.data.annotation.Index
import io.micronaut.data.annotation.Indexes
import io.micronaut.data.annotation.MappedEntity
import io.micronaut.data.annotation.MappedProperty

import java.time.Instant


@CompileStatic
@MappedEntity(schema = 'security')
@AutoClone
@Indexes([@Index(columns = ['catalogueUser', 'name'], unique = true)])
@MapConstructor(includeSuperFields = true, includeSuperProperties = true, noArg = true)
class ApiKey extends Item {

    //TODO: Rename this to 'label'
    String name
    Instant expiryDate
    Boolean refreshable = true
    Boolean disabled = false

    /**
     * The owner of this API Key object.
     */
    @JsonAlias(['catalogue_user_id'])
    @MappedProperty('catalogue_user_id')
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    UUID catalogueUserId


    /****
     * Methods for building a tree-like DSL
     */
    static ApiKey build(
        Map args,
        @DelegatesTo(value = ApiKey, strategy = Closure.DELEGATE_FIRST) Closure closure = { }) {
        new ApiKey(args).tap(closure)
    }

    static ApiKey build(@DelegatesTo(value = ApiKey, strategy = Closure.DELEGATE_FIRST) Closure closure = { }) {
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


}

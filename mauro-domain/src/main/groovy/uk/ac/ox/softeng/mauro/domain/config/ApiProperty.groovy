package uk.ac.ox.softeng.mauro.domain.config

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import groovy.transform.AutoClone
import groovy.transform.CompileStatic
import io.micronaut.core.annotation.Nullable
import io.micronaut.data.annotation.*
import jakarta.validation.constraints.NotBlank
import uk.ac.ox.softeng.mauro.domain.model.Item
import uk.ac.ox.softeng.mauro.domain.security.CatalogueUser

@CompileStatic
@MappedEntity(schema = 'core')
@AutoClone
@Indexes([@Index(columns = ['key'], unique = true)])
class ApiProperty extends Item {

    String key

    String value

    Boolean publiclyVisible

    @Nullable
    @NotBlank
    String category

    @Nullable
    @MappedProperty('last_updated_by')
    @JsonIgnore
    CatalogueUser lastUpdatedBy

    @Transient
    @JsonProperty('lastUpdatedBy')
    String getLastUpdatedByEmailAddress() {
        lastUpdatedBy?.emailAddress
    }
}

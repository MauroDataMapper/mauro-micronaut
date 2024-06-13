package uk.ac.ox.softeng.mauro.domain.config

import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.annotation.JsonProperty
import groovy.transform.AutoClone
import groovy.transform.CompileStatic
import io.micronaut.core.annotation.Nullable
import io.micronaut.data.annotation.Index
import io.micronaut.data.annotation.Indexes
import io.micronaut.data.annotation.MappedEntity
import jakarta.validation.constraints.NotBlank
import uk.ac.ox.softeng.mauro.domain.model.Item
import uk.ac.ox.softeng.mauro.domain.security.CatalogueUser

import java.beans.Transient

@CompileStatic
@MappedEntity(schema = 'core')
@AutoClone
@Indexes([@Index(columns = ['key'], unique = true)])
class ApiProperty extends Item {

    String key

    String value

    Boolean publiclyVisisble

    @Nullable
    @NotBlank
    String category

    @Nullable
    @JsonAlias(['last_updated_by'])
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    CatalogueUser lastUpdatedBy

    @Transient
    String getLastUpdatedBy() {
        lastUpdatedBy?.emailAddress
    }
}

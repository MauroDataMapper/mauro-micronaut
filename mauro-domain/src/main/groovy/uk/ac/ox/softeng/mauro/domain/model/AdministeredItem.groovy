package uk.ac.ox.softeng.mauro.domain.model

import uk.ac.ox.softeng.mauro.exception.MauroInternalException

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import groovy.transform.CompileStatic
import io.micronaut.core.annotation.Introspected
import io.micronaut.core.annotation.Nullable
import io.micronaut.data.annotation.DateCreated
import io.micronaut.data.annotation.DateUpdated
import io.micronaut.data.annotation.GeneratedValue
import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.Index
import io.micronaut.data.annotation.Indexes
import io.micronaut.data.annotation.MappedEntity
import io.micronaut.data.annotation.TypeDef
import io.micronaut.data.annotation.Version
import io.micronaut.data.model.DataType
import io.micronaut.validation.Validated
import jakarta.persistence.Transient
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern

import java.time.OffsetDateTime

@CompileStatic
abstract class AdministeredItem {

    @Id
    @GeneratedValue
    UUID id

    @Version
    Integer version

    @DateCreated
    OffsetDateTime dateCreated

    @DateUpdated
    OffsetDateTime lastUpdated

    @NotBlank
    @Pattern(regexp = /[^\$@|]*/, message = 'Cannot contain $, | or @')
    String label

    @Nullable
    String description

    @Nullable
    String aliasesString

    @Transient
    String domainType = this.class.simpleName

    @Nullable
    String createdBy

    @TypeDef(type = DataType.STRING, converter = Path.PathConverter)
    Path path // should be Path type

    @Nullable
    UUID breadcrumbTreeId // should be BreadcrumbTree type

    @Transient
    @JsonIgnore
    abstract AdministeredItem getParent()

    @Transient
    @JsonIgnore
    Model getOwner() {
        parent.owner
    }

    @Transient
    @JsonIgnore
    String getPathPrefix() {
        null
    }

    @Transient
    @JsonIgnore
    String getPathIdentifier() {
        label
    }

    @Transient
    @JsonIgnore
    String getPathModelIdentifier() {
        null
    }

    /**
     * Recalculate this item's Path from its parent.
     */
    Path updatePath() {
        if (!pathPrefix) throw new MauroInternalException('Class [' + this.class.simpleName + '] is not Pathable')
        if (parent) {
            Path parentPath = parent.path
            if (!parentPath) throw new MauroInternalException('Parent does not have Path or it is not loaded')
            path = parentPath.join(new Path.PathNode(prefix: pathPrefix, identifier: pathIdentifier, modelIdentifier: pathModelIdentifier))
            return path
        } else {
            path = new Path()
            path.nodes = [new Path.PathNode(prefix: pathPrefix, identifier: pathIdentifier, modelIdentifier: pathModelIdentifier)]
            return path
        }
    }
}

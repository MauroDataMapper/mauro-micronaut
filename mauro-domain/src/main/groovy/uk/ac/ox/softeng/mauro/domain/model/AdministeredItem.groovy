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
        if (!pathPrefix) throw new MauroInternalException("Class [${this.class.simpleName}] is not Pathable")
        final int pathLimit = 256
        List<Path.PathNode> pathNodes = []
        int i = 0
        AdministeredItem node = this
        while (node && i < pathLimit) {
            pathNodes.add(0, new Path.PathNode(prefix: node.pathPrefix, identifier: node.pathIdentifier, modelIdentifier: node.pathModelIdentifier))
            i++; node = node.parent
            if (i >= pathLimit) throw new MauroInternalException("Path exceeded maximum depth of [$pathLimit]")
        }

        path = new Path()
        path.nodes = pathNodes
        path
    }
}

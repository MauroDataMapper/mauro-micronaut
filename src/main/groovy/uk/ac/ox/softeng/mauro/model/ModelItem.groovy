package uk.ac.ox.softeng.mauro.model

import com.fasterxml.jackson.annotation.JsonProperty
import groovy.transform.CompileStatic
import io.micronaut.core.annotation.Introspected
import io.micronaut.core.annotation.Nullable
import io.micronaut.core.order.Ordered
import io.micronaut.data.annotation.DateCreated
import io.micronaut.data.annotation.DateUpdated
import io.micronaut.data.annotation.GeneratedValue
import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity
import io.micronaut.data.annotation.MappedProperty
import io.micronaut.data.annotation.Version
import jakarta.persistence.Transient

import java.time.OffsetDateTime

@CompileStatic
@Introspected
@MappedEntity
abstract class ModelItem<M extends Model> implements Ordered {

    @Id
    @GeneratedValue
    UUID id

    @Version
    Integer version

    @DateCreated
    OffsetDateTime dateCreated

    @DateUpdated
    OffsetDateTime lastUpdated

    String label

    @MappedProperty('idx')
    @JsonProperty('index')
    int order

    @Nullable
    String description

    @Nullable
    String path // should be Path type

    @Nullable
    UUID breadcrumbTreeId // should be BreadcrumbTree type

    @Nullable
    String createdBy

    @Nullable
    String aliasesString

    @Transient
    String domainType
}

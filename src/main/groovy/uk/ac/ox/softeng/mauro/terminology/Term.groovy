package uk.ac.ox.softeng.mauro.terminology

import com.fasterxml.jackson.annotation.JsonIgnore
import groovy.transform.CompileStatic
import io.micronaut.core.annotation.Introspected
import io.micronaut.core.annotation.Nullable
import io.micronaut.data.annotation.GeneratedValue
import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.Index
import io.micronaut.data.annotation.Indexes
import io.micronaut.data.annotation.MappedEntity
import io.micronaut.data.annotation.Version

@CompileStatic
@Introspected
@MappedEntity
@Indexes([@Index(columns = ['terminology_id', 'code'], unique = true)])
class Term {

    @Id
    @GeneratedValue
    UUID id

    @JsonIgnore
    Terminology terminology

    @Version
    Integer version

    String code

    @Nullable
    String definition

    @Nullable
    String description

    @Nullable
    String url
}

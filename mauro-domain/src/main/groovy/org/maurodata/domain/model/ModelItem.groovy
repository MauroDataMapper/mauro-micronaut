package org.maurodata.domain.model

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import groovy.transform.CompileStatic
import groovy.transform.Sortable
import io.micronaut.core.annotation.Introspected
import io.micronaut.core.annotation.Nullable
import io.micronaut.data.annotation.MappedEntity
import io.micronaut.data.annotation.MappedProperty
import io.micronaut.data.annotation.Transient
import org.maurodata.domain.folder.Folder

/**
 * A ModelItem is a component of a model that is ordered - for example the data classes in a data model or the
 * data elements within a data class.
 */
@CompileStatic
@Introspected
@MappedEntity
@Sortable(includes = ['order', 'label'], includeSuperProperties = true)
abstract class ModelItem<P extends AdministeredItem> extends AdministeredItem {

    @JsonProperty('index')
    @Nullable
    @MappedProperty('idx')
    Integer order

    /**
     * for modelitem import
     */
    @Nullable
    @Transient
    @JsonIgnore
    Folder folder

}
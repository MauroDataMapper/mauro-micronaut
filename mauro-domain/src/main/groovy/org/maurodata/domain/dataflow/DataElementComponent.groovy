package org.maurodata.domain.dataflow

import com.fasterxml.jackson.annotation.JsonIgnore
import groovy.transform.AutoClone
import groovy.transform.CompileStatic
import groovy.transform.MapConstructor
import io.micronaut.core.annotation.Introspected
import io.micronaut.core.annotation.Nullable
import io.micronaut.data.annotation.MappedEntity
import io.micronaut.data.annotation.Relation
import jakarta.persistence.JoinColumn
import jakarta.persistence.JoinTable
import jakarta.persistence.Transient
import jakarta.validation.constraints.NotNull
import org.maurodata.domain.datamodel.DataElement
import org.maurodata.domain.model.AdministeredItem
import org.maurodata.domain.model.ModelItem

/**
 * A DataElementComponent is associated with a DataClassComponent.
 * It contains lists of source and target dataModel.dataElements
 */
@CompileStatic
@AutoClone
@Introspected
@MappedEntity(schema = 'dataflow')
@MapConstructor(includeSuperFields = true, includeSuperProperties = true, noArg = true)
class DataElementComponent extends ModelItem<DataClassComponent> {
    @Nullable
    String definition

    @Relation(value = Relation.Kind.MANY_TO_MANY)
    @JoinTable(
            name = "data_element_component_source_data_element",
            joinColumns = @JoinColumn(name = "data_element_component_id"),
            inverseJoinColumns = @JoinColumn(name = "data_element_id"))
    List<DataElement> sourceDataElements = []

    @Relation(value = Relation.Kind.MANY_TO_MANY)
    @JoinTable(
            name = "data_element_component_target_data_element",
            joinColumns = @JoinColumn(name = "data_element_component_id"),
            inverseJoinColumns = @JoinColumn(name = "data_element_id"))
    List<DataElement> targetDataElements = []

    @NotNull
    DataClassComponent dataClassComponent

    @Transient
    UUID breadcrumbTreeId

    @Override
    @Transient
    @JsonIgnore
    AdministeredItem getParent() {
        this.dataClassComponent
    }

    @Transient
    @JsonIgnore
    @Override
    void setParent(AdministeredItem dataClassComponent) {
        this.dataClassComponent = (DataClassComponent) dataClassComponent
    }

    @Override
    String getDomainType() {
        DataElementComponent.simpleName
    }

    @Override
    @Transient
    @JsonIgnore
    String getPathPrefix() {
        'dec'
    }

}

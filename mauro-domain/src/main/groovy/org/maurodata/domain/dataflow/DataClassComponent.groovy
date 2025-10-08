package org.maurodata.domain.dataflow

import org.maurodata.domain.model.Item
import org.maurodata.domain.model.ItemUtils

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
import org.maurodata.domain.datamodel.DataClass
import org.maurodata.domain.model.AdministeredItem
import org.maurodata.domain.model.ModelItem

/**
 * A DataClassComponent is associated with a DataFlow.
 * It contains lists of source and target dataModel.dataClasses
 */
@CompileStatic
@AutoClone
@Introspected
@MappedEntity(schema = 'dataflow')
@MapConstructor(includeSuperFields = true, includeSuperProperties = true, noArg = true)
class DataClassComponent extends ModelItem<DataFlow> {

    @NotNull
    @JsonIgnore
    DataFlow dataFlow

    @Nullable
    String definition

    @Relation(value = Relation.Kind.MANY_TO_MANY)
    @JoinTable(
        name = "data_class_component_target_data_class",
        joinColumns = @JoinColumn(name = "data_class_component_id"),
        inverseJoinColumns = @JoinColumn(name = "data_class_id"))
    List<DataClass> targetDataClasses = []

    @Relation(value = Relation.Kind.MANY_TO_MANY)
    @JoinTable(
        name = "data_class_component_source_data_class",
        joinColumns = @JoinColumn(name = "data_class_component_id"),
        inverseJoinColumns = @JoinColumn(name = "data_class_id"))
    List<DataClass> sourceDataClasses = []

    @Relation(value = Relation.Kind.ONE_TO_MANY, mappedBy = 'dataElementComponent')
    List<DataElementComponent> dataElementComponents = []

    @Transient
    UUID breadcrumbTreeId

    @Override
    @Transient
    @JsonIgnore
    AdministeredItem getParent() {
        dataFlow
    }

    @Override
    @Transient
    @JsonIgnore
    void setParent(AdministeredItem dataFlow) {
        this.dataFlow = (DataFlow) dataFlow
    }

    @Override
    @Transient
    @JsonIgnore
    String getPathPrefix() {
        'dcc'
    }

    @Transient
    @JsonIgnore
    List<Collection<? extends ModelItem<DataClassComponent>>> getAllAssociations() {
        [dataElementComponents] as List<Collection<? extends ModelItem<DataClassComponent>>>
    }

    @Override
    void copyInto(Item into) {
        super.copyInto(into)
        DataClassComponent intoDataClassComponent = (DataClassComponent) into
        intoDataClassComponent.dataFlow = ItemUtils.copyItem(this.dataFlow, intoDataClassComponent.dataFlow)
        intoDataClassComponent.definition = ItemUtils.copyItem(this.definition, intoDataClassComponent.definition)
        intoDataClassComponent.targetDataClasses = ItemUtils.copyItems(this.targetDataClasses, intoDataClassComponent.targetDataClasses)
        intoDataClassComponent.sourceDataClasses = ItemUtils.copyItems(this.sourceDataClasses, intoDataClassComponent.sourceDataClasses)
        intoDataClassComponent.dataElementComponents = ItemUtils.copyItems(this.dataElementComponents, intoDataClassComponent.dataElementComponents)
        intoDataClassComponent.breadcrumbTreeId = ItemUtils.copyItem(breadcrumbTreeId, intoDataClassComponent.breadcrumbTreeId)
    }

    @Override
    Item shallowCopy() {
        DataClassComponent dataClassComponentShallowCopy = new DataClassComponent()
        this.copyInto(dataClassComponentShallowCopy)
        return dataClassComponentShallowCopy
    }
}

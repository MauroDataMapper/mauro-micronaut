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
import jakarta.persistence.Transient
import jakarta.validation.constraints.NotNull
import org.maurodata.domain.datamodel.DataModel
import org.maurodata.domain.model.AdministeredItem
import org.maurodata.domain.model.ModelItem

/**
 * A DataFlow is has source and target dataModels
 */
@CompileStatic
@AutoClone
@Introspected
@MappedEntity(schema = 'dataflow')
@MapConstructor(includeSuperFields = true, includeSuperProperties = true, noArg = true)
class DataFlow extends ModelItem<DataModel> {
    @Nullable
    String diagramLayout

    @Nullable
    String definition

    @NotNull
    DataModel source

    @NotNull
    DataModel target

    @Relation(value = Relation.Kind.ONE_TO_MANY, mappedBy = 'dataClassComponent')
    List<DataClassComponent> dataClassComponents = []

    @Transient
    UUID breadcrumbTreeId


    @Override
    String getDomainType() {
        DataFlow.simpleName
    }

    Boolean hasChildren() {
        dataClassComponents
    }

    @Transient
    @JsonIgnore
    @Override
    void setAssociations() {
        super.setAssociations()
        dataClassComponents.each {dataClassComponent ->
            dataClassComponent.dataFlow = this
            dataClassComponent.setAssociations()
            dataClassComponent.dataElementComponents.each {dataElementComponent ->
                dataElementComponent.dataClassComponent = dataClassComponent
                dataElementComponent.setAssociations()
            }
        }

    }


    @Override
    @Transient
    @JsonIgnore
    AdministeredItem getParent() {
        return target
    }

    @Override
    @Transient
    @JsonIgnore
    void setParent(AdministeredItem parent) {
        this.target = parent as DataModel
    }


    @Override
    @Transient
    @JsonIgnore
    String getPathPrefix() {
        'df'
    }

    @Override
    void copyInto(Item into) {
        super.copyInto(into)
        DataFlow intoDataFlow = (DataFlow) into
        intoDataFlow.diagramLayout = ItemUtils.copyItem(this.diagramLayout, intoDataFlow.diagramLayout)
        intoDataFlow.definition = ItemUtils.copyItem(this.definition, intoDataFlow.definition)
        intoDataFlow.source = ItemUtils.copyItem(this.source, intoDataFlow.source)
        intoDataFlow.target = ItemUtils.copyItem(this.target, intoDataFlow.target)
        intoDataFlow.dataClassComponents = ItemUtils.copyItems(this.dataClassComponents, intoDataFlow.dataClassComponents)
        intoDataFlow.breadcrumbTreeId = ItemUtils.copyItem(breadcrumbTreeId, intoDataFlow.breadcrumbTreeId)
    }

    @Override
    Item shallowCopy() {
        DataFlow dataFlowShallowCopy = new DataFlow()
        this.copyInto(dataFlowShallowCopy)
        return dataFlowShallowCopy
    }
    @Override
    @Transient
    @JsonIgnore
    List<Collection<? extends ModelItem<DataFlow>>> getAllAssociations() {
        [dataClassComponents] as List<Collection<? extends ModelItem<DataFlow>>>
    }

       /**
     * Builder methods
     * @param args
     * @param closure
     * @return
     */
    static DataFlow build(
        Map args,
        @DelegatesTo(value = DataFlow, strategy = Closure.DELEGATE_FIRST) Closure closure = {}) {
        new DataFlow(args).tap(closure)
    }

    static DataFlow build(
        @DelegatesTo(value = DataFlow, strategy = Closure.DELEGATE_FIRST) Closure closure = {}) {
        build [:], closure
    }
}
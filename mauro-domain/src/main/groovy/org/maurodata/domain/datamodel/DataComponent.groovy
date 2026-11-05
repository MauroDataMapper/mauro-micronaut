package org.maurodata.domain.datamodel

import com.fasterxml.jackson.annotation.JsonIgnore
import groovy.transform.AutoClone
import groovy.transform.CompileStatic
import groovy.transform.MapConstructor
import io.micronaut.core.annotation.Introspected
import io.micronaut.data.annotation.MappedEntity
import io.micronaut.data.annotation.Transient
import org.maurodata.domain.model.AdministeredItem
import org.maurodata.domain.model.Item
import org.maurodata.domain.model.ItemUtils
import org.maurodata.domain.model.Model
import org.maurodata.domain.model.ModelItem
import org.maurodata.domain.diff.DiffBuilder
import org.maurodata.domain.diff.DiffableItem
import org.maurodata.domain.diff.ObjectDiff
import org.maurodata.visitor.DomainVisitor

/**
 * A component within a DataModel. Its type determines the rules governing
 * how it is used. DataFlow ItemLinks express the inputs and outputs for a
 * component of type DataFlow.
 */
@CompileStatic
@AutoClone(excludes = ['dataModel'])
@Introspected
@MappedEntity(schema = 'datamodel', value = 'data_component')
@MapConstructor(includeSuperFields = true, includeSuperProperties = true, noArg = true)
class DataComponent extends ModelItem<DataModel> implements DiffableItem<DataComponent> {

    static final String DATA_FLOW_TYPE = 'DataFlow'

    String type

    @JsonIgnore
    DataModel dataModel

    @Override
    <T> T accept(DomainVisitor<T> visitor) {
        visitor.visitDataComponent(this)
    }

    @Override
    @Transient
    @JsonIgnore
    AdministeredItem getParent() {
        dataModel
    }

    @Override
    @Transient
    @JsonIgnore
    void setParent(AdministeredItem parent) {
        dataModel = (DataModel) parent
    }

    @Override
    @Transient
    @JsonIgnore
    Model getOwner() {
        dataModel?.owner ?: super.getOwner()
    }

    @Override
    @Transient
    @JsonIgnore
    String getPathPrefix() {
        'dcp'
    }

    @Override
    @Transient
    @JsonIgnore
    String getPathIdentifier() {
        "${type}: ${label}"
    }

    @Override
    @Transient
    @JsonIgnore
    String getDiffIdentifier() {
        dataModel ? "${dataModel.diffIdentifier}|${getPathNodeString()}" : getPathNodeString()
    }

    @Override
    @Transient
    @JsonIgnore
    ObjectDiff<DataComponent> diff(DataComponent other, String lhsPathRoot, String rhsPathRoot) {
        ObjectDiff<DataComponent> result = DiffBuilder.objectDiff(DataComponent)
            .leftHandSide(id?.toString(), this)
            .rightHandSide(other.id?.toString(), other)
        result.label = label
        result.appendString('type', type, other.type, this, other)
        result.appendString(DiffBuilder.DESCRIPTION, description, other.description, this, other)
        result.appendString(DiffBuilder.ALIASES_STRING, aliasesString, other.aliasesString, this, other)
        result
    }

    @Override
    void copyInto(Item into) {
        super.copyInto(into)
        DataComponent intoComponent = (DataComponent) into
        intoComponent.type = type
        intoComponent.dataModel = ItemUtils.copyItem(dataModel, intoComponent.dataModel)
    }

    @Override
    Item shallowCopy() {
        DataComponent copy = new DataComponent()
        copyInto(copy)
        copy
    }
}

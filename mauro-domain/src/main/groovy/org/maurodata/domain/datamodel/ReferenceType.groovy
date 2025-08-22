package org.maurodata.domain.datamodel

import org.maurodata.domain.diff.BaseCollectionDiff
import org.maurodata.domain.diff.CollectionDiff
import org.maurodata.domain.diff.DiffBuilder
import org.maurodata.domain.diff.DiffableItem
import org.maurodata.domain.diff.ObjectDiff
import org.maurodata.domain.model.AdministeredItem
import org.maurodata.domain.model.ModelItem

import com.fasterxml.jackson.annotation.JsonIgnore
import groovy.transform.AutoClone
import groovy.transform.CompileStatic
import groovy.transform.MapConstructor
import io.micronaut.core.annotation.Introspected
import jakarta.persistence.Transient

@CompileStatic
@AutoClone
@MapConstructor(includeSuperFields = true, includeSuperProperties = true, noArg = true)
@Introspected
class ReferenceType extends ModelItem<DataType> implements DiffableItem<ReferenceType> {

    DataClass getReferenceClass(){
        referenceClass
    }

    @Override
    String getDomainType() {
        domainType = ReferenceType.simpleName
    }

    @Override
    CollectionDiff fromItem() {
        new BaseCollectionDiff(id, getDiffIdentifier(), label)
    }

    @Override
    String getDiffIdentifier() {
        // TODO: Not sure what the path to this is supposed to be
        getPathNodeString()
    }

    @Override
    @JsonIgnore
    @Transient
    ObjectDiff<ReferenceType> diff(ReferenceType other, String lhsPathRoot, String rhsPathRoot) {
        ObjectDiff<ReferenceType> base = DiffBuilder.objectDiff(ReferenceType)
            .leftHandSide(id?.toString(), this)
            .rightHandSide(other.id?.toString(), other)
        base.label = this.label
        base.appendString(DiffBuilder.DESCRIPTION, this.description, other.description, this, other)
        base.appendString(DiffBuilder.ALIASES_STRING, this.aliasesString, other.aliasesString, this, other)
        //todo check diff
        base
    }

    @Override
    AdministeredItem getParent() {
        return null
    }

    @Override
    void setParent(AdministeredItem parent) {

    }
}

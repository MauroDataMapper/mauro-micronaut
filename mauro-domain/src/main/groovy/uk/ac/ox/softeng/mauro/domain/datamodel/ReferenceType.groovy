package uk.ac.ox.softeng.mauro.domain.datamodel

import uk.ac.ox.softeng.mauro.domain.diff.CollectionDiff
import uk.ac.ox.softeng.mauro.domain.diff.DiffBuilder
import uk.ac.ox.softeng.mauro.domain.diff.DiffableItem
import uk.ac.ox.softeng.mauro.domain.diff.ObjectDiff
import uk.ac.ox.softeng.mauro.domain.model.AdministeredItem
import uk.ac.ox.softeng.mauro.domain.model.ModelItem

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
    @Override
    String getDomainType() {
        domainType = ReferenceType.simpleName
    }

    @Override
    CollectionDiff fromItem() {
        return null
    }

    @Override
    String getDiffIdentifier() {
        return null
    }

    @Override
    @JsonIgnore
    @Transient
    ObjectDiff<ReferenceType> diff(ReferenceType other) {
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

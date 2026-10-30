package org.maurodata.domain.facet

import com.fasterxml.jackson.annotation.JsonIgnore
import groovy.transform.AutoClone
import groovy.transform.CompileStatic
import groovy.transform.MapConstructor
import io.micronaut.data.annotation.MappedEntity
import io.micronaut.data.annotation.TypeDef
import io.micronaut.data.annotation.Transient
import io.micronaut.data.model.DataType
import org.maurodata.domain.model.Path
import org.maurodata.domain.model.Item
import org.maurodata.visitor.DomainVisitor

@CompileStatic
@MappedEntity(schema = 'core')
@AutoClone
@MapConstructor(includeSuperFields = true, includeSuperProperties = true, noArg = true)
class ItemLink extends Facet {

    String type

    @TypeDef(type = DataType.STRING, converter = Path.PathConverter)
    Path path

    @Override
    <T> T accept(DomainVisitor<T> visitor) {
        visitor.visitItemLink(this)
    }

    @Override
    @Transient
    @JsonIgnore
    String getPathPrefix() {
        'il'
    }

    @Override
    @Transient
    @JsonIgnore
    String getPathIdentifier() {
        path?.toString()
    }

    @Override
    void copyInto(Item into) {
        super.copyInto(into)
        ItemLink intoItemLink = (ItemLink) into
        intoItemLink.type = type
        intoItemLink.path = path ? new Path(path.toString()) : null
    }

    @Override
    Item shallowCopy() {
        ItemLink copy = new ItemLink()
        copyInto(copy)
        copy
    }
}

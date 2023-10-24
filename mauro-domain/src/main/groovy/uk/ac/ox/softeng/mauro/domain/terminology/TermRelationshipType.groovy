
package uk.ac.ox.softeng.mauro.domain.terminology

import uk.ac.ox.softeng.mauro.domain.model.AdministeredItem

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import groovy.transform.CompileStatic
import io.micronaut.core.annotation.Introspected
import io.micronaut.core.annotation.Nullable
import io.micronaut.data.annotation.Index
import io.micronaut.data.annotation.Indexes
import io.micronaut.data.annotation.MappedEntity
import jakarta.persistence.Transient
import uk.ac.ox.softeng.mauro.domain.model.ModelItem

@CompileStatic
@Introspected
@MappedEntity
@Indexes([@Index(columns = ['terminology_id', 'label'], unique = true)])
class TermRelationshipType extends ModelItem<Terminology> {

    @JsonIgnore
    Terminology terminology

    @Nullable
    Boolean parentalRelationship

    @Nullable
    Boolean childRelationship

    @Override
    @Transient
    @JsonIgnore
    Terminology getParent() {
        terminology
    }

    @Override
    @Transient
    @JsonIgnore
    void setParent(AdministeredItem terminology) {
        this.terminology = (Terminology) terminology
    }

    @Override
    @Transient
    @JsonIgnore
    String getPathPrefix() {
        'trt'
    }
}

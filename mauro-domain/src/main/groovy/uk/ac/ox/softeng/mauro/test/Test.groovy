package uk.ac.ox.softeng.mauro.test

import uk.ac.ox.softeng.mauro.domain.model.AdministeredItem
import uk.ac.ox.softeng.mauro.domain.model.Model
import uk.ac.ox.softeng.mauro.domain.terminology.Term

import com.fasterxml.jackson.annotation.JsonIgnore
import groovy.transform.CompileStatic
import io.micronaut.data.annotation.MappedEntity
import io.micronaut.data.annotation.MappedProperty
import io.micronaut.data.annotation.Relation
import io.micronaut.data.model.DataType
import jakarta.persistence.Transient

@CompileStatic
@MappedEntity
class Test extends Model {

    @Relation(value = Relation.Kind.ONE_TO_MANY)
//    @MappedProperty(type = DataType.JSON)
//    @JsonIgnore
    List<Term> terms

    @Override
    @Transient
    @JsonIgnore
    Collection<AdministeredItem> getAllContents() {
        return null
    }

    @Override
    @Transient
    @JsonIgnore
    String getPathPrefix() {
        'test'
    }
}

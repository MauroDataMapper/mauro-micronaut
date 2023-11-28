package uk.ac.ox.softeng.mauro.test

import uk.ac.ox.softeng.mauro.domain.facet.Facet
import uk.ac.ox.softeng.mauro.domain.model.AdministeredItem
import uk.ac.ox.softeng.mauro.domain.model.Model
import uk.ac.ox.softeng.mauro.domain.terminology.Term

import com.fasterxml.jackson.annotation.JsonIgnore
import groovy.transform.CompileStatic
import io.micronaut.core.annotation.Introspected
import io.micronaut.data.annotation.MappedEntity
import io.micronaut.data.annotation.MappedProperty
import io.micronaut.data.annotation.Relation
import io.micronaut.data.annotation.TypeDef
import io.micronaut.data.model.DataType
import jakarta.persistence.Column
import jakarta.persistence.Transient

@CompileStatic
@MappedEntity
@Introspected
class Test extends Facet {

    @Relation(value = Relation.Kind.ONE_TO_MANY)
    List<Term> terms

//    void setTerms(List<Term> terms) {
//        this.terms = terms
//    }
//
//    @TypeDef(type = DataType.JSON)
//    List<Term> getTerms() {
//        terms
//    }

//    @Override
//    @Transient
//    @JsonIgnore
//    Collection<AdministeredItem> getAllContents() {
//        return null
//    }
//
//    @Override
//    @Transient
//    @JsonIgnore
//    String getPathPrefix() {
//        'test'
//    }
}

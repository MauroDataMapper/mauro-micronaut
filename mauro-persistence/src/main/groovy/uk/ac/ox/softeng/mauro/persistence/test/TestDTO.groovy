package uk.ac.ox.softeng.mauro.persistence.test

import uk.ac.ox.softeng.mauro.domain.model.AdministeredItem
import uk.ac.ox.softeng.mauro.domain.model.Model
import uk.ac.ox.softeng.mauro.domain.terminology.Term
import uk.ac.ox.softeng.mauro.test.Test

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.annotation.JsonNaming
import groovy.transform.CompileStatic
import io.micronaut.core.annotation.Introspected
import io.micronaut.data.annotation.Transient
import io.micronaut.data.annotation.TypeDef
import io.micronaut.data.model.DataType

@CompileStatic
@Introspected
class TestDTO extends Test {

    String label

//    @JsonIgnore
    @TypeDef(type = DataType.JSON)
    List<Term> terms

//    @Transient
//    List<Term> termsParsed

//    @Override
//    @jakarta.persistence.Transient
//    @JsonIgnore
//    Collection<AdministeredItem> getAllContents() {
//        return null
//    }
//
//    @Override
//    @jakarta.persistence.Transient
//    @JsonIgnore
//    String getPathPrefix() {
//        'test'
//    }
}

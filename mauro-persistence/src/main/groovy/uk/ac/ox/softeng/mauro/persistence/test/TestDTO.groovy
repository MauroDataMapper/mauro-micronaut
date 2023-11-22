package uk.ac.ox.softeng.mauro.persistence.test

import uk.ac.ox.softeng.mauro.domain.terminology.Term

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.JsonNode
import io.micronaut.core.annotation.Introspected
import io.micronaut.data.annotation.Transient
import io.micronaut.data.annotation.TypeDef
import io.micronaut.data.model.DataType

@Introspected
class TestDTO {

    UUID id

    String label

//    @JsonIgnore
    @TypeDef(type = DataType.JSON)
    JsonNode terms

//    @Transient
//    List<Term> termsParsed
}

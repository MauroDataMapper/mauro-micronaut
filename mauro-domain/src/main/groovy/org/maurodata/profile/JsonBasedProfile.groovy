package org.maurodata.profile

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.ObjectReader
import groovy.transform.CompileStatic

@CompileStatic
abstract class JsonBasedProfile implements Profile {

    @JsonIgnore
    abstract String getJsonFileName()

    JsonBasedProfile(ObjectMapper objectMapper ) {
        ObjectReader objectReader = objectMapper.readerForUpdating(this)
        objectReader.readValue(getClass().getClassLoader().getResourceAsStream(getJsonFileName()))
    }

}

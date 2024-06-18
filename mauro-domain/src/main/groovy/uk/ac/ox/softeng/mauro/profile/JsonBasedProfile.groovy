package uk.ac.ox.softeng.mauro.profile

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.ObjectReader
import io.micronaut.core.annotation.Creator
import jakarta.inject.Inject
import uk.ac.ox.softeng.mauro.plugin.PluginType

abstract class JsonBasedProfile implements Profile {

    ObjectMapper objectMapper

    abstract String getJsonFileName()

    JsonBasedProfile(ObjectMapper objectMapper ) {
        this.objectMapper = objectMapper
        ObjectReader objectReader = this.objectMapper.readerForUpdating(this)
        objectReader.readValue(getClass().getClassLoader().getResourceAsStream(getJsonFileName()))
    }

}

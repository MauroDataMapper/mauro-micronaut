package uk.ac.ox.softeng.mauro.profile

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.ObjectReader
import io.micronaut.core.annotation.Creator
import jakarta.inject.Inject
import uk.ac.ox.softeng.mauro.plugin.PluginType

abstract class JsonBasedProfile implements Profile {

    abstract String getJsonFileName()

    JsonBasedProfile() {
        ObjectReader objectReader = (new ObjectMapper()).readerForUpdating(this)
        objectReader.readValue(getClass().getClassLoader().getResourceAsStream(getJsonFileName()))

        System.err.println(sections.size())
    }
}

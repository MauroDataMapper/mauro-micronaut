package uk.ac.ox.softeng.mauro.export

import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.annotation.JsonProperty
import uk.ac.ox.softeng.mauro.domain.model.AdministeredItem

import com.fasterxml.jackson.annotation.JsonFormat
import groovy.transform.CompileStatic
import io.micronaut.core.annotation.Introspected

import java.time.Instant

@CompileStatic
@Introspected
class ExportMetadata {

    String namespace

    String name

    String version

    @JsonAlias("exportedOn")
    Instant exportDate

    String exportedBy


    @JsonProperty("exporter")
    private void unpackNestedExporter(Map<String,Object> exporter) {
        this.namespace = exporter["namespace"]
        this.name = exporter["name"]
        this.version = exporter["version"]
    }

}

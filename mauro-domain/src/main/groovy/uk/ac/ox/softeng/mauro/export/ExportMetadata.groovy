package uk.ac.ox.softeng.mauro.export

import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.annotation.JsonProperty
import uk.ac.ox.softeng.mauro.domain.model.AdministeredItem

import com.fasterxml.jackson.annotation.JsonFormat
import groovy.transform.CompileStatic
import groovy.transform.MapConstructor
import io.micronaut.core.annotation.Introspected

import java.time.Instant

@CompileStatic
@Introspected
@MapConstructor(includeSuperFields = true, includeSuperProperties = true, noArg = true)
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

    static ExportMetadata build(
        Map args,
        @DelegatesTo(value = ExportMetadata, strategy = Closure.DELEGATE_FIRST) Closure closure = { }) {
        new ExportMetadata(args).tap(closure)
    }

    static ExportMetadata build(
        @DelegatesTo(value = ExportMetadata, strategy = Closure.DELEGATE_FIRST) Closure closure = { }) {
        build [:], closure
    }

    /**
     * DSL helper method for setting the namespace.  Returns the namespace passed in.
     *
     * @see #namespace
     */
    String namespace(String namespace) {
        this.namespace = namespace
        this.namespace
    }

    /**
     * DSL helper method for setting the name.  Returns the name passed in.
     *
     * @see #name
     */
    String name(String name) {
        this.name = name
        this.name
    }

    /**
     * DSL helper method for setting the version.  Returns the version passed in.
     *
     * @see #version
     */
    String version(String version) {
        this.version = version
        this.version
    }

    /**
     * DSL helper method for setting the export date.  Returns the date/time passed in.
     *
     * @see #exportDate
     */
    Instant exportDate(String exportDate) {
        this.exportDate = Instant.parse(exportDate)
        this.exportDate
    }

    /**
     * DSL helper method for setting the export date.  Returns the date/time passed in.
     *
     * @see #exportDate
     */
    Instant exportDate(Instant exportDate) {
        this.exportDate = exportDate
        this.exportDate
    }

    /**
     * DSL helper method for setting the exportedBy property.  Returns the string passed in.
     *
     * @see #exportedBy
     */
    String exportedBy(String exportedBy) {
        this.exportedBy = exportedBy
        this.exportedBy
    }

}

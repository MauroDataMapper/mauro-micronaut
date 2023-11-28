package uk.ac.ox.softeng.mauro.export

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

//    @JsonFormat(pattern = AdministeredItem.DATETIME_FORMAT)
    Instant exportDate

    String exportedBy
}

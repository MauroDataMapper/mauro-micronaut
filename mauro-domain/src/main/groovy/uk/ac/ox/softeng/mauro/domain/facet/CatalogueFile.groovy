package uk.ac.ox.softeng.mauro.domain.facet

import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.annotation.JsonProperty
import groovy.transform.AutoClone
import groovy.transform.CompileStatic

@CompileStatic
@AutoClone
abstract class CatalogueFile extends Facet implements CatalogueFileOutput{
    @JsonAlias(['file_contents'])
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    byte[] fileContents
    @JsonAlias(['file_name'])
    String fileName
    @JsonAlias(['file_size'])
    Long fileSize
    @JsonAlias(['file_type'])
    String fileType
}
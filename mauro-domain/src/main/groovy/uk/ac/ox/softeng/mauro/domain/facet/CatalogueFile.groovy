package uk.ac.ox.softeng.mauro.domain.facet

import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import groovy.transform.AutoClone
import groovy.transform.CompileStatic
import io.micronaut.core.annotation.NonNull

@CompileStatic
@AutoClone
abstract class CatalogueFile extends Facet implements CatalogueFileOutput{
    @NonNull
    @JsonAlias(['file_contents'])
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    byte[] fileContents

    @NonNull
    @JsonAlias(['file_name'])
    String fileName

    @NonNull
    @JsonAlias(['file_size'])
    Long fileSize

    @NonNull
    @JsonAlias(['file_type'])
    String fileType


}
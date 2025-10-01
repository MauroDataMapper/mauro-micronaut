package org.maurodata.domain.facet

import org.maurodata.domain.model.Item
import org.maurodata.domain.model.ItemUtils

import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import groovy.transform.AutoClone
import groovy.transform.CompileStatic
import io.micronaut.core.annotation.NonNull
import io.micronaut.data.annotation.Transient

@CompileStatic
@AutoClone
abstract class CatalogueFile extends Facet implements CatalogueFileOutput {
    @NonNull
    @JsonAlias(['file_contents'])
    //@JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
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

    @Transient
    @JsonIgnore
    @Override
    String getPathPrefix() {
        'cf'
    }

    //TODO: Not clean: fileName may contain characters used by Paths
    @Transient
    @JsonIgnore
    @Override
    String getPathIdentifier() {
        fileName
    }

    @Override
    void copyInto(Item into) {
        super.copyInto(into)
        CatalogueFile intoCatalogueFile = (CatalogueFile) into
        intoCatalogueFile.fileContents = ItemUtils.copyItem(this.fileContents, intoCatalogueFile.fileContents)
        intoCatalogueFile.fileName = ItemUtils.copyItem(this.fileName, intoCatalogueFile.fileName)
        intoCatalogueFile.fileSize = ItemUtils.copyItem(this.fileSize, intoCatalogueFile.fileSize)
        intoCatalogueFile.fileType = ItemUtils.copyItem(this.fileType, intoCatalogueFile.fileType)
    }
}
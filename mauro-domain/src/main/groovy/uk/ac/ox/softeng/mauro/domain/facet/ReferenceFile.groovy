package uk.ac.ox.softeng.mauro.domain.facet

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import groovy.transform.AutoClone
import groovy.transform.CompileStatic
import io.micronaut.data.annotation.Index
import io.micronaut.data.annotation.Indexes
import io.micronaut.data.annotation.MappedEntity
import jakarta.persistence.Transient

@CompileStatic
@MappedEntity(value = 'reference_file', schema = 'core', alias = 'reference_file_')
@AutoClone
@Indexes([@Index(columns = ['multi_facet_aware_item_id'])])
@JsonIgnoreProperties(ignoreUnknown = true)
class ReferenceFile extends CatalogueFile {

    @Override
    @JsonIgnore
    @Transient
    byte[] fileContent() {
        this.fileContents = Arrays.copyOf(fileContents, fileContents.size())
        fileContents
    }


}
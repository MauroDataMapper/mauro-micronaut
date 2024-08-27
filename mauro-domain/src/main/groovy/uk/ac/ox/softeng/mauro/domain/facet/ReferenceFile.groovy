package uk.ac.ox.softeng.mauro.domain.facet

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import groovy.transform.AutoClone
import groovy.transform.CompileStatic
import io.micronaut.data.annotation.Index
import io.micronaut.data.annotation.Indexes
import io.micronaut.data.annotation.MappedEntity
import jakarta.persistence.Transient
import uk.ac.ox.softeng.mauro.domain.diff.BaseCollectionDiff
import uk.ac.ox.softeng.mauro.domain.diff.CollectionDiff
import uk.ac.ox.softeng.mauro.domain.diff.DiffBuilder
import uk.ac.ox.softeng.mauro.domain.diff.DiffableItem
import uk.ac.ox.softeng.mauro.domain.diff.ObjectDiff

@CompileStatic
@MappedEntity(value = 'reference_file', schema = 'core', alias = 'reference_file_')
@AutoClone
@Indexes([@Index(columns = ['multi_facet_aware_item_id'])])
@JsonIgnoreProperties(ignoreUnknown = true)
class ReferenceFile extends CatalogueFile implements DiffableItem<ReferenceFile> {

    void setFileSize() {
        this.fileSize = fileContents.size()
    }

    @Override
    @JsonIgnore
    @Transient
    byte[] fileContent() {
        this.fileContents = Arrays.copyOf(fileContents, fileContents.size())
        fileContents
    }


    @Override
    @Transient
    @JsonIgnore
    CollectionDiff fromItem() {
        new BaseCollectionDiff(id)
    }

    @Override
    @Transient
    @JsonIgnore
    String getDiffIdentifier() {
        fileName
    }

    @Override
    @Transient
    @JsonIgnore
    ObjectDiff<ReferenceFile> diff(ReferenceFile other) {
        ObjectDiff<ReferenceFile> base = DiffBuilder.objectDiff(ReferenceFile)
                .leftHandSide(id?.toString(), this)
                .rightHandSide(other.id?.toString(), other)
        base.appendString(DiffBuilder.FILE_NAME, this.fileName, other.fileName)
        base
    }
}
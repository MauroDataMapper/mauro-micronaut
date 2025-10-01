package org.maurodata.domain.facet

import org.maurodata.domain.model.Item

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import groovy.transform.AutoClone
import groovy.transform.CompileStatic
import io.micronaut.data.annotation.Index
import io.micronaut.data.annotation.Indexes
import io.micronaut.data.annotation.MappedEntity
import jakarta.persistence.Transient
import org.maurodata.domain.diff.BaseCollectionDiff
import org.maurodata.domain.diff.CollectionDiff
import org.maurodata.domain.diff.DiffBuilder
import org.maurodata.domain.diff.DiffableItem
import org.maurodata.domain.diff.ObjectDiff

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
        new BaseCollectionDiff(id, getDiffIdentifier(), null)
    }

    @Override
    @Transient
    @JsonIgnore
    String getDiffIdentifier() {
        "${pathPrefix}:${fileName}"
    }

    @Override
    @Transient
    @JsonIgnore
    ObjectDiff<ReferenceFile> diff(ReferenceFile other, String lhsPathRoot, String rhsPathRoot) {
        ObjectDiff<ReferenceFile> base = DiffBuilder.objectDiff(ReferenceFile)
            .leftHandSide(id?.toString(), this)
            .rightHandSide(other.id?.toString(), other)
        base.appendString(DiffBuilder.FILE_NAME, this.fileName, other.fileName, this, other)
        base
    }

    @Transient
    @JsonIgnore
    ObjectDiff<ReferenceFile> diff(ReferenceFile other) {
        diff(other, null, null)
    }

    @Transient
    @JsonIgnore
    @Override
    String getPathPrefix() {
        'rf'
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
    }

    @Override
    Item shallowCopy() {
        ReferenceFile referenceFileShallowCopy = new ReferenceFile()
        this.copyInto(referenceFileShallowCopy)
        return referenceFileShallowCopy
    }
}
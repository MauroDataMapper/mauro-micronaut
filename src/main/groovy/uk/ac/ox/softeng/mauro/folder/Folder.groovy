package uk.ac.ox.softeng.mauro.folder

import uk.ac.ox.softeng.mauro.model.Model

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.Introspected
import io.micronaut.core.annotation.Nullable
import io.micronaut.data.annotation.MappedEntity
import io.micronaut.data.annotation.Relation
import jakarta.persistence.Transient

@CompileStatic
@Introspected
@MappedEntity
class Folder extends Model {

    @Nullable
    Folder parentFolder

    @Transient
    String aliasesString

    @Transient
    String modelType = domainType

    @Transient
    UUID breadcrumbTreeId

    @Transient
    String organisation

    @Transient
    @Override
    Folder getFolder() {
        parentFolder
    }

    @Transient
    @Override
    void setFolder(Folder folder) {
        parentFolder = folder
    }

    @Relation(value = Relation.Kind.ONE_TO_MANY, mappedBy = 'parentFolder')
    List<Folder> childFolders
}

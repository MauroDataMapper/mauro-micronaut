package uk.ac.ox.softeng.mauro.folder

import uk.ac.ox.softeng.mauro.model.Model

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.Introspected
import io.micronaut.core.annotation.Nullable
import io.micronaut.data.annotation.MappedEntity
import io.micronaut.data.annotation.Relation

@CompileStatic
@Introspected
@MappedEntity
class Folder extends Model {

//    String modelType = Folder.simpleName

    @Nullable
    Folder parentFolder

    @Relation(value = Relation.Kind.ONE_TO_MANY, mappedBy = 'parentFolder')
    List<Folder> childFolders
}

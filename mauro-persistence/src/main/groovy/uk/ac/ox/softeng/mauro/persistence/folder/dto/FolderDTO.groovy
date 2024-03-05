package uk.ac.ox.softeng.mauro.persistence.folder.dto

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.Introspected
import io.micronaut.core.annotation.Nullable
import io.micronaut.data.annotation.MappedEntity
import io.micronaut.data.annotation.MappedProperty
import io.micronaut.data.annotation.TypeDef
import io.micronaut.data.annotation.sql.ColumnTransformer
import io.micronaut.data.model.DataType
import uk.ac.ox.softeng.mauro.domain.facet.Metadata
import uk.ac.ox.softeng.mauro.domain.folder.Folder
import uk.ac.ox.softeng.mauro.persistence.model.dto.AdministeredItemDTO

@CompileStatic
@Introspected
@MappedEntity(value = 'folder', schema = 'core', alias = 'folder_')
class FolderDTO extends Folder implements AdministeredItemDTO {

    @Nullable
    @TypeDef(type = DataType.JSON)
    @MappedProperty
    @ColumnTransformer(read = '(select json_agg(metadata) from core.metadata where multi_facet_aware_item_id = folder_.id)')
    List<Metadata> metadata = []
}

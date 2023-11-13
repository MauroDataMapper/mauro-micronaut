package uk.ac.ox.softeng.mauro.persistence.datamodel

import uk.ac.ox.softeng.mauro.domain.datamodel.DataModel
import uk.ac.ox.softeng.mauro.domain.folder.Folder
import uk.ac.ox.softeng.mauro.domain.model.AdministeredItem
import uk.ac.ox.softeng.mauro.persistence.model.ModelRepository

import groovy.transform.CompileStatic
import io.micronaut.data.annotation.Join
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.r2dbc.annotation.R2dbcRepository
import io.micronaut.data.repository.reactive.ReactorPageableRepository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@CompileStatic
@R2dbcRepository(dialect = Dialect.POSTGRES)
abstract class DataModelRepository implements ReactorPageableRepository<DataModel, UUID>, ModelRepository<DataModel> {

    /*
    private static final String FIND_QUERY_SQL = '''SELECT data_model_."id",
        data_model_."version",
        data_model_."date_created",
        data_model_."last_updated",
        data_model_."created_by",
        data_model_."path",
        data_model_."aliases_string",
        data_model_."breadcrumb_tree_id",
        data_model_."finalised",
        data_model_."date_finalised",
        data_model_."documentation_version",
        data_model_."readable_by_everyone",
        data_model_."readable_by_authenticated_users",
        data_model_."model_type",
        data_model_."organisation",
        data_model_."deleted",
        data_model_."author",
        data_model_."folder_id",
        data_model_."authority_id",
        data_model_."branch_name",
        data_model_."model_version",
        data_model_."model_version_tag",
        data_model_."label",
        data_model_."description",
        data_model_data_types_."id"                                    AS data_types_id,
        data_model_data_types_."version"                               AS data_types_version,
        data_model_data_types_."idx"                                   AS data_types_idx,
        data_model_data_types_."date_created"                          AS data_types_date_created,
        data_model_data_types_."last_updated"                          AS data_types_last_updated,
        data_model_data_types_."label"                                 AS data_types_label,
        data_model_data_types_."description"                           AS data_types_description,
        data_model_data_types_."path"                                  AS data_types_path,
        data_model_data_types_."breadcrumb_tree_id"                    AS data_types_breadcrumb_tree_id,
        data_model_data_types_."created_by"                            AS data_types_created_by,
        data_model_data_types_."aliases_string"                        AS data_types_aliases_string,
        data_model_data_types_."data_model_id"                         AS data_types_data_model_id,
        FROM "data_model" data_model_
    LEFT JOIN "data_type" data_model_data_types_ ON datamodel_."id" = data_model_data_types_."data_model_id"
    WHERE (data_model_."id" = :id)'''
*/
    //    @Query('''SELECT terminology.*, term.*, term_relationship.*, term_relationship_type.*
    //FROM terminology
    //LEFT JOIN term ON terminology.id = term.terminology_id
    //LEFT JOIN term_relationship ON term.id IN (source_term_id, target_term_id)
    //LEFT JOIN term_relationship_type ON terminology.id = term_relationship_type.terminology_id''')
    //        terminology_term_relationships_."terminology_id"           AS term_relationships_terminology_id,
//    @Query(FIND_QUERY_SQL)

    @Join(value = 'dataTypes', type = Join.Type.LEFT_FETCH)
    abstract Mono<DataModel> findById(UUID id)

    @Join(value = 'dataTypes', type = Join.Type.LEFT_FETCH)
    abstract Mono<DataModel> findByFolderIdAndId(UUID folderId, UUID id)

    abstract Mono<DataModel> readById(UUID id)

    abstract Mono<DataModel> readByFolderIdAndId(UUID folderId, UUID id)

    abstract Flux<DataModel> readAllByFolder(Folder folder)

    @Override
    Mono<DataModel> readByParentIdAndId(UUID parentId, UUID id) {
        readByFolderIdAndId(parentId, id)
    }

    @Override
    Mono<DataModel> findByParentIdAndId(UUID parentId, UUID id) {
        findByFolderIdAndId(parentId, id)
    }

    @Override
    Flux<DataModel> readAllByParent(AdministeredItem parent) {
        readAllByFolder((Folder) parent)
    }

    @Override
    Boolean handles(Class clazz) {
        clazz == DataModel
    }

    @Override
    Boolean handles(String domainType) {
        domainType.toLowerCase() in ['datamodel', 'datamodels']
    }
}

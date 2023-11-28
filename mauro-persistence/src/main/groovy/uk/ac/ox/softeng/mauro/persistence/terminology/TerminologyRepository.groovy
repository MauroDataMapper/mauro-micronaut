package uk.ac.ox.softeng.mauro.persistence.terminology

import uk.ac.ox.softeng.mauro.domain.folder.Folder
import uk.ac.ox.softeng.mauro.domain.model.AdministeredItem

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.NonNull
import io.micronaut.data.annotation.Join
import io.micronaut.data.annotation.Query
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.r2dbc.annotation.R2dbcRepository
import io.micronaut.data.repository.reactive.ReactorPageableRepository
import jakarta.validation.Valid
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import uk.ac.ox.softeng.mauro.persistence.model.ModelRepository
import uk.ac.ox.softeng.mauro.domain.terminology.Terminology

@CompileStatic
@R2dbcRepository(dialect = Dialect.POSTGRES)
abstract class TerminologyRepository implements ReactorPageableRepository<Terminology, UUID>, ModelRepository<Terminology> {

    private static final String FIND_QUERY_SQL = '''SELECT terminology_."id",
        terminology_."version",
        terminology_."date_created",
        terminology_."last_updated",
        terminology_."created_by",
        terminology_."aliases_string",
        terminology_."breadcrumb_tree_id",
        terminology_."finalised",
        terminology_."date_finalised",
        terminology_."documentation_version",
        terminology_."readable_by_everyone",
        terminology_."readable_by_authenticated_users",
        terminology_."model_type",
        terminology_."organisation",
        terminology_."deleted",
        terminology_."author",
        terminology_."folder_id",
        terminology_."authority_id",
        terminology_."branch_name",
        terminology_."model_version",
        terminology_."model_version_tag",
        terminology_."label",
        terminology_."description",
        terminology_terms_."id"                                    AS terms_id,
        terminology_terms_."version"                               AS terms_version,
        terminology_terms_."idx"                                   AS terms_idx,
        terminology_terms_."date_created"                          AS terms_date_created,
        terminology_terms_."last_updated"                          AS terms_last_updated,
        terminology_terms_."label"                                 AS terms_label,
        terminology_terms_."description"                           AS terms_description,
        terminology_terms_."breadcrumb_tree_id"                    AS terms_breadcrumb_tree_id,
        terminology_terms_."created_by"                            AS terms_created_by,
        terminology_terms_."aliases_string"                        AS terms_aliases_string,
        terminology_terms_."terminology_id"                        AS terms_terminology_id,
        terminology_terms_."code"                                  AS terms_code,
        terminology_terms_."definition"                            AS terms_definition,
        terminology_terms_."url"                                   AS terms_url,
        terminology_terms_."is_parent"                             AS terms_is_parent,
        terminology_terms_."depth"                                 AS terms_depth,
        terminology_term_relationships_."id"                       AS term_relationships_id,
        terminology_term_relationships_."version"                  AS term_relationships_version,
        terminology_term_relationships_."idx"                      AS term_relationships_idx,
        terminology_term_relationships_."date_created"             AS term_relationships_date_created,
        terminology_term_relationships_."last_updated"             AS term_relationships_last_updated,
        terminology_term_relationships_."label"                    AS term_relationships_label,
        terminology_term_relationships_."description"              AS term_relationships_description,
        terminology_term_relationships_."breadcrumb_tree_id"       AS term_relationships_breadcrumb_tree_id,
        terminology_term_relationships_."created_by"               AS term_relationships_created_by,
        terminology_term_relationships_."aliases_string"           AS term_relationships_aliases_string,
        terminology_term_relationships_."source_term_id"           AS term_relationships_source_term_id,
        terminology_term_relationships_."target_term_id"           AS term_relationships_target_term_id,
        terminology_term_relationships_."relationship_type_id"     AS term_relationships_relationship_type_id,
        terminology_term_relationship_types_."id"                  AS term_relationship_types_id,
        terminology_term_relationship_types_."version"             AS term_relationship_types_version,
        terminology_term_relationship_types_."idx"                 AS term_relationship_types_idx,
        terminology_term_relationship_types_."date_created"        AS term_relationship_types_date_created,
        terminology_term_relationship_types_."last_updated"        AS term_relationship_types_last_updated,
        terminology_term_relationship_types_."label"               AS term_relationship_types_label,
        terminology_term_relationship_types_."description"         AS term_relationship_types_description,
        terminology_term_relationship_types_."breadcrumb_tree_id"  AS term_relationship_types_breadcrumb_tree_id,
        terminology_term_relationship_types_."created_by"          AS term_relationship_types_created_by,
        terminology_term_relationship_types_."aliases_string"      AS term_relationship_types_aliases_string,
        terminology_term_relationship_types_."terminology_id"      AS term_relationship_types_terminology_id,
        terminology_term_relationship_types_."parental_relationship" AS term_relationship_types_parental_relationship,
        terminology_term_relationship_types_."child_relationship"  AS term_relationship_types_child_relationship
        FROM terminology."terminology" terminology_
    LEFT JOIN terminology."term" terminology_terms_ ON terminology_."id" = terminology_terms_."terminology_id"
    LEFT JOIN terminology."term_relationship" terminology_term_relationships_ ON terminology_terms_.id IN (terminology_term_relationships_.source_term_id,
    terminology_term_relationships_.target_term_id)
    LEFT JOIN terminology."term_relationship_type" terminology_term_relationship_types_ ON terminology_."id" = terminology_term_relationship_types_."terminology_id"
    WHERE (terminology_."id" = :id)'''
/*

        terminology_authority_."id" AS authority_id,
        terminology_authority_."version" AS authority_version,
        terminology_authority_."date_created" AS authority_date_created,
        terminology_authority_."last_updated" AS authority_last_updated,
        terminology_authority_."readable_by_everyone" AS authority_readable_by_everyone,
        terminology_authority_."readable_by_authenticated_users" AS authority_readable_by_authenticated_users,
        terminology_authority_."label" AS authority_label,
        terminology_authority_."aliases_string" AS authority_aliases_string,
        terminology_authority_."created_by" AS authority_created_by,
        terminology_authority_."breadcrumb_tree_id" AS authority_breadcrumb_tree_id,
        terminology_authority_."url" AS authority_url

        LEFT JOIN core."authority" terminology_authority_ ON terminology_."authority_id" = terminology_authority_."id"

 */

    //    @Query('''SELECT terminology.*, term.*, term_relationship.*, term_relationship_type.*
    //FROM terminology
    //LEFT JOIN term ON terminology.id = term.terminology_id
    //LEFT JOIN term_relationship ON term.id IN (source_term_id, target_term_id)
    //LEFT JOIN term_relationship_type ON terminology.id = term_relationship_type.terminology_id''')
    //        terminology_term_relationships_."terminology_id"           AS term_relationships_terminology_id,
//    @Query(FIND_QUERY_SQL)
////    @Join(value = 'authority', type = Join.Type.LEFT_FETCH)
//    @Join(value = 'terms', type = Join.Type.LEFT_FETCH)
//    @Join(value = 'termRelationshipTypes', type = Join.Type.LEFT_FETCH)
//    @Join(value = 'termRelationships', type = Join.Type.LEFT_FETCH)
//    abstract Mono<Terminology> findById(UUID id)
//
//    Mono<Terminology> findById(UUID id) {
//        findTerminologyFacetDTOById(id) as Mono<Terminology>
//    }

    Mono<Terminology> findById(UUID id) {
        findTerminologyFacetDTOById(id) as Mono<Terminology>
    }

    @Query('''select *, (select json_agg(metadata) from core.metadata where multi_facet_aware_item_id = terminology.id) as metadata
        from terminology.terminology
        where terminology.id = :id''')
    abstract Mono<TerminologyFacetDTO> findTerminologyFacetDTOById(UUID id)

    @Query(FIND_QUERY_SQL)
//    @Join(value = 'authority', type = Join.Type.LEFT_FETCH)
    @Join(value = 'terms', type = Join.Type.LEFT_FETCH)
    @Join(value = 'termRelationshipTypes', type = Join.Type.LEFT_FETCH)
    @Join(value = 'termRelationships', type = Join.Type.LEFT_FETCH)
    abstract Mono<Terminology> findByFolderIdAndId(UUID folderId, UUID id)

    @Query('''select *, (select json_agg(metadata) from core.metadata where multi_facet_aware_item_id = terminology.id) as metadata
        from terminology.terminology
        where terminology.id = :id''')
    abstract Mono<Terminology> readById(UUID id)

    abstract Mono<Terminology> readByFolderIdAndId(UUID folderId, UUID id)

    abstract Flux<Terminology> readAllByFolder(Folder folder)

    @Override
    Mono<Terminology> readByParentIdAndId(UUID parentId, UUID id) {
        readByFolderIdAndId(parentId, id)
    }

    @Override
    Mono<Terminology> findByParentIdAndId(UUID parentId, UUID id) {
        findByFolderIdAndId(parentId, id)
    }

    @Override
    Flux<Terminology> readAllByParent(AdministeredItem parent) {
        readAllByFolder((Folder) parent)
    }

    @Override
    Boolean handles(Class clazz) {
        clazz == Terminology
    }

    @Override
    Boolean handles(String domainType) {
        domainType.toLowerCase() in ['terminology', 'terminologies']
    }
}

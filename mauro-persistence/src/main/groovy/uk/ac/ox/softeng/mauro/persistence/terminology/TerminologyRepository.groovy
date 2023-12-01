package uk.ac.ox.softeng.mauro.persistence.terminology


import uk.ac.ox.softeng.mauro.domain.folder.Folder
import uk.ac.ox.softeng.mauro.domain.model.AdministeredItem
import uk.ac.ox.softeng.mauro.persistence.terminology.dto.TerminologyDTORepository

import groovy.transform.CompileStatic
import io.micronaut.data.annotation.Join
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.r2dbc.annotation.R2dbcRepository
import io.micronaut.data.repository.reactive.ReactorPageableRepository
import jakarta.inject.Inject
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import uk.ac.ox.softeng.mauro.persistence.model.ModelRepository
import uk.ac.ox.softeng.mauro.domain.terminology.Terminology

@CompileStatic
@R2dbcRepository(dialect = Dialect.POSTGRES)
abstract class TerminologyRepository implements ReactorPageableRepository<Terminology, UUID>, ModelRepository<Terminology> {

    @Inject
    TerminologyDTORepository terminologyDTORepository

    static final String FIND_QUERY_SQL = '''select terminology_.*,
        (select json_agg(metadata) from core.metadata where multi_facet_aware_item_id = terminology_.id) as metadata,
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
        FROM terminology."terminology" terminology_
        LEFT JOIN core."authority" terminology_authority_ ON terminology_."authority_id" = terminology_authority_."id"
        WHERE (terminology_."id" = :id)
        '''

    Mono<Terminology> findById(UUID id) {
        terminologyDTORepository.findById(id) as Mono<Terminology>
    }

//    @Query(FIND_QUERY_SQL)
//    @Join(value = 'authority', type = Join.Type.LEFT_FETCH)
//    abstract Mono<TerminologyDTO> findTerminologyDTOById(UUID id)

    @Join(value = 'terms', type = Join.Type.LEFT_FETCH)
    @Join(value = 'termRelationshipTypes', type = Join.Type.LEFT_FETCH)
    @Join(value = 'termRelationships', type = Join.Type.LEFT_FETCH)
    abstract Mono<Terminology> findByFolderIdAndId(UUID folderId, UUID id)

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

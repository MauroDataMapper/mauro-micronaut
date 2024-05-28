package uk.ac.ox.softeng.mauro.persistence.search.dto

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.Nullable
import io.micronaut.data.annotation.Query
import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.repository.GenericRepository
import jakarta.inject.Singleton

import java.sql.Date
import java.time.LocalDate

@CompileStatic
@Singleton
@JdbcRepository(dialect = Dialect.POSTGRES)
abstract class SearchRepository implements GenericRepository<SearchResultsDTO, UUID> {

    @Query(value = '''
        select search_domains.id,
            search_domains.domain_type,
            search_domains.label,
            search_domains.description,
            search_domains.date_created,
            search_domains.last_updated,
            search_domains.ts,
            core.tsvector_agg(metadata.ts) as metadata_ts,
            ts_rank(tsvector_concat(core.tsvector_agg(metadata.ts), search_domains.ts), to_tsquery('english', :searchTerm), 1)
        from core.metadata
        right join core.search_domains on metadata.multi_facet_aware_item_id = search_domains.id

        where  (metadata.ts @@ to_tsquery('english', :searchTerm) or search_domains.ts @@ to_tsquery('english', :searchTerm)) 
                and ( (:domainTypes) is null or search_domains.domain_type in (:domainTypes)) 
                and (:modelId is null or search_domains.model_id = :modelId)
                and ( cast(:createdBefore as date) is null or :createdBefore > search_domains.date_created)
                and ( cast(:createdAfter as date) is null or :createdAfter <= search_domains.date_created)
                and ( cast(:lastUpdatedBefore as date) is null or :lastUpdatedBefore > search_domains.last_updated)
                and ( cast(:lastUpdatedAfter as date) is null or :lastUpdatedAfter <= search_domains.last_updated)
        group by search_domains.id, search_domains.domain_type, search_domains.label, 
                search_domains.description, search_domains.ts, search_domains.date_created, search_domains.last_updated

        order by ts_rank(tsvector_concat(core.tsvector_agg(metadata.ts), search_domains.ts), to_tsquery('english', :searchTerm), 1) desc, label asc''',
    nativeQuery = true)
    abstract List<SearchResultsDTO> search(String searchTerm, List<String> domainTypes = [], @Nullable UUID modelId = null, @Nullable Date createdBefore = null, @Nullable Date createdAfter = null, @Nullable Date lastUpdatedBefore = null, @Nullable Date lastUpdatedAfter = null)

    @Query(value = '''
        select search_domains.id,
            search_domains.domain_type,
            search_domains.label,
            search_domains.description,
            search_domains.date_created,
            search_domains.last_updated,
            0.0 as ts_rank 
        from core.search_domains

        where  search_domains.label ilike :searchTerm || '%'
                and ( (:domainTypes) is null or search_domains.domain_type in (:domainTypes)) 
                and (:modelId is null or search_domains.model_id = :modelId)
                and ( cast(:createdBefore as date) is null or :createdBefore > search_domains.date_created)
                and ( cast(:createdAfter as date) is null or :createdAfter <= search_domains.date_created)
                and ( cast(:lastUpdatedBefore as date) is null or :lastUpdatedBefore > search_domains.last_updated)
                and ( cast(:lastUpdatedAfter as date) is null or :lastUpdatedAfter <= search_domains.last_updated)
        group by search_domains.id, search_domains.domain_type, search_domains.label, 
                search_domains.description, search_domains.date_created, search_domains.last_updated

        order by search_domains.label asc''',
            nativeQuery = true)
    abstract List<SearchResultsDTO> prefixSearch(String searchTerm, List<String> domainTypes = [], @Nullable UUID modelId = null, @Nullable Date createdBefore = null, @Nullable Date createdAfter = null, @Nullable Date lastUpdatedBefore = null, @Nullable Date lastUpdatedAfter = null)

}

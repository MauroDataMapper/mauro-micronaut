package uk.ac.ox.softeng.mauro.persistence.search.dto

import groovy.transform.CompileStatic
import io.micronaut.data.annotation.Query
import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.repository.GenericRepository
import jakarta.inject.Singleton

@CompileStatic
@Singleton
@JdbcRepository(dialect = Dialect.POSTGRES)
abstract class SearchRepository implements GenericRepository<SearchDTO, UUID> {

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

        group by search_domains.id, search_domains.domain_type, search_domains.label, 
                search_domains.description, search_domains.ts, search_domains.date_created, search_domains.last_updated

        order by ts_rank(tsvector_concat(core.tsvector_agg(metadata.ts), search_domains.ts), to_tsquery('english', :searchTerm), 1) desc, label asc''',
    nativeQuery = true)
    abstract List<SearchDTO> search(String searchTerm, List<String> domainTypes = [])

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

        group by search_domains.id, search_domains.domain_type, search_domains.label, 
                search_domains.description, search_domains.date_created, search_domains.last_updated

        order by search_domains.label desc''',
            nativeQuery = true)
    abstract List<SearchDTO> prefixSearch(String searchTerm, List<String> domainTypes = [])

}

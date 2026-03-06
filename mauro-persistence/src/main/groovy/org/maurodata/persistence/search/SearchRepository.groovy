package org.maurodata.persistence.search

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.Nullable
import io.micronaut.data.annotation.Query
import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.repository.GenericRepository
import jakarta.inject.Singleton
import org.maurodata.domain.search.dto.SearchRequestDTO
import org.maurodata.domain.search.dto.SearchResultsDTO

import java.sql.Date

@CompileStatic
@Singleton
@JdbcRepository(dialect = Dialect.POSTGRES)
abstract class SearchRepository implements GenericRepository<SearchResultsDTO, UUID> {

    @Query(value = '''WITH q AS (
  SELECT websearch_to_tsquery('english', :searchTerm) AS query
)
SELECT
  sd.id, sd.domain_type, sd.label, sd.description,
  sd.date_created, sd.last_updated,
  ts_rank_cd(sd.combined_ts, q.query) AS ts_rank
FROM search.search_domains sd
CROSS JOIN q
WHERE sd.combined_ts @@ q.query
  AND ((:domainTypes) IS NULL OR sd.domain_type IN (:domainTypes))
  AND (:modelId IS NULL OR sd.model_id = CAST(:modelId AS uuid))
  AND (CAST(:createdBefore AS timestamptz) IS NULL OR CAST(:createdBefore AS timestamptz) > sd.date_created)
  AND (CAST(:createdAfter AS timestamptz) IS NULL OR CAST(:createdAfter AS timestamptz) <= sd.date_created)
  AND (CAST(:lastUpdatedBefore AS timestamptz) IS NULL OR CAST(:lastUpdatedBefore AS timestamptz) > sd.last_updated)
  AND (CAST(:lastUpdatedAfter AS timestamptz) IS NULL OR CAST(:lastUpdatedAfter AS timestamptz) <= sd.last_updated)
ORDER BY ts_rank DESC, label ASC''',
    nativeQuery = true)
    abstract List<SearchResultsDTO> search(String searchTerm, @Nullable List<String> domainTypes = [], @Nullable UUID modelId = null, @Nullable Date createdBefore = null, @Nullable Date createdAfter = null, @Nullable Date lastUpdatedBefore = null, @Nullable Date lastUpdatedAfter = null)

    @Query(value = '''
        select search_domains.id,
            search_domains.domain_type,
            search_domains.label,
            search_domains.description,
            search_domains.date_created,
            search_domains.last_updated,
            0.0 as ts_rank 
        from search.search_domains

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
    abstract List<SearchResultsDTO> prefixSearch(String searchTerm, @Nullable List<String> domainTypes = [], @Nullable UUID modelId = null, @Nullable Date createdBefore = null, @Nullable Date createdAfter = null, @Nullable Date lastUpdatedBefore = null, @Nullable Date lastUpdatedAfter = null)

    List<SearchResultsDTO> search(SearchRequestDTO searchRequestDTO) {
        if(searchRequestDTO.prefixSearch) {
            return prefixSearch(
                    searchRequestDTO.searchTerm,
                    searchRequestDTO.domainTypes,
                    searchRequestDTO.withinModelId,
                    searchRequestDTO.createdBefore,
                    searchRequestDTO.createdAfter,
                    searchRequestDTO.lastUpdatedBefore,
                    searchRequestDTO.lastUpdatedAfter
            )
        } else {
            return search(
                    searchRequestDTO.searchTerm,
                    searchRequestDTO.domainTypes,
                    searchRequestDTO.withinModelId,
                    searchRequestDTO.createdBefore,
                    searchRequestDTO.createdAfter,
                    searchRequestDTO.lastUpdatedBefore,
                    searchRequestDTO.lastUpdatedAfter
            )
        }

    }


}

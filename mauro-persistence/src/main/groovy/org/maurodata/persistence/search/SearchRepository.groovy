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
  ts_rank_cd(
    setweight(to_tsvector('english', COALESCE(sd.label, '')), 'A') ||
    setweight(to_tsvector('english', COALESCE(sd.description, '')), 'B') ||
    setweight(COALESCE(sd.metadata_ts, ''::tsvector), 'C'),
    q.query
  ) AS ts_rank
FROM search.search_domains sd
CROSS JOIN q
WHERE sd.combined_ts @@ q.query
  AND ((:domainTypes) IS NULL OR sd.domain_type IN (:domainTypes))
  AND (:modelId IS NULL OR sd.model_id IN (
      WITH RECURSIVE requested_scope(id) AS (
          SELECT CAST(:modelId AS uuid)
      ),
      scoped_folders(id) AS (
          SELECT folder.id
          FROM core.folder folder
               JOIN requested_scope scope ON scope.id = folder.id
          UNION ALL
          SELECT child.id
          FROM core.folder child
               JOIN scoped_folders parent ON child.parent_folder_id = parent.id
      ),
      scoped_model_ids(id) AS (
          SELECT scope.id
          FROM requested_scope scope
          WHERE EXISTS (SELECT 1 FROM datamodel.data_model data_model WHERE data_model.id = scope.id)
             OR EXISTS (SELECT 1 FROM terminology.terminology terminology WHERE terminology.id = scope.id)
             OR EXISTS (SELECT 1 FROM terminology.code_set code_set WHERE code_set.id = scope.id)
          UNION
          SELECT data_model.id
          FROM datamodel.data_model data_model
               JOIN scoped_folders folder ON folder.id = data_model.folder_id
          UNION
          SELECT terminology.id
          FROM terminology.terminology terminology
               JOIN scoped_folders folder ON folder.id = terminology.folder_id
          UNION
          SELECT code_set.id
          FROM terminology.code_set code_set
               JOIN scoped_folders folder ON folder.id = code_set.folder_id
      )
      SELECT id FROM scoped_model_ids
  ))
  AND (CAST(:createdBefore AS timestamptz) IS NULL OR CAST(:createdBefore AS timestamptz) > sd.date_created)
  AND (CAST(:createdAfter AS timestamptz) IS NULL OR CAST(:createdAfter AS timestamptz) <= sd.date_created)
  AND (CAST(:lastUpdatedBefore AS timestamptz) IS NULL OR CAST(:lastUpdatedBefore AS timestamptz) > sd.last_updated)
  AND (CAST(:lastUpdatedAfter AS timestamptz) IS NULL OR CAST(:lastUpdatedAfter AS timestamptz) <= sd.last_updated)
ORDER BY ts_rank DESC, label ASC''',
    nativeQuery = true)
    abstract List<SearchResultsDTO> search(String searchTerm, @Nullable List<String> domainTypes = [], @Nullable UUID modelId = null, @Nullable Date createdBefore = null, @Nullable Date createdAfter = null, @Nullable Date lastUpdatedBefore = null, @Nullable Date lastUpdatedAfter = null)

    @Query(value = '''WITH q AS (
  SELECT websearch_to_tsquery('english', :searchTerm) AS query
)
SELECT
  sd.id, sd.domain_type, sd.label, sd.description,
  sd.date_created, sd.last_updated,
  ts_rank_cd(
    setweight(to_tsvector('english', COALESCE(sd.label, '')), 'A') ||
    setweight(to_tsvector('english', COALESCE(sd.description, '')), 'B') ||
    setweight(COALESCE(sd.metadata_ts, ''::tsvector), 'C'),
    q.query
  ) AS ts_rank
FROM search.search_domains sd
CROSS JOIN q
WHERE sd.combined_ts @@ q.query
  AND ((:domainTypes) IS NULL OR sd.domain_type IN (:domainTypes))
  AND (:modelId IS NULL OR sd.model_id IN (
      WITH RECURSIVE requested_scope(id) AS (
          SELECT CAST(:modelId AS uuid)
      ),
      scoped_folders(id) AS (
          SELECT folder.id
          FROM core.folder folder
               JOIN requested_scope scope ON scope.id = folder.id
          UNION ALL
          SELECT child.id
          FROM core.folder child
               JOIN scoped_folders parent ON child.parent_folder_id = parent.id
      ),
      scoped_model_ids(id) AS (
          SELECT scope.id
          FROM requested_scope scope
          WHERE EXISTS (SELECT 1 FROM datamodel.data_model data_model WHERE data_model.id = scope.id)
             OR EXISTS (SELECT 1 FROM terminology.terminology terminology WHERE terminology.id = scope.id)
             OR EXISTS (SELECT 1 FROM terminology.code_set code_set WHERE code_set.id = scope.id)
          UNION
          SELECT data_model.id
          FROM datamodel.data_model data_model
               JOIN scoped_folders folder ON folder.id = data_model.folder_id
          UNION
          SELECT terminology.id
          FROM terminology.terminology terminology
               JOIN scoped_folders folder ON folder.id = terminology.folder_id
          UNION
          SELECT code_set.id
          FROM terminology.code_set code_set
               JOIN scoped_folders folder ON folder.id = code_set.folder_id
      )
      SELECT id FROM scoped_model_ids
  ))
  AND (CAST(:createdBefore AS timestamptz) IS NULL OR CAST(:createdBefore AS timestamptz) > sd.date_created)
  AND (CAST(:createdAfter AS timestamptz) IS NULL OR CAST(:createdAfter AS timestamptz) <= sd.date_created)
  AND (CAST(:lastUpdatedBefore AS timestamptz) IS NULL OR CAST(:lastUpdatedBefore AS timestamptz) > sd.last_updated)
  AND (CAST(:lastUpdatedAfter AS timestamptz) IS NULL OR CAST(:lastUpdatedAfter AS timestamptz) <= sd.last_updated)
ORDER BY ts_rank DESC, label ASC
LIMIT :limit''',
    nativeQuery = true)
    abstract List<SearchResultsDTO> searchLimited(String searchTerm, @Nullable List<String> domainTypes = [], @Nullable UUID modelId = null, @Nullable Date createdBefore = null, @Nullable Date createdAfter = null, @Nullable Date lastUpdatedBefore = null, @Nullable Date lastUpdatedAfter = null, Integer limit = 100)

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
                and (:modelId is null or search_domains.model_id in (
                    with recursive requested_scope(id) as (
                        select cast(:modelId as uuid)
                    ),
                    scoped_folders(id) as (
                        select folder.id
                        from core.folder folder
                             join requested_scope scope on scope.id = folder.id
                        union all
                        select child.id
                        from core.folder child
                             join scoped_folders parent on child.parent_folder_id = parent.id
                    ),
                    scoped_model_ids(id) as (
                        select scope.id
                        from requested_scope scope
                        where exists (select 1 from datamodel.data_model data_model where data_model.id = scope.id)
                           or exists (select 1 from terminology.terminology terminology where terminology.id = scope.id)
                           or exists (select 1 from terminology.code_set code_set where code_set.id = scope.id)
                        union
                        select data_model.id
                        from datamodel.data_model data_model
                             join scoped_folders folder on folder.id = data_model.folder_id
                        union
                        select terminology.id
                        from terminology.terminology terminology
                             join scoped_folders folder on folder.id = terminology.folder_id
                        union
                        select code_set.id
                        from terminology.code_set code_set
                             join scoped_folders folder on folder.id = code_set.folder_id
                    )
                    select id from scoped_model_ids
                ))
                and ( cast(:createdBefore as date) is null or :createdBefore > search_domains.date_created)
                and ( cast(:createdAfter as date) is null or :createdAfter <= search_domains.date_created)
                and ( cast(:lastUpdatedBefore as date) is null or :lastUpdatedBefore > search_domains.last_updated)
                and ( cast(:lastUpdatedAfter as date) is null or :lastUpdatedAfter <= search_domains.last_updated)
        group by search_domains.id, search_domains.domain_type, search_domains.label,
                search_domains.description, search_domains.date_created, search_domains.last_updated

        order by search_domains.label asc''',
            nativeQuery = true)
    abstract List<SearchResultsDTO> prefixSearch(String searchTerm, @Nullable List<String> domainTypes = [], @Nullable UUID modelId = null, @Nullable Date createdBefore = null, @Nullable Date createdAfter = null, @Nullable Date lastUpdatedBefore = null, @Nullable Date lastUpdatedAfter = null)

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
                and (:modelId is null or search_domains.model_id in (
                    with recursive requested_scope(id) as (
                        select cast(:modelId as uuid)
                    ),
                    scoped_folders(id) as (
                        select folder.id
                        from core.folder folder
                             join requested_scope scope on scope.id = folder.id
                        union all
                        select child.id
                        from core.folder child
                             join scoped_folders parent on child.parent_folder_id = parent.id
                    ),
                    scoped_model_ids(id) as (
                        select scope.id
                        from requested_scope scope
                        where exists (select 1 from datamodel.data_model data_model where data_model.id = scope.id)
                           or exists (select 1 from terminology.terminology terminology where terminology.id = scope.id)
                           or exists (select 1 from terminology.code_set code_set where code_set.id = scope.id)
                        union
                        select data_model.id
                        from datamodel.data_model data_model
                             join scoped_folders folder on folder.id = data_model.folder_id
                        union
                        select terminology.id
                        from terminology.terminology terminology
                             join scoped_folders folder on folder.id = terminology.folder_id
                        union
                        select code_set.id
                        from terminology.code_set code_set
                             join scoped_folders folder on folder.id = code_set.folder_id
                    )
                    select id from scoped_model_ids
                ))
                and ( cast(:createdBefore as date) is null or :createdBefore > search_domains.date_created)
                and ( cast(:createdAfter as date) is null or :createdAfter <= search_domains.date_created)
                and ( cast(:lastUpdatedBefore as date) is null or :lastUpdatedBefore > search_domains.last_updated)
                and ( cast(:lastUpdatedAfter as date) is null or :lastUpdatedAfter <= search_domains.last_updated)
        group by search_domains.id, search_domains.domain_type, search_domains.label, 
                search_domains.description, search_domains.date_created, search_domains.last_updated

        order by search_domains.label asc
        limit :limit''',
            nativeQuery = true)
    abstract List<SearchResultsDTO> prefixSearchLimited(String searchTerm, @Nullable List<String> domainTypes = [], @Nullable UUID modelId = null, @Nullable Date createdBefore = null, @Nullable Date createdAfter = null, @Nullable Date lastUpdatedBefore = null, @Nullable Date lastUpdatedAfter = null, Integer limit = 100)

    List<SearchResultsDTO> search(SearchRequestDTO searchRequestDTO) {
        search(searchRequestDTO, null)
    }

    List<SearchResultsDTO> search(SearchRequestDTO searchRequestDTO, @Nullable Integer limit) {
        boolean limited = limit != null && limit > 0
        if (searchRequestDTO.prefixSearch) {
            if (limited) {
                return prefixSearchLimited(
                    searchRequestDTO.searchTerm,
                    searchRequestDTO.domainTypes,
                    searchRequestDTO.withinModelId,
                    searchRequestDTO.createdBefore,
                    searchRequestDTO.createdAfter,
                    searchRequestDTO.lastUpdatedBefore,
                    searchRequestDTO.lastUpdatedAfter,
                    limit
                )
            }
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
            if (limited) {
                return searchLimited(
                    searchRequestDTO.searchTerm,
                    searchRequestDTO.domainTypes,
                    searchRequestDTO.withinModelId,
                    searchRequestDTO.createdBefore,
                    searchRequestDTO.createdAfter,
                    searchRequestDTO.lastUpdatedBefore,
                    searchRequestDTO.lastUpdatedAfter,
                    limit
                )
            }
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

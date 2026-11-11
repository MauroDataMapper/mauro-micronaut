package org.maurodata.persistence.overlay

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.Nullable
import io.micronaut.data.annotation.Repository
import io.micronaut.data.annotation.Query
import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import org.maurodata.domain.overlay.Overlay
import org.maurodata.persistence.ContentsService
import org.maurodata.persistence.model.ModelRepository
import org.maurodata.domain.model.AdministeredItem
import org.maurodata.web.ListResponse
import org.maurodata.web.PaginationParams

@CompileStatic
@Repository
@JdbcRepository(dialect = Dialect.POSTGRES)
abstract class OverlayRepository implements ModelRepository<Overlay> {
    OverlayRepository(ContentsService contentsService) { this.contentsService = contentsService }

    @Override Class getDomainClass() { Overlay }
    @Override Boolean handles(Class clazz) { domainClass.isAssignableFrom(clazz) }
    @Override Boolean handles(String domainType) { domainType != null && domainType.equalsIgnoreCase('overlay') }
    @Override Boolean handlesPathPrefix(String pathPrefix) { 'ov'.equalsIgnoreCase(pathPrefix) }
    @Override @Nullable abstract Overlay findById(UUID id)
    @Override @Nullable abstract List<Overlay> findAllByFolderId(UUID folderId)
    @Override @Nullable abstract List<Overlay> readAllByFolderIdIn(Collection<UUID> folderIds)

    @Override
    ListResponse<Overlay> readListResponseByParent(AdministeredItem parent, @Nullable PaginationParams params) {
        if (!(parent instanceof org.maurodata.domain.folder.Folder)) return ListResponse.from([], 0)
        UUID folderId = parent.id
        if (!params || params.max == null || params.max <= 0 || params.all?.equalsIgnoreCase('true')) {
            String label = params?.label ?: null
            String description = params?.description ?: null
            List<Overlay> all = params?.order?.equalsIgnoreCase('desc')
                ? readAllByFolderIdAndFiltersDesc(folderId, label, description)
                : readAllByFolderIdAndFiltersAsc(folderId, label, description)
            return ListResponse.from(all, all.size())
        }
        String label = params.label ?: null
        String description = params.description ?: null
        Long count = countByFolderIdAndFilters(folderId, label, description)
        Integer offset = Math.max(0, params.offset ?: 0)
        List<Overlay> page = params.order?.equalsIgnoreCase('desc')
            ? readPageByFolderIdAndFiltersDesc(folderId, label, description, params.max, offset)
            : readPageByFolderIdAndFiltersAsc(folderId, label, description, params.max, offset)
        ListResponse.from(page, count)
    }

    @Query('''select count(*) from core.overlay
              where folder_id = :folderId
                and (:label is null or lower(label) like concat('%', lower(:label), '%'))
                and (:description is null or lower(description) like concat('%', lower(:description), '%'))''')
    abstract Long countByFolderIdAndFilters(UUID folderId, @Nullable String label, @Nullable String description)

    @Query('''select * from core.overlay where folder_id = :folderId
              and (:label is null or lower(label) like concat('%', lower(:label), '%'))
              and (:description is null or lower(description) like concat('%', lower(:description), '%'))
              order by label asc''')
    abstract List<Overlay> readAllByFolderIdAndFiltersAsc(UUID folderId, @Nullable String label, @Nullable String description)

    @Query('''select * from core.overlay where folder_id = :folderId
              and (:label is null or lower(label) like concat('%', lower(:label), '%'))
              and (:description is null or lower(description) like concat('%', lower(:description), '%'))
              order by label desc''')
    abstract List<Overlay> readAllByFolderIdAndFiltersDesc(UUID folderId, @Nullable String label, @Nullable String description)

    @Query('''select * from core.overlay where folder_id = :folderId
              and (:label is null or lower(label) like concat('%', lower(:label), '%'))
              and (:description is null or lower(description) like concat('%', lower(:description), '%'))
              order by label asc limit :max offset :offset''')
    abstract List<Overlay> readPageByFolderIdAndFiltersAsc(UUID folderId, @Nullable String label,
                                                           @Nullable String description, Integer max, Integer offset)

    @Query('''select * from core.overlay where folder_id = :folderId
              and (:label is null or lower(label) like concat('%', lower(:label), '%'))
              and (:description is null or lower(description) like concat('%', lower(:description), '%'))
              order by label desc limit :max offset :offset''')
    abstract List<Overlay> readPageByFolderIdAndFiltersDesc(UUID folderId, @Nullable String label,
                                                            @Nullable String description, Integer max, Integer offset)
}

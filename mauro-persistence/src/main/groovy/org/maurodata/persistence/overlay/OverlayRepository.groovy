package org.maurodata.persistence.overlay

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.Nullable
import io.micronaut.data.annotation.Repository
import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import org.maurodata.domain.overlay.Overlay
import org.maurodata.persistence.ContentsService
import org.maurodata.persistence.model.ModelRepository

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
}

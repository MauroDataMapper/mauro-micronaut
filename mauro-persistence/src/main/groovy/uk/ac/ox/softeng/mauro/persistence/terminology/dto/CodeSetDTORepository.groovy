package uk.ac.ox.softeng.mauro.persistence.terminology.dto

import groovy.transform.CompileStatic
import io.micronaut.data.annotation.Join
import io.micronaut.data.annotation.Query
import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.repository.GenericRepository
import uk.ac.ox.softeng.mauro.domain.terminology.CodeSet

@CompileStatic
@JdbcRepository(dialect = Dialect.POSTGRES)
abstract class CodeSetDTORepository implements GenericRepository<CodeSetDTO, UUID> {

    @Join(value = 'authority', type = Join.Type.LEFT_FETCH)
    abstract CodeSetDTO findById(UUID id)

    @Query(''' select * from terminology.code_set cs where cs.folder_id = :folderId ''')
    abstract List<CodeSet> findAllByFolderId(UUID folderId)
}

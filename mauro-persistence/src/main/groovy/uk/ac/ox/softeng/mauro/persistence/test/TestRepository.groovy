package uk.ac.ox.softeng.mauro.persistence.test

import uk.ac.ox.softeng.mauro.domain.folder.Folder
import uk.ac.ox.softeng.mauro.domain.model.AdministeredItem
import uk.ac.ox.softeng.mauro.persistence.model.ModelRepository
import uk.ac.ox.softeng.mauro.test.Test

import groovy.transform.CompileStatic
import io.micronaut.data.annotation.Join
import io.micronaut.data.annotation.Query
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.r2dbc.annotation.R2dbcRepository
import io.micronaut.data.repository.reactive.ReactorPageableRepository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@CompileStatic
@R2dbcRepository(dialect = Dialect.POSTGRES)
abstract class TestRepository implements ReactorPageableRepository<Test, UUID>/*, ModelRepository<Test>*/ {

//    @Query('''select test.*,
//       (select array_agg(term) from terminology.term join test_term tt ON term.id = tt.term_id where tt.test_id = test.id) as terms
//       from test''')
//    @Join(value = 'terms')
    abstract Mono<Test> readById(UUID id)

//    @Query('''select *, (select json_agg(term) from terminology.term join test_term tt ON term.id = tt.term_id where tt.test_id = test.id) as terms
//        from test
//        where test.id = :id''')
//    abstract Mono<TestDTO> findById(UUID id)

    @Query('''select *, (select json_agg(term) from terminology.term join test_term tt ON term.id = tt.term_id where tt.test_id = test.id) as terms
        from test
        where test.id = :id''')
    abstract Mono<TestDTO> findTestDTOById(UUID id)

//    abstract Mono<Test> findByFolderIdAndId(UUID folderId, UUID id)
//
//    abstract Mono<Test> readByFolderIdAndId(UUID folderId, UUID id)
//
//    Mono<Test> findByParentIdAndId(UUID parentId, UUID id) {
//        findByFolderIdAndId(parentId, id)
//    }
//
//    Mono<Test> readByParentIdAndId(UUID parentId, UUID id) {
//        readByFolderIdAndId(parentId, id)
//    }
//
//    Flux<Test> readAllByParent(AdministeredItem parent) {
//        readAllByFolder((Folder) parent)
//    }

//    @Override
//    Boolean handles(Class clazz) {
//        clazz == Test
//    }
//
//    @Override
//    Boolean handles(String domainType) {
//        domainType.toLowerCase() in ['test', 'tests']
//    }

}

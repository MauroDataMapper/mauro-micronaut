//package uk.ac.ox.softeng.mauro.folder
//
//
//import io.micronaut.data.model.query.builder.sql.Dialect
//import io.micronaut.data.r2dbc.annotation.R2dbcRepository
//import io.micronaut.data.repository.reactive.ReactorPageableRepository
//
//@R2dbcRepository(dialect = Dialect.POSTGRES)
//abstract class FolderRepository implements ReactorPageableRepository<Folder, UUID> {
//
////    @Join(value = 'childFolders', type = Join.Type.LEFT_FETCH)
////    abstract Mono<Folder> findById(UUID id)
////
////    abstract Mono<Folder> readById(UUID id)
//
////    @Override
////    Boolean handles(Class clazz) {
////        clazz == Folder
////    }
////
////    @Override
////    Boolean handles(String domainType) {
////        domainType.toLowerCase() in ['folder', 'folders']
////    }
//}
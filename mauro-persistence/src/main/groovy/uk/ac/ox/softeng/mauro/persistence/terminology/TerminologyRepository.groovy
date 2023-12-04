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

    Mono<Terminology> findById(UUID id) {
        terminologyDTORepository.findById(id) as Mono<Terminology>
    }

    @Override
    Boolean handles(Class clazz) {
        clazz == Terminology
    }
}

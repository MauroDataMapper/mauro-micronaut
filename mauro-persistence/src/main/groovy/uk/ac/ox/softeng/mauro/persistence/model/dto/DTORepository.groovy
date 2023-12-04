package uk.ac.ox.softeng.mauro.persistence.model.dto

import uk.ac.ox.softeng.mauro.domain.model.Item

import io.micronaut.data.annotation.Join
import io.micronaut.data.repository.GenericRepository
import reactor.core.publisher.Mono

abstract class DTORepository<DTO extends Item> implements GenericRepository<DTO, UUID> {

    // Should be overriden if additional joins needed
    @Join(value = 'authority', type = Join.Type.LEFT_FETCH)
    abstract Mono<DTO> findById(UUID)
}

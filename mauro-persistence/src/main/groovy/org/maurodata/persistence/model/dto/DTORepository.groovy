package org.maurodata.persistence.model.dto

import io.micronaut.data.annotation.Join
import io.micronaut.data.repository.GenericRepository
import org.maurodata.domain.model.Item

abstract class DTORepository<DTO extends Item> implements GenericRepository<DTO, UUID> {

    // Should be overriden if additional joins needed
    @Join(value = 'authority', type = Join.Type.LEFT_FETCH)
    abstract DTO findById(UUID)
}

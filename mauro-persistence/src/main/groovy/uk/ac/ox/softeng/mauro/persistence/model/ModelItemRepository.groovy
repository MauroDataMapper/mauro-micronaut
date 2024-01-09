package uk.ac.ox.softeng.mauro.persistence.model

import uk.ac.ox.softeng.mauro.domain.model.AdministeredItem
import uk.ac.ox.softeng.mauro.domain.model.ModelItem

import groovy.transform.CompileStatic
import io.micronaut.data.repository.reactive.ReactorPageableRepository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@CompileStatic
trait ModelItemRepository<I extends ModelItem> implements AdministeredItemRepository<I> {

}
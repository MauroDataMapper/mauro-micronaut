package uk.ac.ox.softeng.mauro.persistence.model

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.Nullable
import uk.ac.ox.softeng.mauro.domain.model.AdministeredItem

@CompileStatic
trait AdministeredItemRepository<I extends AdministeredItem> implements ItemRepository<I> {

    @Nullable
    List<I> findAllByParent(AdministeredItem item) {
        // Should be implemented by override with joins, possibly using a DTO
        throw new UnsupportedOperationException('Method should be implemented')
    }

    @Nullable
    abstract List<I> readAllByParent(AdministeredItem item)
}
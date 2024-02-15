package uk.ac.ox.softeng.mauro.persistence.model

import groovy.transform.CompileStatic
import uk.ac.ox.softeng.mauro.domain.model.AdministeredItem

@CompileStatic
trait AdministeredItemRepository<I extends AdministeredItem> implements ItemRepository<I> {

    List<I> findAllByParent(AdministeredItem item) {
        // Should be implemented by override with joins, possibly using a DTO
        throw new UnsupportedOperationException('Method should be implemented')
    }

    abstract List<I> readAllByParent(AdministeredItem item)
}
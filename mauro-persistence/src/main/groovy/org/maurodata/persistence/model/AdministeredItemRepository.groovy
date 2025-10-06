package org.maurodata.persistence.model

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.Nullable
import org.maurodata.domain.model.AdministeredItem

@CompileStatic
trait AdministeredItemRepository<I extends AdministeredItem> implements ItemRepository<I> {

    @Nullable
    List<I> findAllByParent(AdministeredItem item) {
        // Should be implemented by override with joins, possibly using a DTO
        throw new UnsupportedOperationException('Method should be implemented')
    }

    @Nullable
    abstract List<I> readAllByParent(AdministeredItem item)

    @Nullable
    List<I> findAllByParentAndPathIdentifier(UUID item, String pathIdentifier){
        throw new UnsupportedOperationException('Method should be implemented')
    }


    abstract <I extends AdministeredItem> List<I> findAllByLabel(String label)
}
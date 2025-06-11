package org.maurodata.persistence.model

import groovy.transform.CompileStatic
import org.maurodata.domain.model.ModelItem

@CompileStatic
trait ModelItemRepository<I extends ModelItem> implements AdministeredItemRepository<I> {

}
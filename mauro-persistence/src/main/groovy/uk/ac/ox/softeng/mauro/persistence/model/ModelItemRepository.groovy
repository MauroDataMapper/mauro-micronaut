package uk.ac.ox.softeng.mauro.persistence.model

import groovy.transform.CompileStatic
import uk.ac.ox.softeng.mauro.domain.model.AdministeredItem
import uk.ac.ox.softeng.mauro.domain.model.ModelItem

@CompileStatic
trait ModelItemRepository<I extends ModelItem> implements AdministeredItemRepository<I> {

    abstract I findWithContentById(UUID id, AdministeredItem parent)
}
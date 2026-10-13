package org.maurodata.visitor

import groovy.transform.CompileStatic
import org.maurodata.domain.model.Item

/**
 * A closure-free handler that is invoked for every visited item and does its own type dispatch.
 * Registered via {@link VisitorRegistry#addHandler(ItemHandler)}; unlike a visitor method override it survives
 * composition of visitors with {@code +}.
 */
@CompileStatic
interface ItemHandler {

    void enter(Item item)

    void leave(Item item)
}

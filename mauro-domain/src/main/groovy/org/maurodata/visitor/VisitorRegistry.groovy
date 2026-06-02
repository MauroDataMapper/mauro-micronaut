package org.maurodata.visitor

import groovy.transform.CompileStatic
import org.maurodata.domain.model.Item

/**
 * Type-to-handler registry used by traversal visitors.
 */
@CompileStatic
class VisitorRegistry {

    Map<Class<? extends Item>, List<Closure<?>>> handlersByType = [:]

    <T extends Item> VisitorRegistry on(Class<T> type, Closure<?> handler) {
        if (!type || !handler) {
            return this
        }
        List<Closure<?>> handlers = handlersByType.computeIfAbsent(type) {[] as List<Closure<?>>}
        handlers.add(handler)
        return this
    }


    void apply(Item item) {
        if (!item) {
            return
        }
        handlersByType.each {Class<? extends Item> type, List<Closure<?>> handlers ->
            if (type.isInstance(item)) {
                handlers.each {Closure<?> handler ->
                    handler.call(item)
                }
            }
        }
    }

    VisitorRegistry addAll(VisitorRegistry other) {
        if (!other) {
            return this
        }

        other.handlersByType.each {Class<? extends Item> type, List<Closure<?>> handlers ->
            List<Closure<?>> currentHandlers = handlersByType.computeIfAbsent(type) {[] as List<Closure<?>>}
            currentHandlers.addAll(handlers)
        }
        return this
    }

    VisitorRegistry plus(VisitorRegistry other) {
        VisitorRegistry merged = new VisitorRegistry()
        merged.addAll(this)
        merged.addAll(other)
        return merged
    }
}



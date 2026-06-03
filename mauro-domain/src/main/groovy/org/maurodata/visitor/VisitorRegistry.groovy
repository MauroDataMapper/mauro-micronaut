package org.maurodata.visitor

import groovy.transform.CompileStatic
import org.maurodata.domain.model.Item

/**
 * Type-to-handler registry used by traversal visitors.
 */
@CompileStatic
class VisitorRegistry {

    Map<Class<? extends Item>, List<Closure<?>>> enterHandlersByType = [:]
    Map<Class<? extends Item>, List<Closure<?>>> leaveHandlersByType = [:]

    void applyEnter(Item item) {
        if (!item) {
            return
        }
        enterHandlersByType.each {Class<? extends Item> type, List<Closure<?>> handlers ->
            if (type.isInstance(item)) {
                handlers.each {Closure<?> handler ->
                    handler.call(item)
                }
            }
        }
    }

    void applyLeave(Item item) {
        if (!item) {
            return
        }
        leaveHandlersByType.each {Class<? extends Item> type, List<Closure<?>> handlers ->
            if (type.isInstance(item)) {
                handlers.each {Closure<?> handler ->
                    handler.call(item)
                }
            }
        }
    }


    <T extends Item> VisitorRegistry onEnter(Class<T> type, Closure<?> handler) {
        if (!type || !handler) {
            return this
        }
        List<Closure<?>> handlers = enterHandlersByType.getOrDefault(type, [] as List<Closure<?>>)
        handlers.add(handler)
        return this
    }

    <T extends Item> VisitorRegistry onLeave(Class<T> type, Closure<?> handler) {
        if (!type || !handler) {
            return this
        }
        List<Closure<?>> handlers = leaveHandlersByType.getOrDefault(type, [] as List<Closure<?>>)
        handlers.add(handler)
        return this
    }

    VisitorRegistry addAll(VisitorRegistry other) {
        if (!other) {
            return this
        }

        other.enterHandlersByType.each {Class<? extends Item> type, List<Closure<?>> handlers ->
            List<Closure<?>> currentHandlers = enterHandlersByType.getOrDefault(type, [] as List<Closure<?>>)
            currentHandlers.addAll(handlers)
        }
        other.leaveHandlersByType.each {Class<? extends Item> type, List<Closure<?>> handlers ->
            List<Closure<?>> currentHandlers = leaveHandlersByType.getOrDefault(type, [] as List<Closure<?>>)
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



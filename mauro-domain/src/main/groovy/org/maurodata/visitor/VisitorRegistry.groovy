package org.maurodata.visitor

import groovy.transform.CompileStatic
import org.maurodata.domain.model.Item

import java.util.concurrent.ConcurrentHashMap

/**
 * Type-to-handler registry used by traversal visitors.
 */
@CompileStatic
class VisitorRegistry {

    Map<Class<? extends Item>, List<Closure<?>>> enterHandlersByType = [:]
    Map<Class<? extends Item>, List<Closure<?>>> leaveHandlersByType = [:]

    private final List<ItemHandler> handlers = []
    private final Map<Class<?>, List<Closure<?>>> enterCache = new ConcurrentHashMap<>()
    private final Map<Class<?>, List<Closure<?>>> leaveCache = new ConcurrentHashMap<>()

    private static List<Closure<?>> matching(Map<Class<? extends Item>, List<Closure<?>>> handlersByType, Class<?> itemClass) {
        List<Closure<?>> result = []
        for (Map.Entry<Class<? extends Item>, List<Closure<?>>> entry : handlersByType.entrySet()) {
            if (entry.key.isAssignableFrom(itemClass)) {
                result.addAll(entry.value)
            }
        }
        result
    }

    void applyEnter(Item item) {
        if (item == null) {
            return
        }
        List<Closure<?>> closures = enterCache.computeIfAbsent(item.getClass()) { Class<?> c -> matching(enterHandlersByType, c) }
        for (int i = 0; i < closures.size(); i++) {
            closures.get(i).call(item)
        }
        for (int i = 0; i < handlers.size(); i++) {
            handlers.get(i).enter(item)
        }
    }

    void applyLeave(Item item) {
        if (item == null) {
            return
        }
        List<Closure<?>> closures = leaveCache.computeIfAbsent(item.getClass()) { Class<?> c -> matching(leaveHandlersByType, c) }
        for (int i = 0; i < closures.size(); i++) {
            closures.get(i).call(item)
        }
        for (int i = 0; i < handlers.size(); i++) {
            handlers.get(i).leave(item)
        }
    }

    <T extends Item> VisitorRegistry onEnter(Class<T> type, Closure<?> handler) {
        if (type == null || handler == null) {
            return this
        }
        List<Closure<?>> handlers = enterHandlersByType.computeIfAbsent(type) { [] as List<Closure<?>> }
        handlers.add(handler)
        clearCaches()
        return this
    }

    <T extends Item> VisitorRegistry onLeave(Class<T> type, Closure<?> handler) {
        if (type == null || handler == null) {
            return this
        }
        List<Closure<?>> handlers = leaveHandlersByType.computeIfAbsent(type) { [] as List<Closure<?>> }
        handlers.add(handler)
        clearCaches()
        return this
    }

    VisitorRegistry addHandler(ItemHandler handler) {
        if (handler != null && !handlers.contains(handler)) {
            handlers.add(handler)
        }
        return this
    }

    private void clearCaches() {
        enterCache.clear()
        leaveCache.clear()
    }

    VisitorRegistry addAll(VisitorRegistry other) {
        if (other == null) {
            return this
        }
        clearCaches()
        for (ItemHandler h : other.handlers) {
            addHandler(h)
        }

        other.enterHandlersByType.each {Class<? extends Item> type, List<Closure<?>> handlers ->
            List<Closure<?>> currentHandlers = enterHandlersByType.computeIfAbsent(type) { [] as List<Closure<?>> }
            currentHandlers.addAll(handlers)
        }
        other.leaveHandlersByType.each {Class<? extends Item> type, List<Closure<?>> handlers ->
            List<Closure<?>> currentHandlers = leaveHandlersByType.computeIfAbsent(type) { [] as List<Closure<?>> }
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


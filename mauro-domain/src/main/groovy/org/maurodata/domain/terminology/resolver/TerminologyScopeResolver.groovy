package org.maurodata.domain.terminology.resolver

import com.fasterxml.jackson.annotation.ObjectIdGenerator
import com.fasterxml.jackson.annotation.ObjectIdResolver
import org.maurodata.domain.terminology.Term

class TerminologyScopeResolver implements ObjectIdResolver {

    private static final Map<Object, Map<ObjectIdGenerator.IdKey, Object>> terminologyScopes = new WeakHashMap<>()

    private Object parent // the parent for this resolver

    TerminologyScopeResolver() {
    }

    TerminologyScopeResolver(Object parent) {
        System.err.println("Creating TerminologyScopeResolver for parent: ${parent}")
        this.parent = parent
        terminologyScopes.computeIfAbsent(parent, k -> new IdentityHashMap<>())
    }

    @Override
    void bindItem(ObjectIdGenerator.IdKey id, Object pojo) {
        System.err.println("Binding id ${id} in TerminologyScopeResolver for parent: ${parent}")
        terminologyScopes.get(parent).put(id, pojo)
    }

    @Override
    Object resolveId(ObjectIdGenerator.IdKey id) {
        System.err.println("Resolving id ${id} in TerminologyScopeResolver for parent: ${parent}")
        Object existing = terminologyScopes.get(parent).get(id)
        if(existing) {
            return existing
        } else if(id.scope == Term) {
            Term placeholder = new Term(code: id.key.toString())
            terminologyScopes.get(parent).put(id, placeholder)
            return placeholder
        }
        return null
    }

    @Override
    ObjectIdResolver newForDeserialization(Object context) {
        // context is the parent terminology
        return new TerminologyScopeResolver(context)
    }

    @Override
    boolean canUseFor(ObjectIdResolver resolverType) {
        return resolverType.getClass() == getClass()
    }
}

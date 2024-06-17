package uk.ac.ox.softeng.mauro.domain.terminology

import com.fasterxml.jackson.annotation.ObjectIdGenerator
import com.fasterxml.jackson.databind.JsonMappingException

class TermJsonIdGenerator extends ObjectIdGenerator<String> {
    @Override
    Class<?> getScope() {
        null
    }

    @Override
    boolean canUseFor(ObjectIdGenerator<?> objectIdGenerator) {
        objectIdGenerator.class == this.class //&& objectIdGenerator.scope == this.scope
    }

    @Override
    ObjectIdGenerator<String> forScope(Class<?> aClass) {
        this
    }

    @Override
    ObjectIdGenerator<String> newForSerialization(Object o) {
        this
    }

    @Override
    IdKey key(Object o) {
        if (o !instanceof Term) throw new JsonMappingException('Only good for Terms!')
        if (!o.terminology.id) throw new JsonMappingException('Unique ID for Terminology is required!')

        new IdKey(this.getClass(), Term, "$o.terminology.id $o.code")
    }

    @Override
    String generateId(Object o) {
        o
    }
}

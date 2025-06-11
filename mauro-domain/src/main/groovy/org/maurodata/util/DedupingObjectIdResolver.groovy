package org.maurodata.util

import com.fasterxml.jackson.annotation.ObjectIdGenerator.IdKey
import com.fasterxml.jackson.annotation.ObjectIdResolver
import com.fasterxml.jackson.annotation.SimpleObjectIdResolver


class DedupingObjectIdResolver extends SimpleObjectIdResolver {
    @Override
    void bindItem(IdKey id, Object ob) {
        if (_items == null) {
            _items = new HashMap<>()
        }
        _items.put(id, ob)
    }

    @Override
    ObjectIdResolver newForDeserialization(Object context) {
        return new DedupingObjectIdResolver()
    }
}
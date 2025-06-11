package org.maurodata.domain.dataflow

import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.util.StdConverter
import groovy.transform.CompileStatic

@CompileStatic
@JsonDeserialize(converter = TypeConverter)
enum Type {
    SOURCE,
    TARGET

    static class TypeConverter extends StdConverter<String, Type> {
        @Override
        Type convert(String value) {
            value ? valueOf(value.toUpperCase()) : null
        }
    }

}
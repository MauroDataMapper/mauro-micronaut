package uk.ac.ox.softeng.mauro.domain.security

import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.util.StdConverter
import groovy.transform.CompileStatic

@CompileStatic
@JsonDeserialize(converter = RoleTypeConverter)
enum Role {
    READER,
    REVIEWER,
    AUTHOR,
    EDITOR,
    CONTAINER_ADMIN

    static class RoleTypeConverter extends StdConverter<String, Role> {
        @Override
        Role convert(String value) {
            value ? valueOf(value.toUpperCase().replace(' ', '_')) : null
        }
    }
}
package org.maurodata.domain.security

import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.util.StdConverter
import groovy.transform.CompileStatic

@CompileStatic
@JsonDeserialize(converter = RoleTypeConverter)
enum Role {

    READER('Reader'),
    REVIEWER('Reviewer'),
    AUTHOR('Author'),
    EDITOR('Editor'),
    CONTAINER_ADMIN('Container Admin')

    String displayName

    Role(String displayName) {
        this.displayName = displayName
    }

    static class RoleTypeConverter extends StdConverter<String, Role> {
        @Override
        Role convert(String value) {
            value ? valueOf(value.toUpperCase().replace(' ', '_')) : null
        }
    }

    static class RoleDTO {
        String name
        String displayName

        // Empty constructor for Jackson Serialization
        RoleDTO() { }

        RoleDTO(Role role) {
            this.name = role.name()
            this.displayName = role.displayName
        }
    }

}

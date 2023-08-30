package uk.ac.ox.softeng.mauro.model.version

import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.util.StdConverter

@JsonDeserialize(converter = VersionChangeTypeConverter)
enum VersionChangeType {
    MAJOR,
    MINOR,
    PATCH

    static class VersionChangeTypeConverter extends StdConverter<String, VersionChangeType> {
        @Override
        VersionChangeType convert(String value) {
            valueOf(value.toUpperCase())
        }
    }
}
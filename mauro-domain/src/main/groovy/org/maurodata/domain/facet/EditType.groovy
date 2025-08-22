package org.maurodata.domain.facet


import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.util.StdConverter
import groovy.transform.CompileStatic

/**
 * An enumeration to describe the change in version numbers between iterations of a model.
* <p>
 *     May be either a major, minor or patch version change.
 *     An internal class VersionChangeTypeConverter is used to transform the enumeration into a JSON string for
 *     use within the API.
 */
@CompileStatic
@JsonDeserialize(converter = EditTypeConverter)
enum EditType {

    CREATE,
    IMPORT,
    UPDATE,
    DELETE,
    MERGE,
    COPY,
    FINALISE,
    CHANGELOG,
    VIEW,
    EXPORT,
    CHANGENOTICE

    static class EditTypeConverter extends StdConverter<String, EditType> {

        @Override
        EditType convert(String value) {
            valueOf(value.toUpperCase())
        }

    }

}

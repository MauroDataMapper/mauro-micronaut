package org.maurodata.profile

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonValue
import groovy.json.JsonSlurper
import org.apache.commons.lang3.time.DateUtils

enum ProfileFieldDataType {

    BOOLEAN('boolean'),
    STRING('string'),
    TEXT('text'),   // like a string but makes a bigger box in the UI
    INT('int'),
    DECIMAL('decimal'),
    DATE('date'),
    DATETIME('datetime'),
    TIME('time'),
    FOLDER('folder'),
    MODEL('model'),
    ENUMERATION('enumeration'),
    JSON('json')

    @JsonValue
    String label

    @JsonIgnore
    static String[] accptableDateFormats = ['dd/MM/yyyy', 'dd-MM-yyyy', 'MM/dd/yyyy', 'MM-dd-yyyy', 'yyyy/MM/dd', 'yyyy-MM-dd']
    @JsonIgnore
    static String[] accptableDateTimeFormats = ['dd/MM/yyyy\'T\'HH:mm:ss', 'dd-MM-yyyy\'T\'HH:mm:ss']
    @JsonIgnore
    static String[] accptableTimeFormats = ['HH:mm:ss', 'HH:mm']

    @JsonIgnore
    static JsonSlurper jsonSlurper = new JsonSlurper()

    ProfileFieldDataType(String label) {
        this.label = label
    }

    @JsonCreator
    static ProfileFieldDataType fromString(String key) {
        values().find { it.label.equalsIgnoreCase(key) }
    }

    Boolean validateStringAgainstType(String input) {
        if(!input || input == '') { // empty values are always valid against the data type
            return true
        }
        switch (label) {
            case 'boolean':
                if(!["true", "t", "false", "f"].contains(input)) {
                    return false
                }
                break
            case 'int':
                try {
                    Integer.parseInt(input)
                } catch (Exception ignored) {
                    return false
                }
                break
            case 'decimal':
                try {
                    Double.parseDouble(input)
                } catch (Exception ignored) {
                    return false
                }
                break

            case 'date':
                try {
                    DateUtils.parseDateStrictly(input, accptableDateFormats)
                } catch (Exception ignored) {
                    return false
                }
                break
            case 'datetime':
                try {
                    DateUtils.parseDateStrictly(input, accptableDateTimeFormats)
                } catch (Exception ignored) {
                    return false
                }
                break
            case 'time':
                try {
                    DateUtils.parseDateStrictly(input, accptableTimeFormats)
                } catch (Exception ignored) {
                    return false
                }
                break
            case 'model':
                try {
                    UUID.fromString(input)
                    // TODO - check there's actually a model here?
                } catch (Exception ignored) {
                    return false
                }
                break
            case 'folder':
                try {
                    UUID.fromString(input)
                    // TODO - check there's actually a folder here?
                } catch (Exception ignored) {
                    return false
                }
                break
            case 'json':
                try {
                    jsonSlurper.parseText(input)
                } catch (Exception ignored) {
                    return false
                }
                break
        }
        return true
    }


}
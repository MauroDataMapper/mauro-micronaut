package uk.ac.ox.softeng.mauro.test.domain.profile

import io.micronaut.test.extensions.spock.annotation.MicronautTest
import spock.lang.Specification
import uk.ac.ox.softeng.mauro.profile.ProfileFieldDataType

@MicronautTest
class ProfileFieldDataTypeSpec extends  Specification{


    def "Test validation of profile field values"() {

        expect:
        ProfileFieldDataType.fromString(dataType).validateStringAgainstType(value) == result

        where:
        dataType        | value                        | result
        "boolean"       | "t"                          | true
        "boolean"       | "true"                       | true
        "boolean"       | "f"                          | true
        "boolean"       | "false"                      | true
        "boolean"       | ""                           | true
        "boolean"       | null                         | true
        "boolean"       | "something"                  | false

        "string"        | "some text here"             | true
        "string"        | ""                           | true
        "string"        | null                         | true

        "text"          | "some text here"             | true
        "text"          | """"<h1>Header</h1>
                              <p>some text here</p>""" | true
        "text"          | ""                           | true
        "text"          | null                         | true

        "int"           | "0"                          | true
        "int"           | "1"                          | true
        "int"           | "12345678"                   | true
        "int"           | "-1"                         | true
        "int"           | ""                           | true
        "int"           | null                         | true
        "int"           | "abc"                        | false
        "int"           | "17e3"                       | false

        "decimal"       | "0"                          | true
        "decimal"       | "1.5"                        | true
        "decimal"       | "12345678.872398173"         | true
        "decimal"       | "-1.12333"                   | true
        "decimal"       | "17e3"                       | true
        "decimal"       | ""                           | true
        "decimal"       | null                         | true
        "decimal"       | "abc"                        | false
        "decimal"       | "1.3.4"                      | false

        "date"          | "01/01/1990"                 | true
        "date"          | "31/12/2034"                 | true
        "date"          | "12/31/2034"                 | true
        "date"          | "2034/12/31"                 | true
        "date"          | "01-01-1990"                 | true
        "date"          | "31-12-2034"                 | true
        "date"          | "12-31-2034"                 | true
        "date"          | "2034-12-31"                 | true
        "date"          | ""                           | true
        "date"          | null                         | true
        "date"          | "Monday"                     | false
        "date"          | "Tomorrow"                   | false
        "date"          | "01/01/01/12"                | false
        "date"          | "2034/31/12"                 | false
        "date"          | "2034-31-12"                 | false
        "date"          | "1234"                       | false

        "datetime"      | "01/01/1990T12:34:00"        | true
        "datetime"      | "31/12/2034T12:34:00"        | true
        "datetime"      | "01-01-1990T12:34:00"        | true
        "datetime"      | "31-12-2034T12:34:00"        | true
        "datetime"      | "31-12-2034T23:59:59"        | true
        "datetime"      | ""                           | true
        "datetime"      | null                         | true
        "datetime"      | "Monday"                     | false
        "datetime"      | "Tomorrow"                   | false
        "datetime"      | "01/01/12"                   | false
        "datetime"      | "01/01/01/12"                | false
        "datetime"      | "31-12-2034T59:59:59"        | false
        "datetime"      | "12/31/2034T12:34:00"        | false
        "datetime"      | "1234"                       | false

        "time"          | "12:34:00"                   | true
        "time"          | "23:59:59"                   | true
        "time"          | ""                           | true
        "time"          | null                         | true
        "time"          | "This afternoon"             | false
        "time"          | "Midnight"                   | false
        "time"          | "01/01/12"                   | false
        "time"          | "24:70:79"                   | false
        "time"          | "1234"                       | false

        "folder"        | UUID.randomUUID().toString() | true
        "folder"        | ""                           | true
        "folder"        | null                         | true
        "folder"        | "12431-23123-32abc-123"      | false

        "model"         | UUID.randomUUID().toString() | true
        "model"         | ""                           | true
        "model"         | null                         | true
        "model"         | "12431-23123-32abc-123"      | false

        // Enumerations are just strings - these are validated elsewhere
        "enumeration"   | "Male"                       | true
        "enumeration"   | ""                           | true
        "enumeration"   | null                         | true

        "json"          | "{}"                         | true
        "json"          | "[]"                         | true
        "json"          | "true"                       | true
        "json"          | "\"abcde\""                  | true
        "json"          | "123"                        | true
        "json"          | "{\"key\": \"value\"}"       | true
        "json"          | "[\"One\", \"Two\"]"         | true
        "json"          | ""                           | true
        "json"          | null                         | true
        "json"          | "abcde"                      | false
        "json"          | "[}"                         | false
        "json"          | "{ key: \"value\"}"          | false
    }
}

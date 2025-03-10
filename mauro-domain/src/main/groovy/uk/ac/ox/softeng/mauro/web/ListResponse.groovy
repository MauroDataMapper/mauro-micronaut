package uk.ac.ox.softeng.mauro.web

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.ObjectMapper
import groovy.transform.CompileStatic
import groovy.transform.stc.ClosureParams
import groovy.transform.stc.FirstParam
import groovy.transform.stc.FromString

/**
 * A ListResponse is a helper class for providing pagination in the API.  It contains a list of items, and a total count
 * of items, which is larger than or equal to the number of items returned
 */
@CompileStatic
class ListResponse<T> {

    Integer count

    @JsonInclude
    List<T> items

    static ListResponse from(List items) {
        new ListResponse(count: items.size(), items: items ?: [])
    }

    void bindItems(ObjectMapper objectMapper, Class<T> clazz) {
        items = items.collect { item ->
            return objectMapper.convertValue(item, clazz)
        }
    }

    List<T> each(@ClosureParams(value = FromString, options = ["T"]) Closure c) {
        items?.each c
    }

    T find(@ClosureParams(value = FromString, options = ["T"]) Closure c) {
        items?.find c
    }

}
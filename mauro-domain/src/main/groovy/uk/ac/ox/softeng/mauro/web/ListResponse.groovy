package uk.ac.ox.softeng.mauro.web

import com.fasterxml.jackson.annotation.JsonInclude
import groovy.transform.CompileStatic

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
}
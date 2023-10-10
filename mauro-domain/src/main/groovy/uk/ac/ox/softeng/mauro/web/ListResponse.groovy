package uk.ac.ox.softeng.mauro.web

import com.fasterxml.jackson.annotation.JsonInclude

class ListResponse<T> {
    Integer count

    @JsonInclude
    List<T> items

    static ListResponse from(List items) {
        new ListResponse(count: items.size(), items: items ?: [])
    }
}

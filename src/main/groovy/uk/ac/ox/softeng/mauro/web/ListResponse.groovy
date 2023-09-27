package uk.ac.ox.softeng.mauro.web

import com.fasterxml.jackson.annotation.JsonInclude

class ListResponse<O> {
    Integer count

    @JsonInclude
    List<O> items

    static ListResponse from(List items) {
        new ListResponse(count: items.size(), items: items ?: [])
    }
}

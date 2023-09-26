package uk.ac.ox.softeng.mauro.web

import com.fasterxml.jackson.annotation.JsonInclude

class ListResponse {
    Integer count

    @JsonInclude
    List items

    static ListResponse from(List items) {
        new ListResponse(count: items.size(), items: items ?: [])
    }
}

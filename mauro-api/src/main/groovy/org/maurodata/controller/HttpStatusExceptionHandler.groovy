package org.maurodata.controller

import io.micronaut.context.annotation.Replaces
import io.micronaut.http.server.exceptions.ExceptionHandler
import io.micronaut.http.server.exceptions.HttpStatusHandler
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpResponse
import jakarta.inject.Singleton
import io.micronaut.http.HttpStatus
import io.micronaut.http.exceptions.HttpStatusException

@Replaces(HttpStatusHandler.class)
@Singleton
class HttpStatusExceptionHandler implements ExceptionHandler<HttpStatusException, HttpResponse<Map<String, Object>>> {

    @Override
    HttpResponse<Map<String, Object>> handle(HttpRequest request, HttpStatusException exception) {
        HttpStatus status = exception.getStatus()
        String message = exception.getMessage()
        if (message == null || message.trim().isEmpty()) {
            Object body = exception.getBody().orElse(null)
            if (body != null) {
                message = body.toString()
            }
        }

        if (message == null || message.trim().isEmpty()) {
            System.err.println("Missing exception message!")
            exception.printStackTrace()
        }

        Map<String, Object> errorMessage = Collections.singletonMap("message", message)

        Map<String, Object> embedded = new HashMap<>()
        embedded.put("errors", List.of(errorMessage))

        Map<String, Object> links = new HashMap<>()
        links.put("self", Map.of(
            "href", request.getUri().toString(),
            "templated", false
        ))

        Map<String, Object> body = new LinkedHashMap<>(10)
        body.put("message", status.getReason())
        body.put("total", 1)
        body.put("errors", List.of(errorMessage))
        body.put("_embedded", embedded)
        body.put("_links", links)

        return HttpResponse.status(status).body(body)
    }
}

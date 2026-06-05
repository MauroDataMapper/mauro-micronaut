package org.maurodata.controller

import groovy.transform.CompileStatic
import io.micronaut.context.annotation.Replaces
import io.micronaut.http.server.exceptions.ExceptionHandler
import io.micronaut.http.server.exceptions.HttpStatusHandler
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpResponse
import jakarta.inject.Singleton
import io.micronaut.http.HttpStatus
import io.micronaut.http.exceptions.HttpStatusException

@CompileStatic
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

        Map<String, String> errorMessage = Collections.singletonMap("message", message)

        Map<String, Object> body = GlobalThrowableExceptionHandler.createMessageBody(status.getReason(), errorMessage, request.getUri())

        return HttpResponse.status(status).body(body)
    }
}
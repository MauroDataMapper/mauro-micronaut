package org.maurodata.controller

import groovy.transform.CompileStatic
import io.micronaut.context.annotation.Replaces
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import io.micronaut.http.exceptions.ContentLengthExceededException
import io.micronaut.http.server.exceptions.ContentLengthExceededHandler
import io.micronaut.http.server.exceptions.ExceptionHandler
import jakarta.inject.Singleton

@CompileStatic
@Replaces(ContentLengthExceededHandler)
@Singleton
class ContentLengthExceededExceptionHandler implements ExceptionHandler<ContentLengthExceededException, HttpResponse<Map<String, Object>>> {

    @Override
    HttpResponse<Map<String, Object>> handle(HttpRequest request, ContentLengthExceededException exception) {
        String message = exception.message ?: 'Request content exceeds the configured maximum size'
        Map<String, String> errorMessage = Collections.singletonMap('message', message)
        Map<String, Object> body = GlobalThrowableExceptionHandler.createMessageBody(
            HttpStatus.REQUEST_ENTITY_TOO_LARGE.reason,
            errorMessage,
            request.uri)

        HttpResponse.status(HttpStatus.REQUEST_ENTITY_TOO_LARGE).body(body)
    }
}

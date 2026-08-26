package org.maurodata.controller

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.micronaut.context.annotation.Property
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import io.micronaut.http.server.exceptions.ExceptionHandler
import jakarta.inject.Singleton

import java.sql.SQLException

@CompileStatic
@Singleton
@Slf4j
class GlobalThrowableExceptionHandler implements ExceptionHandler<Throwable, HttpResponse<Map<String, Object>>> {

    @Property(name = 'mauro.errors.sanitize', defaultValue = 'true')
    boolean sanitiseUiErrors

    @Override
    HttpResponse<Map<String, Object>> handle(HttpRequest request, Throwable exception) {
        Throwable root = rootCause(exception)
        String message =
            (sanitiseUiErrors && isDatabaseError(root)) ? 'Database error - refer to logs for details' : exception.message

        Map<String, String> errorMessage = Collections.singletonMap('message', message)

        log.error('Unhandled exception', exception)
        Map<String, Object> body = createMessageBody(HttpStatus.INTERNAL_SERVER_ERROR.reason, errorMessage, request.uri)

        HttpResponse.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body)
    }

    private static boolean isDatabaseError(Throwable t) {
        t instanceof SQLException || t?.class?.name == 'org.postgresql.util.PSQLException'
    }

    private static Throwable rootCause(Throwable t) {
        Throwable cur = t
        while (cur?.cause && cur.cause != cur) cur = cur.cause
        cur
    }

    static Map<String, Object> createMessageBody(String message, Map<String, String> errorMessage, URI requestUri) {
        Map<String, Object> body = new LinkedHashMap<>(6)
        body.put('message', message)
        body.put('total', 1)
        body.put('errors', List.of(errorMessage))
        body.put('_embedded', [errors: List.of(errorMessage)])
        body.put('_links', [self: [href: requestUri.toString(), templated: false]])

        return body
    }

}

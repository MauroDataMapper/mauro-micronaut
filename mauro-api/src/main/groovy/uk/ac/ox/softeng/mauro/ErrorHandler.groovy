package uk.ac.ox.softeng.mauro

import uk.ac.ox.softeng.mauro.domain.model.Item

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.micronaut.core.annotation.Nullable
import io.micronaut.http.HttpStatus
import io.micronaut.http.exceptions.HttpStatusException
@Slf4j
@CompileStatic
class ErrorHandler {
    static void handleError(HttpStatus httpStatus, String errorMessage) {
        throw new HttpStatusException(httpStatus, errorMessage)
    }

     static void handleError(HttpStatus httpStatus, @Nullable Object object, String errorMessage) {
        if (!object) handleError(httpStatus, errorMessage)
    }
}

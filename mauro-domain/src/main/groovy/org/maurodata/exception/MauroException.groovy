package org.maurodata.exception

import groovy.transform.CompileStatic

@CompileStatic
abstract class MauroException extends RuntimeException {

    MauroException(String message) {
        super(message)
    }

    MauroException(String message, Throwable cause) {
        super(message, cause)
    }
}

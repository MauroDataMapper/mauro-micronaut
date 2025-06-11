package org.maurodata.exception

import groovy.transform.CompileStatic

@CompileStatic
class MauroInternalException extends MauroException {

    MauroInternalException(String message) {
        super(message)
    }

    MauroInternalException(String message, Throwable cause) {
        super(message, cause)
    }
}

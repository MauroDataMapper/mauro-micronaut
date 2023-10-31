package uk.ac.ox.softeng.mauro.exception

import groovy.transform.CompileStatic

@CompileStatic
class MauroApplicationException extends MauroException {

    MauroApplicationException(String message) {
        super(message)
    }

    MauroApplicationException(String message, Throwable cause) {
        super(message, cause)
    }
}

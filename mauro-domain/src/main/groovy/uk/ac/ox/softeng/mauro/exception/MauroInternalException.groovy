package uk.ac.ox.softeng.mauro.exception

class MauroInternalException extends MauroException {

    MauroInternalException(String message) {
        super(message)
    }

    MauroInternalException(String message, Throwable cause) {
        super(message, cause)
    }
}

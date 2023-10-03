package uk.ac.ox.softeng.mauro.exception

abstract class MauroException extends RuntimeException {

    MauroException(String message) {
        super(message)
    }

    MauroException(String message, Throwable cause) {
        super(message, cause)
    }
}

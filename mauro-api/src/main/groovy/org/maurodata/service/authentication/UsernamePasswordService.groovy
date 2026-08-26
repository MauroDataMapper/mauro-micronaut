package org.maurodata.service.authentication

import org.maurodata.security.utils.SecureRandomStringGenerator

import groovy.transform.CompileStatic
import io.micronaut.context.annotation.Property
import jakarta.inject.Singleton
import org.apache.commons.text.RandomStringGenerator

import java.security.MessageDigest
import java.security.NoSuchAlgorithmException

@CompileStatic
@Singleton
class UsernamePasswordService {
    @Property(name = 'micronaut.security.endpoints.login.enabled', defaultValue = 'true')
    boolean micronautSecurityEndpointsLoginEnabled

    @Property(name = 'mauro.login.password-length', defaultValue = '12')
    int mauroLoginPasswordLength

    @Property(name = 'mauro.login.password-max-length', defaultValue = '64')
    int mauroLoginPasswordMaxLength

    @Property(name = 'mauro.login.temp-password-length', defaultValue = '8')
    int mauroLoginTempPasswordLength

    @Property(name = 'mauro.login.password-length-denies', defaultValue = 'false')
    boolean mauroLoginPasswordLengthDenies

    @Property(name = 'mauro.login.retry-wait', defaultValue = '1000')
    long mauroLoginRetryWait

    @Property(name = 'mauro.login.fail-wait', defaultValue = '2000')
    long mauroLoginFailWait

    enum InputValidationCheckStatus {
        MISSING_DETAILS,
        USERNAME_LENGTH_EXCEEDED,
        PASSWORD_LENGTH_EXCEEDED,
        OK
    }

    enum PasswordUse {
        TEMPORARY,
        MAIN
    }

    enum PasswordRequirementsCheckStatus {
        PASSWORD_LENGTH_TOO_SMALL,
        PASSWORD_CONTAINS_USERNAME,
        OK
    }

    volatile long lastAttempt = 0L

    // #CWE-307
    void failWait() {
        Thread.currentThread().sleep(mauroLoginFailWait)
    }

    long retryWait() {
        long waited = 0L
        // Rate limit all password tries to 1 per mauroLoginRetryWait ms
        if (mauroLoginRetryWait > 0) {
            synchronized (this) {
                final long now = System.currentTimeMillis()
                final long elapsedTime = now - lastAttempt
                if (elapsedTime < mauroLoginRetryWait) {
                    final long toWait = mauroLoginRetryWait - elapsedTime
                    Thread.currentThread().sleep(toWait)
                    waited = toWait
                }
                lastAttempt = now
            }
        }
        return waited
    }

    String getReasonMessage(InputValidationCheckStatus inputValidationCheckStatus) {
        switch (inputValidationCheckStatus) {
            case InputValidationCheckStatus.MISSING_DETAILS:
                return 'Username or password is empty'
            case InputValidationCheckStatus.USERNAME_LENGTH_EXCEEDED:
                return "The username exceeds 255 characters in length"
            case InputValidationCheckStatus.PASSWORD_LENGTH_EXCEEDED:
                return "The password exceeds ${mauroLoginPasswordMaxLength} characters in length"
            case InputValidationCheckStatus.OK:
                return "Ok"
            default:
                return "Unspecified reason"
        }
    }

    String getReasonMessage(PasswordRequirementsCheckStatus passwordRequirementsCheckStatus, final PasswordUse passwordUse) {
        switch (passwordRequirementsCheckStatus) {
            case PasswordRequirementsCheckStatus.PASSWORD_LENGTH_TOO_SMALL:
                if(passwordUse == PasswordUse.TEMPORARY) {
                    return "The password is not long enough. Temporary passwords must be at least ${mauroLoginTempPasswordLength} characters long"
                } else {
                    return "The password is not long enough. The password must be at least ${mauroLoginPasswordLength} characters long"
                }
            case PasswordRequirementsCheckStatus.PASSWORD_CONTAINS_USERNAME:
                return "The password contains the username"
            case PasswordRequirementsCheckStatus.OK:
                return "Ok"
            default:
                return "Unspecified reason"
        }
    }

    // #CWE-20
    InputValidationCheckStatus inputValidationCheckUsernamePassword(final String username, final String password) {
        if (!username || !password || username.trim().isEmpty() || password.trim().isEmpty()) {
            return InputValidationCheckStatus.MISSING_DETAILS
        }
        // #CWE-1284
        if (username.length() > 255) {
            return InputValidationCheckStatus.USERNAME_LENGTH_EXCEEDED
        }
        // #CWE-1284
        if (password.length() > mauroLoginPasswordMaxLength) {
            return InputValidationCheckStatus.PASSWORD_LENGTH_EXCEEDED
        }

        return InputValidationCheckStatus.OK
    }

    // #CWE-20
    InputValidationCheckStatus inputValidationCheckPassword(final String password) {
        if (!password || password.trim().isEmpty()) {
            return InputValidationCheckStatus.MISSING_DETAILS
        }
        // #CWE-1284
        if (password.length() > mauroLoginPasswordMaxLength) {
            return InputValidationCheckStatus.PASSWORD_LENGTH_EXCEEDED
        }
        return InputValidationCheckStatus.OK
    }

    // #CWE-521
    PasswordRequirementsCheckStatus passwordRequirementsCheck(final String username, final String password, final PasswordUse passwordUse) {
        if (username != null && !username.isEmpty()) {

            if (password.toLowerCase().contains(username.toLowerCase())) {
                return PasswordRequirementsCheckStatus.PASSWORD_CONTAINS_USERNAME
            }

            final int at = username.indexOf('@')
            if (at >= 0) {
                final String user = username.substring(0, at).toLowerCase()
                if (!user.isEmpty() && password.toLowerCase().contains(user)) {
                    return PasswordRequirementsCheckStatus.PASSWORD_CONTAINS_USERNAME
                }
                if (at + 1 < username.length()) {
                    final String host = username.substring(at + 1).toLowerCase()
                    if (!host.isEmpty() && password.toLowerCase().contains(host)) {
                        return PasswordRequirementsCheckStatus.PASSWORD_CONTAINS_USERNAME
                    }
                }
            }
        }

        if (passwordUse == PasswordUse.TEMPORARY && password.length() < mauroLoginTempPasswordLength) {
            return PasswordRequirementsCheckStatus.PASSWORD_LENGTH_TOO_SMALL
        } else if (passwordUse == PasswordUse.MAIN && password.length() < mauroLoginPasswordLength) {
            return PasswordRequirementsCheckStatus.PASSWORD_LENGTH_TOO_SMALL
        }

        return PasswordRequirementsCheckStatus.OK
    }

    String generateTemporaryPassword() {
        generateRandomPassword(mauroLoginTempPasswordLength)
    }

    boolean passwordEquals(final byte[] hashedPassword, final byte[] salt, final String clearPassword) {
        return hashedPassword == generateHash(clearPassword, salt)
    }

    static String generateRandomPassword(final int minLength = 16) {
        final RandomStringGenerator gen = SecureRandomStringGenerator.alphanumericGenerator()
        final int iterations = minLength.intdiv(4) + (minLength % 4 != 0 ? 1 : 0)
        final StringBuilder str = new StringBuilder(minLength)
        for (int i = 0; i < iterations; i++) {
            if (i < iterations - 1) {
                str.append(gen.generate(3))
                str.append('-')
            } else {
                str.append(gen.generate(4))
            }
        }
        return str.toString()
    }

    static byte[] generateHash(final String password, final byte[] salt) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        if (!password) {
            return new byte[0]
        }
        final MessageDigest digest = MessageDigest.getInstance('SHA-256')
        digest.reset()
        digest.update(salt)
        return digest.digest(password.trim().getBytes('UTF-8'))
    }
}

package org.maurodata.service.authentication

import io.micronaut.context.annotation.Property
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import jakarta.inject.Inject
import org.maurodata.service.authentication.UsernamePasswordService.InputValidationCheckStatus
import org.maurodata.service.authentication.UsernamePasswordService.PasswordRequirementsCheckStatus
import org.maurodata.service.authentication.UsernamePasswordService.PasswordUse

import spock.lang.Specification

@MicronautTest(startApplication = false)
@Property(name = 'mauro.login.password-length', value = '16')
@Property(name = 'mauro.login.password-max-length', value = '20')
@Property(name = 'mauro.login.temp-password-length', value = '12')
@Property(name = 'mauro.login.retry-wait', value = '0')
@Property(name = 'mauro.login.fail-wait', value = '0')
class UsernamePasswordServiceSpec extends Specification {

    @Inject
    UsernamePasswordService usernamePasswordService

    void 'main password requirements enforce configured minimum length'() {
        expect:
        usernamePasswordService.passwordRequirementsCheck('user@example.com', 'a'.repeat(15), PasswordUse.MAIN) ==
            PasswordRequirementsCheckStatus.PASSWORD_LENGTH_TOO_SMALL
        usernamePasswordService.passwordRequirementsCheck('user@example.com', 'a'.repeat(16), PasswordUse.MAIN) ==
            PasswordRequirementsCheckStatus.OK
        usernamePasswordService.getReasonMessage(
            PasswordRequirementsCheckStatus.PASSWORD_LENGTH_TOO_SMALL, PasswordUse.MAIN) ==
            'The password is not long enough. The password must be at least 16 characters long'
    }

    void 'temporary password requirements enforce configured minimum length'() {
        expect:
        usernamePasswordService.passwordRequirementsCheck('user@example.com', 'a'.repeat(11), PasswordUse.TEMPORARY) ==
            PasswordRequirementsCheckStatus.PASSWORD_LENGTH_TOO_SMALL
        usernamePasswordService.passwordRequirementsCheck('user@example.com', 'a'.repeat(12), PasswordUse.TEMPORARY) ==
            PasswordRequirementsCheckStatus.OK
        usernamePasswordService.getReasonMessage(
            PasswordRequirementsCheckStatus.PASSWORD_LENGTH_TOO_SMALL, PasswordUse.TEMPORARY) ==
            'The password is not long enough. Temporary passwords must be at least 12 characters long'
    }

    void 'generated temporary password respects configured minimum length'() {
        when:
        String temporaryPassword = usernamePasswordService.generateTemporaryPassword()

        then:
        temporaryPassword.length() >= 12
        temporaryPassword ==~ /[A-Za-z0-9-]+/
    }

    void 'password requirements reject passwords containing username parts case-insensitively'() {
        expect:
        usernamePasswordService.passwordRequirementsCheck(
            'User.Name@Example.Org', password, PasswordUse.MAIN) ==
            PasswordRequirementsCheckStatus.PASSWORD_CONTAINS_USERNAME

        where:
        password << [
            'prefix-user.name@example.org-suffix',
            'prefix-USER.NAME-suffix',
            'prefix-example.org-suffix'
        ]
    }

    void 'password requirements allow passwords which meet length and do not contain username parts'() {
        expect:
        usernamePasswordService.passwordRequirementsCheck(
            'user@example.com', 'long-enough-password', PasswordUse.MAIN) == PasswordRequirementsCheckStatus.OK
    }

    void 'input validation rejects missing values and configured maximum length violations'() {
        expect:
        usernamePasswordService.inputValidationCheckUsernamePassword(username, password) == expectedStatus

        where:
        username          | password       || expectedStatus
        null              | 'validPassword' || InputValidationCheckStatus.MISSING_DETAILS
        'user@example.com'| null           || InputValidationCheckStatus.MISSING_DETAILS
        ' '.repeat(256)   | 'validPassword' || InputValidationCheckStatus.MISSING_DETAILS
        'u'.repeat(256)   | 'validPassword' || InputValidationCheckStatus.USERNAME_LENGTH_EXCEEDED
        'user@example.com'| 'p'.repeat(21) || InputValidationCheckStatus.PASSWORD_LENGTH_EXCEEDED
        'user@example.com'| 'p'.repeat(20) || InputValidationCheckStatus.OK
    }
}

package org.maurodata.persistence.email

import jakarta.inject.Inject
import spock.lang.Specification
import org.maurodata.domain.email.Email
import org.maurodata.persistence.ContainerizedTest
import org.maurodata.persistence.security.EmailRepository

import java.time.Instant

@ContainerizedTest
class EmailRepositorySpec extends Specification {

    @Inject
    EmailRepository emailRepository

    def "Test store and retrieve email"() {

        when:

        Email testEmail = Email.build {
            sentToEmailAddress "test@test.com"
            subject "My test email"
            body """
                            Dear Test,
                            Here is an email, 
                            Thank you"""
            emailServiceUsed "Default email service"
            dateTimeSent Instant.now()
            successfullySent true
        }

        Email savedEmail = emailRepository.save(testEmail)

        then:
        savedEmail.id

        when:
        Email retrievedEmail = emailRepository.findById(savedEmail.id).get()

        then:
        retrievedEmail.sentToEmailAddress == testEmail.sentToEmailAddress
        retrievedEmail.subject == testEmail.subject
        retrievedEmail.body == testEmail.body

        retrievedEmail.dateTimeSent.toEpochMilli() ==  testEmail.dateTimeSent.toEpochMilli()
        retrievedEmail.successfullySent == testEmail.successfullySent

        when:
        emailRepository.delete(savedEmail)

        then:
        emailRepository.findById(savedEmail.id).isEmpty()

    }
}

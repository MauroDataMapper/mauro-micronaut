package uk.ac.ox.softeng.mauro.persistence.email

import jakarta.inject.Inject
import spock.lang.Shared
import spock.lang.Specification
import uk.ac.ox.softeng.mauro.domain.datamodel.DataClass
import uk.ac.ox.softeng.mauro.domain.datamodel.DataElement
import uk.ac.ox.softeng.mauro.domain.datamodel.DataModel
import uk.ac.ox.softeng.mauro.domain.datamodel.DataType
import uk.ac.ox.softeng.mauro.domain.email.Email
import uk.ac.ox.softeng.mauro.domain.folder.Folder
import uk.ac.ox.softeng.mauro.persistence.ContainerizedTest
import uk.ac.ox.softeng.mauro.persistence.cache.AdministeredItemCacheableRepository
import uk.ac.ox.softeng.mauro.persistence.cache.ModelCacheableRepository
import uk.ac.ox.softeng.mauro.persistence.datamodel.DataModelContentRepository
import uk.ac.ox.softeng.mauro.persistence.security.EmailRepository

import java.time.Instant
import java.time.OffsetDateTime

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
            dateTimeSent OffsetDateTime.now()
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
        retrievedEmail.dateTimeSent == testEmail.dateTimeSent
        retrievedEmail.successfullySent == testEmail.successfullySent

        when:
        emailRepository.delete(savedEmail)

        then:
        emailRepository.findById(savedEmail.id).isEmpty()

    }
}

package uk.ac.ox.softeng.mauro.email

import groovy.util.logging.Slf4j
import io.micronaut.context.annotation.Replaces
import jakarta.inject.Singleton
import uk.ac.ox.softeng.mauro.domain.email.Email
import uk.ac.ox.softeng.mauro.domain.security.CatalogueUser
import uk.ac.ox.softeng.mauro.plugin.EmailPlugin
import uk.ac.ox.softeng.mauro.service.email.MicronautJavamailEmailPlugin

@Slf4j
@Singleton
@Replaces(MicronautJavamailEmailPlugin)
class MockEmailPlugin implements EmailPlugin {

    String version = '1.0.0'

    String displayName = 'Mock Email Plugin'


    @Override
    boolean configure(Map props) {
        return false
    }

    @Override
    void retrySendEmail(Email email) throws Exception {
        log.info("Retrying sending email (Mock Email Plugin)")
        if(email.sentToEmailAddress == 'fail@test.com') {
            throw new Exception ('Failed to send email as part of a test (this was probably intended)')
        }
    }

    @Override
    void sendEmail(CatalogueUser catalogueUserRecipient, Email email) throws Exception {
        log.info("Sending email (Mock Email Plugin)")
        if(catalogueUserRecipient.emailAddress == 'fail@test.com') {
            throw new Exception ('Failed to send email as part of a test (this was probably intended)')
        }
    }

    @Override
    void testConnection() throws Exception {
        log.info("Testing connection (Mock Email Plugin)")
    }
}

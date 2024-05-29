package uk.ac.ox.softeng.mauro.service.email

import io.micronaut.email.Contact
import io.micronaut.email.Email
import io.micronaut.email.EmailSender
import jakarta.inject.Inject
import jakarta.inject.Singleton

@Singleton
class MicronautJavamailEmailPlugin implements EmailPlugin {

    String version = '1.0.0'

    String displayName = 'Micronaut Javamail Email Plugin'


    @Inject
    private final EmailSender<?, ?> emailSender

    MicronautJavamailEmailPlugin(EmailSender<?, ?> emailSender) {
        this.emailSender = emailSender
    }

    @Override
    boolean configure(Map props) {
        return false
    }

    @Override
    String sendEmail(uk.ac.ox.softeng.mauro.domain.email.Email email) {
        try {
            emailSender.send(Email.builder()
                    .from(new Contact('metadatacatalogue@gmail.com', 'Metadata Catalogue'))
                    .to(new Contact('metadatacatalogue@gmail.com', 'Metadata Catalogue'))
                    .subject("Micronaut test")
                    .body("<html><body><strong>Hello</strong> dear Micronaut user.</body></html>", "Hello dear Micronaut user"))
        } catch (Exception e) {
            e.printStackTrace()
            email.failureReason = e.message
        }
    }

    @Override
    boolean testConnection() {
        return true
    }

    @Override
    String validateEmail(uk.ac.ox.softeng.mauro.domain.email.Email email) {
        return null
    }

}

package org.maurodata.service.email

import org.maurodata.plugin.EmailPlugin

import com.fasterxml.jackson.annotation.JsonTypeName
import groovy.transform.CompileStatic
import io.micronaut.context.annotation.Property
import io.micronaut.context.env.Environment
import io.micronaut.email.Contact
import io.micronaut.email.Email
import io.micronaut.email.EmailSender
import io.micronaut.email.javamail.sender.MailPropertiesProvider
import jakarta.inject.Inject
import jakarta.inject.Singleton
import jakarta.mail.Session
import jakarta.mail.Transport
import org.maurodata.domain.security.CatalogueUser

@CompileStatic
@Singleton
@JsonTypeName("MicronautJavamailEmailPlugin")
class MicronautJavamailEmailPlugin implements EmailPlugin {

    @Inject
    private Environment environment

    String version = '1.0.0'

    String displayName = 'Micronaut Javamail Email Plugin'

    boolean enabled = true

    @Inject
    MailPropertiesProvider mailPropertiesProvider

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
    void sendEmail(CatalogueUser catalogueUserRecipient, org.maurodata.domain.email.Email email) throws Exception {
        emailSender.send(Email.builder()
                .from(new Contact('metadatacatalogue@gmail.com', 'Metadata Catalogue'))
                .to(new Contact(catalogueUserRecipient.emailAddress, catalogueUserRecipient.fullName))
                .subject(email.subject)
                .body(email.body))
    }

    @Override
    void retrySendEmail(org.maurodata.domain.email.Email email) throws Exception {
        emailSender.send(Email.builder()
                .from(new Contact('metadatacatalogue@gmail.com', 'Metadata Catalogue'))
                .to(email.sentToEmailAddress)
                .subject(email.subject)
                .body(email.body))
    }

    @Property(name = "javamail.authentication.username")
    private String user

    @Property(name = "javamail.authentication.password")
    private String password



    // We'll just throw an exception that the caller can handle here...
    @Override
    void testConnection() throws Exception {
        Session session = Session.getInstance(mailPropertiesProvider.mailProperties())
        Transport transport = session.getTransport("smtp")
        transport.connect(user, password)
        transport.close()
    }

}

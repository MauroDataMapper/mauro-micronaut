package org.maurodata.service.email

import org.maurodata.plugin.EmailPlugin

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.maurodata.domain.email.Email
import org.maurodata.domain.security.CatalogueUser
import org.maurodata.persistence.security.EmailRepository
import org.maurodata.plugin.MauroPluginService

import java.time.Instant

@CompileStatic
@Singleton
@Slf4j
class EmailService {

    @Inject
    MauroPluginService mauroPluginService

    @Inject
    EmailRepository emailRepository

    EmailPlugin getDefaultEmailPlugin() {
        // for now we'll assume that there's just one, or that we can arbitrarily sort and pick the first
        mauroPluginService.listPlugins(EmailPlugin).sort().first()
    }


    void sendEmail(CatalogueUser catalogueUserRecipient, Email email, boolean async = true) throws Exception {
        EmailPlugin emailPlugin = getDefaultEmailPlugin()
        email.emailServiceUsed = emailPlugin.displayName
        email.sentToEmailAddress = catalogueUserRecipient.emailAddress
        email.dateTimeSent = Instant.now()

        Closure emailSending = {
            log.debug("Sending email to ${catalogueUserRecipient.emailAddress}...")
            try {
                emailPlugin.sendEmail(catalogueUserRecipient, email)
                email.successfullySent = true
                log.debug("Successfully sent email to ${catalogueUserRecipient.emailAddress}")
                // Store email
                emailRepository.save(email)
            } catch (Exception e) {
                email.successfullySent = false
                email.failureReason = e.message
                log.debug("Failed to send email to ${catalogueUserRecipient.emailAddress}")
                log.error("Exception!", e)
                // Store email
                emailRepository.save(email)
                // Forward the exception to the caller
                throw e
            }
        }

        if(async) {
            Thread.start emailSending
        } else {
            emailSending.run()
        }

    }

    void retrySendEmail(Email email, boolean async = true) throws Exception {
        EmailPlugin emailPlugin = getDefaultEmailPlugin()
        email.emailServiceUsed = emailPlugin.displayName
        email.dateTimeSent = Instant.now()

        Closure emailSending = {
            log.debug("Retry sending email to ${email.sentToEmailAddress}...")
            try {
                emailPlugin.retrySendEmail(email)
                email.successfullySent = true
                log.debug("Successfully sent email to ${email.sentToEmailAddress}")
                // Store email
                emailRepository.update(email)
            } catch (Exception e) {
                email.successfullySent = false
                email.failureReason = e.message
                log.debug("Failed to send email to ${email.sentToEmailAddress}")
                log.error("Exception!", e)
                // Store email
                emailRepository.update(email)
                // Forward the exception to the caller
                throw e
            }
        }

        if(async) {
            Thread.start emailSending
        } else {
            emailSending.run()
        }

    }

    void testConnection() throws Exception {
        EmailPlugin emailPlugin = getDefaultEmailPlugin()
        try {
            emailPlugin.testConnection()
        } catch(Exception e) {
            log.debug("Failed to connect to email service")
            log.error("Exception!", e)
            // Forward the exception to the caller
            throw e
        }
    }




}

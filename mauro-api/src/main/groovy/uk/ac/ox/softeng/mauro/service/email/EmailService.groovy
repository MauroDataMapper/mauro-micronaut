package uk.ac.ox.softeng.mauro.service.email

import io.micronaut.email.DefaultEmailSender

import io.micronaut.email.EmailSender
import io.micronaut.email.MultipartBody
import jakarta.inject.Inject
import jakarta.inject.Singleton
import uk.ac.ox.softeng.mauro.domain.email.Email
import uk.ac.ox.softeng.mauro.persistence.security.EmailRepository
import uk.ac.ox.softeng.mauro.plugin.MauroPluginService

import java.time.OffsetDateTime

@Singleton
class EmailService {

    @Inject
    MauroPluginService mauroPluginService

    @Inject
    EmailRepository emailRepository

    EmailPlugin getDefaultEmailPlugin() {
        mauroPluginService.listPlugins(EmailPlugin).first()
    }


    String sendEmail(Email email) {
        EmailPlugin emailPlugin = getDefaultEmailPlugin()
        System.err.println(emailPlugin.displayName)
        email.emailServiceUsed = emailPlugin.displayName
        String response = emailPlugin.sendEmail(email)
        System.err.println(response)
        if(!response) {
            email.successfullySent = true
            email.dateTimeSent = OffsetDateTime.now()
        } else {
            email.successfullySent = false
            email.failureReason = response
        }
        // Store email
        emailRepository.save(email)
        return response

    }




}

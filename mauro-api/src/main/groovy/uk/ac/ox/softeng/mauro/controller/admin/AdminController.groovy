package uk.ac.ox.softeng.mauro.controller.admin

import uk.ac.ox.softeng.mauro.api.admin.AdminApi

import groovy.transform.CompileStatic
import io.micronaut.http.HttpStatus
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Post
import io.micronaut.http.exceptions.HttpStatusException
import io.micronaut.security.annotation.Secured
import io.micronaut.security.rules.SecurityRule
import jakarta.inject.Inject
import uk.ac.ox.softeng.mauro.domain.email.Email
import uk.ac.ox.softeng.mauro.domain.security.CatalogueUser
import uk.ac.ox.softeng.mauro.persistence.security.EmailRepository
import uk.ac.ox.softeng.mauro.plugin.MauroPluginService
import uk.ac.ox.softeng.mauro.plugin.exporter.ModelExporterPlugin
import uk.ac.ox.softeng.mauro.plugin.importer.ModelImporterPlugin
import uk.ac.ox.softeng.mauro.security.AccessControlService
import uk.ac.ox.softeng.mauro.plugin.EmailPlugin
import uk.ac.ox.softeng.mauro.service.email.EmailService
import uk.ac.ox.softeng.mauro.web.ListResponse

@CompileStatic
@Controller('/admin')
@Secured(SecurityRule.IS_ANONYMOUS)
class AdminController implements AdminApi {

    @Inject
    MauroPluginService mauroPluginService

    @Inject
    AccessControlService accessControlService

    @Inject
    EmailService emailService

    private final EmailRepository emailRepository

    AdminController(EmailRepository emailRepository) {
        this.emailRepository = emailRepository
    }

    @Get('/modules')
    List<LinkedHashMap<String, String>> modules() {
        accessControlService.checkAdministrator()

        mauroPluginService.getModulesList()
    }


    @Get('/providers/importers')
    List<ModelImporterPlugin> importers() {
        accessControlService.checkAdministrator()

        mauroPluginService.listPlugins(ModelImporterPlugin)
    }


    @Get('/providers/exporters')
    List<ModelExporterPlugin> exporters() {
        accessControlService.checkAdministrator()

        mauroPluginService.listPlugins(ModelExporterPlugin)
    }

    @Get('/providers/emailers')
    List<EmailPlugin> emailers() {
        mauroPluginService.listPlugins(EmailPlugin)
    }


    /**
     * This is new endpoint that can be used to test sending an email.  You should provide a catalogue user with a
     * firstName, lastName, and emailAddress set.
     * @param catalogueUser
     * @return
     */
    @Post('/email/sendTestEmail')
    Boolean sendTestEmail(@Body CatalogueUser catalogueUser) {
        accessControlService.checkAdministrator()

        if (!catalogueUser.emailAddress) {
            throw new HttpStatusException(HttpStatus.BAD_REQUEST, 'Please specify the email address of the recipient')
        }
        if (!catalogueUser.firstName || !catalogueUser.lastName) {
            throw new HttpStatusException(HttpStatus.BAD_REQUEST, 'Please specify the first and last names of the recipient')
        }

        try {
            Email email = Email.build {
                subject "Test email"
                body "This is a test email to confirm that email functionality is working in Mauro Data Mapper."
            }
            emailService.sendEmail(catalogueUser, email, false)
            return true
        } catch (Exception e) {
            throw new HttpStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.message)
        }
    }

    /**
     * This is a new endpoint which allows users to test the email connection without sending an email
     * @return
     */
    @Get('/email/testConnection')
    boolean testConnection() {
        accessControlService.checkAdministrator()

        try {
            emailService.testConnection()
            return true
        } catch (Exception e) {
            throw new HttpStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.message)
        }
    }


    @Get('/emails')
    ListResponse<Email> list() {
        accessControlService.checkAdministrator()
        System.err.println()
        ListResponse.from(emailRepository.readAll())
    }

    /**
     * This is a new endpoint which allows administrators to retry sending an email (usually one which previously failed to send)
     * @return
     */
    @Post('/emails/{emailId}/retry')
    boolean retryEmail(UUID emailId) {
        accessControlService.checkAdministrator()

        try {
            Email email = emailRepository.findById(emailId).get()
            if (!email) {
                throw new HttpStatusException(HttpStatus.BAD_REQUEST, "Email with id ${emailId.toString()} not found")
            }
            emailService.retrySendEmail(email, false)
            return true
        } catch (Exception e) {
            throw new HttpStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.message)
        }
    }
}

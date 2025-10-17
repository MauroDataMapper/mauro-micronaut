package org.maurodata.controller.admin

import org.maurodata.api.Paths
import org.maurodata.api.admin.AdminApi
import org.maurodata.audit.Audit
import org.maurodata.plugin.MauroPluginDTO

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
import org.maurodata.domain.email.Email
import org.maurodata.domain.security.CatalogueUser
import org.maurodata.persistence.security.EmailRepository
import org.maurodata.plugin.MauroPluginService
import org.maurodata.plugin.exporter.ModelExporterPlugin
import org.maurodata.plugin.exporter.ModelItemExporterPlugin
import org.maurodata.plugin.importer.ImporterPlugin
import org.maurodata.security.AccessControlService
import org.maurodata.plugin.EmailPlugin
import org.maurodata.service.email.EmailService
import org.maurodata.web.ListResponse

@CompileStatic
@Controller()
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

    @Audit
    @Get(Paths.ADMIN_MODULES_LIST)
    List<LinkedHashMap<String, String>> modules() {
        accessControlService.checkAdministrator()

        mauroPluginService.getModulesList()
    }


    @Audit
    @Get(Paths.ADMIN_IMPORTERS_LIST)
    List<MauroPluginDTO> importers() {
        accessControlService.checkAdministrator()

        mauroPluginService.listPluginsAsDTO(ImporterPlugin)
    }

    @Audit
    @Get(Paths.ADMIN_EXPORTERS_LIST)
    List<MauroPluginDTO> exporters() {
        accessControlService.checkAdministrator()

        mauroPluginService.listPluginsAsDTO(ModelExporterPlugin)
    }


    @Audit
    @Get(Paths.ADMIN_EMAILERS_LIST)
    List<MauroPluginDTO> emailers() {
        mauroPluginService.listPluginsAsDTO(EmailPlugin)
    }

    @Audit
    @Get(Paths.ADMIN_DATALOADERS_LIST)
    List<MauroPluginDTO> dataLoaders() {
        []
    }

    /**
     * This is new endpoint that can be used to test sending an email.  You should provide a catalogue user with a
     * firstName, lastName, and emailAddress set.
     * @param catalogueUser
     * @return
     */
    @Audit(level= Audit.AuditLevel.FILE_ONLY)
    @Post(Paths.ADMIN_EMAIL_SEND_TEST)
    Boolean sendTestEmail(@Body CatalogueUser catalogueUser) {
        accessControlService.checkAdministrator()

        if (!catalogueUser.emailAddress) {
            throw new HttpStatusException(HttpStatus.UNPROCESSABLE_ENTITY, 'Please specify the email address of the recipient')
        }
        if (!catalogueUser.firstName || !catalogueUser.lastName) {
            throw new HttpStatusException(HttpStatus.UNPROCESSABLE_ENTITY, 'Please specify the first and last names of the recipient')
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
    @Audit
    @Get(Paths.ADMIN_EMAIL_TEST_CONNECTION)
    boolean testConnection() {
        accessControlService.checkAdministrator()

        try {
            emailService.testConnection()
            return true
        } catch (Exception e) {
            throw new HttpStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.message)
        }
    }

    @Audit
    @Get(Paths.ADMIN_EMAILS)
    ListResponse<Email> listEmails() {
        accessControlService.checkAdministrator()
        ListResponse.from(emailRepository.readAll())
    }

    /**
     * This is a new endpoint which allows administrators to retry sending an email (usually one which previously failed to send)
     * @return
     */
    @Audit(level= Audit.AuditLevel.FILE_ONLY)
    @Post(Paths.ADMIN_EMAIL_RETRY)
    boolean retryEmail(UUID emailId) {
        accessControlService.checkAdministrator()

        try {
            Email email = emailRepository.findById(emailId).get()
            if (!email) {
                throw new HttpStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Email with id ${emailId.toString()} not found")
            }
            emailService.retrySendEmail(email, false)
            return true
        } catch (Exception e) {
            throw new HttpStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.message)
        }
    }
}

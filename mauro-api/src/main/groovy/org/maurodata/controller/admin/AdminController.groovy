package org.maurodata.controller.admin

import io.swagger.v3.oas.annotations.Operation
import org.maurodata.api.Paths
import org.maurodata.api.admin.AdminApi
import org.maurodata.audit.Audit
import org.maurodata.service.command.CommandService
import org.maurodata.plugin.MauroPluginDTO
import org.maurodata.service.plugin.PluginRepositoryService

import groovy.transform.CompileStatic
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Part
import io.micronaut.http.annotation.Post
import io.micronaut.http.exceptions.HttpStatusException
import io.micronaut.http.multipart.CompletedFileUpload
import io.micronaut.runtime.EmbeddedApplication
import io.micronaut.scheduling.TaskExecutors
import io.micronaut.scheduling.annotation.ExecuteOn
import io.micronaut.security.annotation.Secured
import io.micronaut.security.rules.SecurityRule
import jakarta.inject.Inject
import org.maurodata.domain.email.Email
import org.maurodata.domain.security.CatalogueUser
import org.maurodata.persistence.security.EmailRepository
import org.maurodata.plugin.MauroPluginService
import org.maurodata.plugin.exporter.ModelExporterPlugin
import org.maurodata.plugin.importer.ImporterPlugin
import org.maurodata.security.AccessControlService
import org.maurodata.plugin.EmailPlugin
import org.maurodata.service.email.EmailService
import org.maurodata.web.ListResponse

import jakarta.inject.Named

import java.util.concurrent.ExecutorService

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

    @Inject
    PluginRepositoryService service

    private final EmailRepository emailRepository

    @Inject
    @Named(TaskExecutors.IO)
    ExecutorService executor

    @Inject
    private EmbeddedApplication<? extends EmbeddedApplication> application

    private final CommandService commandService

    AdminController(EmailRepository emailRepository, CommandService commandService) {
        this.emailRepository = emailRepository
        this.commandService = commandService
    }

    @Audit
    @Operation(summary = "List the modules", description = "Returns the modules. It is only available to administrator users.")
    @Get(Paths.ADMIN_MODULES_LIST)
    List<LinkedHashMap<String, String>> modules() {
        accessControlService.checkAdministrator()

        mauroPluginService.getModulesList()
    }


    @Audit
    @Operation(summary = "List the importers", description = "Returns the importers. It is only available to administrator users.")
    @Get(Paths.ADMIN_IMPORTERS_LIST)
    List<MauroPluginDTO> importers() {
        accessControlService.checkAdministrator()

        mauroPluginService.listPluginsAsDTO(ImporterPlugin)
    }

    @Audit
    @Operation(summary = "List the exporters", description = "Returns the exporters. It is only available to administrator users.")
    @Get(Paths.ADMIN_EXPORTERS_LIST)
    List<MauroPluginDTO> exporters() {
        accessControlService.checkAdministrator()

        mauroPluginService.listPluginsAsDTO(ModelExporterPlugin)
    }


    @Audit
    @Operation(summary = "List the emailers", description = "Returns the emailers.")
    @Get(Paths.ADMIN_EMAILERS_LIST)
    List<MauroPluginDTO> emailers() {
        mauroPluginService.listPluginsAsDTO(EmailPlugin)
    }

    @Audit
    @Operation(summary = "List the data loaders", description = "Returns the data loaders.")
    @Get(Paths.ADMIN_DATALOADERS_LIST)
    List<MauroPluginDTO> dataLoaders() {
        []
    }

    @Audit
    @ExecuteOn(TaskExecutors.BLOCKING)
    @Get(Paths.ADMIN_AVAILABLE_PROVIDERS_LIST)
    List<Map<String, String>> available() {
        accessControlService.checkAdministrator()
        service.listAvailablePlugins()
    }

    @Audit
    @ExecuteOn(TaskExecutors.BLOCKING)
    @Post(Paths.ADMIN_INSTALL_PROVIDER)
    Map<String, Object> installPlugin(String plugin) {
        accessControlService.checkAdministrator()
        return service.installPlugin(plugin)
    }

    /**
     * This is new endpoint that can be used to test sending an email.  You should provide a catalogue user with a
     * firstName, lastName, and emailAddress set.
     * @param catalogueUser
     * @return
     */
    @Audit(level= Audit.AuditLevel.FILE_ONLY)
    @Operation(summary = "Send a test email", description = "Sends a test email. It is only available to administrator users.")
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
    @Operation(summary = "Test the email connection", description = "Tests the email connection. It is only available to administrator users.")
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
    @Operation(summary = "List the emails", description = "Returns the emails. It is only available to administrator users.")
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
    @Operation(summary = "Retry an email", description = "Retries an email. It is only available to administrator users.")
    @Post(Paths.ADMIN_EMAIL_RETRY)
    boolean retryEmail(UUID emailId) {
        accessControlService.checkAdministrator()

        try {
            Email email = emailRepository.findById(emailId).get()
            if (!email) {
                throw new HttpStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Email with id ${emailId} not found")
            }
            emailService.retrySendEmail(email, false)
            return true
        } catch (Exception e) {
            throw new HttpStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.message)
        }
    }

    @Audit
    @Post(Paths.ADMIN_SHUTDOWN)
    Boolean shutDown() {
        accessControlService.checkAdministrator()

        executor.submit(
            () -> {
                try {
                    Thread.sleep(1000)
                } catch (InterruptedException ignored) {
                }
                application.stop()
            }
        )

        return true
    }

    @Audit
    @Get(Paths.ADMIN_COMMANDS)
    List<Map<String,String>> commands() {
        accessControlService.checkAdministrator()
        commandService.commands()
    }

    @Audit
    @Post(Paths.ADMIN_COMMAND_PREPARE)
    Map<String,Object> planCommand(String commandName, @Body String[] commandArgs) {
        accessControlService.checkAdministrator()
        commandService.planCommand( commandName, commandArgs)
    }

    @Audit
    @Post(uri = Paths.ADMIN_COMMAND_UPLOAD_FILE, consumes = MediaType.MULTIPART_FORM_DATA)
    HttpResponse<?> fileCommand(UUID executionId, @Part("position") int position, @Part("file") CompletedFileUpload file) {

        accessControlService.checkAdministrator()
        commandService.fileCommand(executionId.toString(), position, file.getFilename(), file.getInputStream())

        return HttpResponse.ok()
    }

    @Audit
    @Post(uri = Paths.ADMIN_COMMAND_RUN)
    HttpResponse<InputStream> runCommand(UUID executionId) {
        accessControlService.checkAdministrator()
        try {
            return HttpResponse.ok(commandService.runCommand(executionId.toString()))
                                       .contentType(MediaType.APPLICATION_OCTET_STREAM)

        } catch(Throwable th) {
            th.printStackTrace()
            throw th
        }
    }

    @Audit
    @Post(uri = Paths.ADMIN_COMMAND_CLOSE)
    HttpResponse<?> closeCommand(UUID executionId) {
        accessControlService.checkAdministrator()
        commandService.closeCommand(executionId.toString())
        return HttpResponse.ok()
    }
}

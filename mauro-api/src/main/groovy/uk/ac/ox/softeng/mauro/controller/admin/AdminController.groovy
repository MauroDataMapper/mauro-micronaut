package uk.ac.ox.softeng.mauro.controller.admin

import uk.ac.ox.softeng.mauro.domain.email.Email
import uk.ac.ox.softeng.mauro.plugin.MauroPlugin
import uk.ac.ox.softeng.mauro.plugin.MauroPluginService
import uk.ac.ox.softeng.mauro.plugin.exporter.ModelExporterPlugin
import uk.ac.ox.softeng.mauro.plugin.importer.ModelImporterPlugin

import groovy.transform.CompileStatic
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import jakarta.inject.Inject
import uk.ac.ox.softeng.mauro.service.email.EmailService

@CompileStatic
@Controller('/admin')
class AdminController {

    @Inject
    MauroPluginService mauroPluginService

    @Inject
    EmailService emailService

    @Get('/modules')
    List<LinkedHashMap<String, String>> modules() {
        mauroPluginService.getModulesList()
    }


    @Get('/providers/importers')
    List<ModelImporterPlugin> importers() {
        mauroPluginService.listPlugins(ModelImporterPlugin)
    }


    @Get('/providers/exporters')
    List<ModelExporterPlugin> exporters() {
        mauroPluginService.listPlugins(ModelExporterPlugin)
    }


    @Get('/sendEmail')
    Boolean sendEmail() {
        try{

            Email email = Email.build {
                sentToEmailAddress "metadatacatalogue@gmail.com"
                subject "My test email"
                body """
                            Dear Test,
                            Here is an email, 
                            Thank you"""

            }
            emailService.sendEmail(email)
        } catch (Exception e) {
            e.printStackTrace()
            return false
        }
        return true
    }

}

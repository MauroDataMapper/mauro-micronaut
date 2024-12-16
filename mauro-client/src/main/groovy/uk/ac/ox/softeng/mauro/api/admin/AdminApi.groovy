package uk.ac.ox.softeng.mauro.api.admin


import uk.ac.ox.softeng.mauro.domain.email.Email
import uk.ac.ox.softeng.mauro.domain.security.CatalogueUser
import uk.ac.ox.softeng.mauro.plugin.EmailPlugin
import uk.ac.ox.softeng.mauro.plugin.exporter.ModelExporterPlugin
import uk.ac.ox.softeng.mauro.plugin.importer.ModelImporterPlugin
import uk.ac.ox.softeng.mauro.web.ListResponse

import groovy.transform.CompileStatic
import io.micronaut.context.annotation.Primary
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Header
import io.micronaut.http.annotation.Post
import io.micronaut.http.client.annotation.Client

@CompileStatic
@Client('${micronaut.http.services.mauro.url}/admin')
@Header(name='apiKey', value = '${micronaut.http.services.mauro.apikey}')
@Primary
interface AdminApi {

    @Get('/modules')
    List<LinkedHashMap<String, String>> modules()


    @Get('/providers/importers')
    List<ModelImporterPlugin> importers()


    @Get('/providers/exporters')
    List<ModelExporterPlugin> exporters()

    @Get('/providers/emailers')
    List<EmailPlugin> emailers()


    /**
     * This is new endpoint that can be used to test sending an email.  You should provide a catalogue user with a
     * firstName, lastName, and emailAddress set.
     * @param catalogueUser
     * @return
     */
    @Post('/email/sendTestEmail')
    Boolean sendTestEmail(@Body CatalogueUser catalogueUser)

    /**
     * This is a new endpoint which allows users to test the email connection without sending an email
     * @return
     */
    @Get('/email/testConnection')
    boolean testConnection()


    @Get('/emails')
    ListResponse<Email> list()

    /**
     * This is a new endpoint which allows administrators to retry sending an email (usually one which previously failed to send)
     * @return
     */
    @Post('/emails/{emailId}/retry')
    boolean retryEmail(UUID emailId)
}

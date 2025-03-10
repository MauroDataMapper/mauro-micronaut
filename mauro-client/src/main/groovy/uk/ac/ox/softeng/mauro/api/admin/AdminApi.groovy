package uk.ac.ox.softeng.mauro.api.admin

import uk.ac.ox.softeng.mauro.api.MauroApi
import uk.ac.ox.softeng.mauro.api.Paths
import uk.ac.ox.softeng.mauro.domain.email.Email
import uk.ac.ox.softeng.mauro.domain.security.CatalogueUser
import uk.ac.ox.softeng.mauro.plugin.EmailPlugin
import uk.ac.ox.softeng.mauro.plugin.MauroPluginDTO
import uk.ac.ox.softeng.mauro.plugin.exporter.ModelExporterPlugin
import uk.ac.ox.softeng.mauro.plugin.importer.ModelImporterPlugin
import uk.ac.ox.softeng.mauro.web.ListResponse

import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Post

@MauroApi()
interface AdminApi {

    @Get(Paths.ADMIN_MODULES_LIST)
    List<LinkedHashMap<String, String>> modules()


    @Get(Paths.ADMIN_IMPORTERS_LIST)
    List<MauroPluginDTO> importers()


    @Get(Paths.ADMIN_EXPORTERS_LIST)
    List<MauroPluginDTO> exporters()

    @Get(Paths.ADMIN_EMAILERS_LIST)
    List<MauroPluginDTO> emailers()


    /**
     * This is new endpoint that can be used to test sending an email.  You should provide a catalogue user with a
     * firstName, lastName, and emailAddress set.
     * @param catalogueUser
     * @return
     */
    @Post(Paths.ADMIN_EMAIL_SEND_TEST)
    Boolean sendTestEmail(@Body CatalogueUser catalogueUser)

    /**
     * This is a new endpoint which allows users to test the email connection without sending an email
     * @return
     */
    @Get(Paths.ADMIN_EMAIL_TEST_CONNECTION)
    boolean testConnection()


    @Get(Paths.ADMIN_EMAILS)
    ListResponse<Email> listEmails()

    /**
     * This is a new endpoint which allows administrators to retry sending an email (usually one which previously failed to send)
     * @return
     */
    @Post(Paths.ADMIN_EMAIL_RETRY)
    boolean retryEmail(UUID emailId)
}

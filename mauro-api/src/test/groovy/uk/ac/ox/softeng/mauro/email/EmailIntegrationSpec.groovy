package uk.ac.ox.softeng.mauro.email

import uk.ac.ox.softeng.mauro.api.admin.AdminApi
import uk.ac.ox.softeng.mauro.plugin.MauroPluginDTO

import io.micronaut.runtime.EmbeddedApplication
import jakarta.inject.Inject

import uk.ac.ox.softeng.mauro.domain.email.Email
import uk.ac.ox.softeng.mauro.domain.security.CatalogueUser
import uk.ac.ox.softeng.mauro.persistence.ContainerizedTest
import uk.ac.ox.softeng.mauro.plugin.EmailPlugin
import uk.ac.ox.softeng.mauro.testing.CommonDataSpec
import uk.ac.ox.softeng.mauro.web.ListResponse

import jakarta.inject.Singleton

@ContainerizedTest
@Singleton
class EmailIntegrationSpec extends CommonDataSpec {

    void 'test get email providers'() {
        when:
        List<MauroPluginDTO> emailPlugins = adminApi.emailers()
        then:
        emailPlugins.size() == 1
        emailPlugins.find {it.displayName == 'Mock Email Plugin'}
    }

    void 'test email connection'() {
        when:
        boolean response = adminApi.testConnection()

        then:
        response

    }
    void 'test send test email'() {
        given:
        CatalogueUser testUserSucceed = new CatalogueUser(firstName: 'Succeed', lastName: 'User', emailAddress: 'pass@test.com')
        CatalogueUser testUserFail = new CatalogueUser(firstName: 'Fail', lastName: 'User', emailAddress: 'fail@test.com')

        when:
        boolean response = adminApi.sendTestEmail(testUserSucceed)
        then:
        response

        when:
        adminApi.sendTestEmail(testUserFail)
        then:
        def e = thrown(Exception)
        e.message == "Internal Server Error"

        when:
        ListResponse<Email> allEmails = adminApi.listEmails()

        then:
        allEmails.count == 2
        allEmails.items.count {Email it -> it.successfullySent} == 1
        allEmails.items.count {Email it -> !it.successfullySent} == 1

        UUID failedEmailId = allEmails.items.find {Email it -> !it.successfullySent}?.id

        // Try and resend it - assume fail again
        when:
        adminApi.retryEmail(failedEmailId)
        then:
        e = thrown(Exception)
        e.message == "Internal Server Error"

        when:
        allEmails = adminApi.listEmails()
        allEmails.bindItems(objectMapper, Email)

        then:
        allEmails.count == 2
        allEmails.items.count {Email it -> it.successfullySent} == 1
        allEmails.items.count {Email it -> !it.successfullySent} == 1

    }


}

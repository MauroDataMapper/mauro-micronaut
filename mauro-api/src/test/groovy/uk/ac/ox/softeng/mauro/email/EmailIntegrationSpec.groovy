package uk.ac.ox.softeng.mauro.email


import io.micronaut.runtime.EmbeddedApplication
import jakarta.inject.Inject

import uk.ac.ox.softeng.mauro.domain.email.Email
import uk.ac.ox.softeng.mauro.domain.security.CatalogueUser
import uk.ac.ox.softeng.mauro.persistence.ContainerizedTest
import uk.ac.ox.softeng.mauro.plugin.EmailPlugin
import uk.ac.ox.softeng.mauro.testing.CommonDataSpec
import uk.ac.ox.softeng.mauro.web.ListResponse

@ContainerizedTest
class EmailIntegrationSpec extends CommonDataSpec {

    @Inject
    EmbeddedApplication<?> application


    void 'test get email providers'() {
        when:
        List<EmailPlugin> emailPlugins = GET('admin/providers/emailers', List, EmailPlugin)
        then:
        emailPlugins.size() == 1
        emailPlugins.find {it.displayName == 'Mock Email Plugin'}
    }

    void 'test email connection'() {
        when:
        boolean response = GET("admin/email/testConnection", Boolean)

        then:
        response

    }
    void 'test send test email'() {
        given:
        CatalogueUser testUserSucceed = new CatalogueUser(firstName: 'Succeed', lastName: 'User', emailAddress: 'pass@test.com')
        CatalogueUser testUserFail = new CatalogueUser(firstName: 'Fail', lastName: 'User', emailAddress: 'fail@test.com')

        when:
        boolean response = POST("admin/email/sendTestEmail", testUserSucceed, Boolean)
        then:
        response

        when:
        POST("admin/email/sendTestEmail", testUserFail, Boolean)
        then:
        def e = thrown(Exception)
        e.message == "Internal Server Error"

        when:
        ListResponse<Email> allEmails = (ListResponse<Email>) GET('admin/emails', ListResponse, Email)

        then:
        allEmails.count == 2
        allEmails.items.count {Email it -> it.successfullySent} == 1
        allEmails.items.count {Email it -> !it.successfullySent} == 1

        UUID failedEmailId = allEmails.items.find {Email it -> !it.successfullySent}?.id

        // Try and resend it - assume fail again
        when:
        POST("admin/emails/${failedEmailId}/retry", null, Boolean)
        then:
        e = thrown(Exception)
        e.message == "Internal Server Error"

        when:
        allEmails = GET('admin/emails', ListResponse, Email)
        allEmails.bindItems(objectMapper, Email)

        then:
        allEmails.count == 2
        allEmails.items.count {Email it -> it.successfullySent} == 1
        allEmails.items.count {Email it -> !it.successfullySent} == 1


    }


}

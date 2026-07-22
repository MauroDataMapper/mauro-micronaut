package org.maurodata.controller

import org.maurodata.audit.Audit

import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpStatus
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Consumes
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Post
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.annotation.Client
import io.micronaut.http.client.exceptions.HttpClientResponseException
import io.micronaut.http.client.multipart.MultipartBody
import io.micronaut.context.annotation.Property
import io.micronaut.scheduling.TaskExecutors
import io.micronaut.scheduling.annotation.ExecuteOn
import io.micronaut.security.annotation.Secured
import io.micronaut.security.rules.SecurityRule
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import jakarta.inject.Inject
import reactor.core.publisher.Flux
import spock.lang.Specification

@MicronautTest
@Property(name = 'micronaut.server.max-request-size', value = '4096')
@Property(name = 'micronaut.server.multipart.enabled', value = 'true')
@Property(name = 'micronaut.server.multipart.max-file-size', value = '512')
class ContentLengthExceededExceptionHandlerSpec extends Specification {

    @Inject
    @Client('/')
    HttpClient client

    void 'request bodies larger than max request size return Mauro error response'() {
        given:
        String body = 'x' * 8192

        when:
        client.toBlocking().retrieve(
            HttpRequest.POST('/test-content-length/plain', body).contentType(MediaType.TEXT_PLAIN_TYPE),
            Map)

        then:
        HttpClientResponseException exception = thrown()
        exception.status == HttpStatus.REQUEST_ENTITY_TOO_LARGE

        and:
        Map responseBody = exception.response.getBody(Map).get()
        responseBody.message == HttpStatus.REQUEST_ENTITY_TOO_LARGE.reason
        responseBody.total == 1
        responseBody.errors.first().message.contains('exceeds the maximum allowed content length')
        responseBody._embedded.errors.first().message == responseBody.errors.first().message
    }

    void 'multipart files larger than max file size return Mauro error response'() {
        given:
        byte[] content = ('x' * 1024).bytes
        MultipartBody body = MultipartBody.builder()
            .addPart('file', 'large.txt', MediaType.TEXT_PLAIN_TYPE, content)
            .build()

        when:
        client.toBlocking().retrieve(
            HttpRequest.POST('/test-content-length/multipart', body).contentType(MediaType.MULTIPART_FORM_DATA_TYPE),
            Map)

        then:
        HttpClientResponseException exception = thrown()
        exception.status == HttpStatus.REQUEST_ENTITY_TOO_LARGE

        and:
        Map responseBody = exception.response.getBody(Map).get()
        responseBody.message == HttpStatus.REQUEST_ENTITY_TOO_LARGE.reason
        responseBody.total == 1
        responseBody.errors.first().message.contains('exceeds the maximum allowed content length')
        responseBody._embedded.errors.first().message == responseBody.errors.first().message
    }

}

@Controller('/test-content-length')
@Secured(SecurityRule.IS_ANONYMOUS)
class TestContentLengthController {

    @Post(uri = '/plain', consumes = MediaType.TEXT_PLAIN)
    @Audit(level = Audit.AuditLevel.FILE_ONLY)
    Map<String, Object> plain(@Body String body) {
        [size: body.length()]
    }

    @Post(uri = '/multipart', consumes = MediaType.MULTIPART_FORM_DATA)
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @ExecuteOn(TaskExecutors.BLOCKING)
    @Audit(level = Audit.AuditLevel.FILE_ONLY)
    Map<String, Object> multipart(@Body io.micronaut.http.server.multipart.MultipartBody body) {
        int partCount = Flux.from(body).count().block().intValue()
        [parts: partCount]
    }
}

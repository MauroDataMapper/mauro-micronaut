package uk.ac.ox.softeng.mauro.controller.admin

import uk.ac.ox.softeng.mauro.domain.Email
import uk.ac.ox.softeng.mauro.domain.folder.Folder
import uk.ac.ox.softeng.mauro.domain.model.AdministeredItem
import uk.ac.ox.softeng.mauro.domain.model.Model
import uk.ac.ox.softeng.mauro.persistence.EmailRepository
import uk.ac.ox.softeng.mauro.web.ListResponse

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.runtime.EmbeddedApplication
import jakarta.inject.Inject
import reactor.core.publisher.Mono

import java.util.function.BiFunction

@Slf4j
@CompileStatic
@Controller('/admin')

class AdminController {

    @Inject
    EmbeddedApplication application

    @Inject
    EmailRepository emailRepository

    @Get('/restart')
    void restart() {
        System.err.println(application.properties)
        application.refresh()
    }

    @Get('/emails')
    Mono<ListResponse<Email>> listAll() {
        emailRepository.findAll().collectList().map {List<Email> emails ->
            ListResponse.from(emails)
        }
    }



}

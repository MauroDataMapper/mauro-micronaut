package uk.ac.ox.softeng.mauro.controller.security.openidconnect

import groovy.transform.CompileStatic
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.security.annotation.Secured
import io.micronaut.security.rules.SecurityRule
import io.micronaut.views.View

import static io.micronaut.security.rules.SecurityRule.IS_AUTHENTICATED

@Controller
@CompileStatic
class HomeController {
    @Secured(SecurityRule.IS_ANONYMOUS)
    @View("home")
    @Get
    public Map<String, Object> index() {
        [:]
    }


    @Secured(IS_AUTHENTICATED)
    @Get("/secure")
    public Map<String, Object> secured() {
        Collections.singletonMap("secured", true) as Map<String, Object>
    }
}

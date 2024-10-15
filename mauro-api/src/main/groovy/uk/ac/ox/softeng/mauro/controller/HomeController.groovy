package uk.ac.ox.softeng.mauro.controller

import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.security.annotation.Secured
import io.micronaut.security.rules.SecurityRule

@Controller
class HomeController {

    @Secured(SecurityRule.IS_ANONYMOUS)

    @Get
    public Map<String, Object> index() {
        return new HashMap<>();
    }
}

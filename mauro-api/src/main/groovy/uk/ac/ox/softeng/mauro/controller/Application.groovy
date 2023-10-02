package uk.ac.ox.softeng.mauro.controller

import io.micronaut.runtime.Micronaut
import groovy.transform.CompileStatic
import io.swagger.v3.oas.annotations.OpenAPIDefinition
import io.swagger.v3.oas.annotations.info.Info

@OpenAPIDefinition(
    info = @Info(
        title = 'Mauro Micronaut Terminologies',
        version = '0.0',
        description = 'Mauro Terminologies API in Micronaut'
    )
)
@CompileStatic
class Application {

    static void main(String[] args) {
        Micronaut.run(Application, args)
    }
}

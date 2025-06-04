package uk.ac.ox.softeng.mauro.controller

import groovy.transform.CompileStatic
import io.micronaut.runtime.Micronaut
import io.swagger.v3.oas.annotations.OpenAPIDefinition
import io.swagger.v3.oas.annotations.info.Info

@OpenAPIDefinition(
    info = @Info(
        title = 'Mauro',
        version = '0.0',
        description = 'Mauro REST API (Micronaut)'
    )
)
@CompileStatic
class Application {

    static void main(String[] args) {
        Micronaut.run(Application, args)
    }
}

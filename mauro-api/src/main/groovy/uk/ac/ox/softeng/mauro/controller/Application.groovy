package uk.ac.ox.softeng.mauro.controller

import io.micronaut.context.ApplicationContextBuilder
import io.micronaut.context.ApplicationContextConfigurer
import io.micronaut.context.annotation.ContextConfigurer
import io.micronaut.core.annotation.NonNull
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
    @ContextConfigurer
    static class Configurer implements ApplicationContextConfigurer {
        @Override
        public void configure(@NonNull ApplicationContextBuilder builder) {
            builder.defaultEnvironments("dev");
        }
    }
    static void main(String[] args) {
        Micronaut.run(Application, args)
    }
}

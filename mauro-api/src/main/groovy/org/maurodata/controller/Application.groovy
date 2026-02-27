package org.maurodata.controller

import groovy.transform.CompileStatic
import io.micronaut.openapi.annotation.OpenAPIGroupInfo
import io.micronaut.runtime.Micronaut
import io.swagger.v3.oas.annotations.OpenAPIDefinition
import io.swagger.v3.oas.annotations.info.Contact
import io.swagger.v3.oas.annotations.info.Info
import io.swagger.v3.oas.annotations.info.License
import io.swagger.v3.oas.annotations.servers.Server


@OpenAPIDefinition(
    info = @Info(
        title = "Mauro",
        version = "1.0"
    ), servers = @Server(url = "http://localhost:8080")
)
@OpenAPIGroupInfo(
    names = "v1",
    info = @OpenAPIDefinition(
        info = @Info(
            title = "Mauro",
            version = "1.0",
            description = "This is Mauro API version 1",
            license = @License(name = "Apache 2.0", url = "https://github.com/MauroDataMapper/mauro-micronaut/blob/develop/NOTICE"),
            contact = @Contact(url = "https://maurodatamapper.github.io/", name = "MDM")
        )
    )
)
@CompileStatic
class Application {

    static void main(String[] args) {
        Micronaut.run(Application, args)
    }
}

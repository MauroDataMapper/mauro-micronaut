package org.maurodata

import uk.ac.ox.softeng.mauro.api.admin.AdminApi

import io.micronaut.configuration.picocli.PicocliRunner
import jakarta.inject.Inject
import org.slf4j.Logger
import picocli.CommandLine.Command

import static org.slf4j.LoggerFactory.getLogger


@Command(name="my-cli-app")
abstract class ApiClientTest implements Runnable {

    static final Logger LOG = getLogger(ApiClientTest)

    @Inject AdminApi adminApi

    static void main(String[] args) {
        PicocliRunner.run(ApiClientTest, args)
    }

    abstract void run()


}

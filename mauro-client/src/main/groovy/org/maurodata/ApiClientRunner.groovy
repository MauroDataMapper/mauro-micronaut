package org.maurodata

import io.micronaut.configuration.picocli.PicocliRunner

import org.slf4j.Logger
import picocli.CommandLine.Command

import java.lang.invoke.MethodHandles

import static org.slf4j.LoggerFactory.getLogger

@Command(name="my-cli-app")
abstract class ApiClientRunner extends ApiClient implements Runnable  {

    protected static final Logger log = getLogger(ApiClientRunner)

    static void main(String[] args) {
        PicocliRunner.run(ApiClientRunner, args)
    }

    abstract void run()

}

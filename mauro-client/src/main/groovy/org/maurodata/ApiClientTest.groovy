package org.maurodata

import uk.ac.ox.softeng.mauro.api.admin.AdminApi
import uk.ac.ox.softeng.mauro.api.datamodel.DataModelApi
import uk.ac.ox.softeng.mauro.api.folder.FolderApi

import io.micronaut.configuration.picocli.PicocliRunner
import io.micronaut.http.client.HttpClient
import jakarta.inject.Inject
import org.slf4j.Logger
import picocli.CommandLine.Command

import static org.slf4j.LoggerFactory.getLogger


@Command(name="my-cli-app")
abstract class ApiClientTest implements Runnable {

    static final Logger LOG = getLogger(ApiClientTest)

    @Inject AdminApi adminApi
    @Inject DataModelApi dataModelApi
    @Inject FolderApi folderApi

    @Inject HttpClient client

    static void main(String[] args) {
        System.err.println(AdminApi.class.annotations)

        PicocliRunner.run(ApiClientTest, args)
    }

    abstract void run()


}

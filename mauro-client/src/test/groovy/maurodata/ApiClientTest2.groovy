package maurodata

import io.micronaut.http.client.HttpClient
import jakarta.inject.Singleton
import org.maurodata.ApiClientTest
import picocli.CommandLine.Command

@Command(name="my-cli-app")
@Singleton
class ApiClientTest2 extends ApiClientTest {

    @Override
    void run() {
        System.err.println(ApiClientTest2.class.annotations)


        client = HttpClient.create(new URL("http://localhost:8080"))
        System.err.println(client.properties)
        LOG.error("Here")
        adminApi.list().items.each {email ->
            System.err.println(email.subject)
        }
        folderApi.listAll().items.each {folder ->
            System.err.println(folder.label)
        }
    }

}

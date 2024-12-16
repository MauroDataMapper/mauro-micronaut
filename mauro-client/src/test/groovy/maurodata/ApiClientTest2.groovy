package maurodata

import jakarta.inject.Singleton
import org.maurodata.ApiClientTest
import picocli.CommandLine.Command

@Command(name="my-cli-app")
@Singleton
class ApiClientTest2 extends ApiClientTest {

    @Override
    void run() {
        LOG.error("Here")
        System.err.println(adminApi.list().count)
    }

}

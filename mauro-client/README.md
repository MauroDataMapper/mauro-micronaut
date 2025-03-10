# Creating a Mauro Client Application

The Mauro declarative client defined by this project should make it easy to interact with an existing Mauro application.  
To create a new client application in a new Groovy project, follow the steps below:

## Add the client dependencies - build.gradle

The `build.gradle` at the root level: you need to include the micronaut library in the top-level plugins section: 

```aiignore
plugins {
    id 'groovy'
    id 'io.micronaut.library' version '4.4.5'
}
```

The following dependencies seem to be the minimum requirement for building:

```groovy
repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    implementation 'org.apache.groovy:groovy:4.0.14'
    
    implementation 'uk.ac.ox.softeng.mauro:mauro-client:0.0.1-SNAPSHOT'
    implementation 'uk.ac.ox.softeng.mauro:mauro-domain:0.0.1-SNAPSHOT'
}
```
This will bring in the domain model and the generated declarative client.  Obviously the version numbers will need replacing to match the latest build / release 
of Mauro.

## Micronaut connection configuration - application.yml

The application.yml is the configuration file for Micronaut - it needs to go in `src/main/resources`.  

The application name can be anything; the `url` and `apiKey` define the connection details for the Mauro instance to connect to.  
Username / password combinations may be available in the future.

```yaml
micronaut:
    application:
        name: mauro-client
    http:
        services:
            mauro:
                url: http://localhost:8088
                apikey: 02586cec-20ba-4fb3-99f5-2676f9ca4649
```

## A simple example application

About the simplest example of a client application:

```groovy
import jakarta.inject.Singleton

@Singleton
class SimpleApplication1 extends ApiClient {

    @Override
    void run() {
        folderApi.listAll().each {
            log.info(it.label)
        }
    }
}
```
This application extends the Mauro `ApiClient` that already defines a `public static void main()` method, so this class should be runnable.

_Note that the `@Singleton` annotation is required to make sure the correct ApiClient Bean is configured, and this must be the annotation from the `jakarta.inject` package_

The `run()` method is where the application starts - it is wired up as part of the [PicoCli](https://micronaut-projects.github.io/micronaut-picocli/5.6.0/guide/) setup.

This method connects to the Mauro instance, calls the API to retrieve the list of all folders, and then for each, prints its name.

Note that at the moment it's difficult to have two applications in the same classpath - you'll need to use the `@Primary` annotation [described here](https://docs.micronaut.io/4.1.4/api/io/micronaut/context/annotation/Primary.html)

## A (slightly) more complicated example

```groovy
import uk.ac.ox.softeng.mauro.domain.datamodel.DataModel
import uk.ac.ox.softeng.mauro.domain.folder.Folder

import jakarta.inject.Singleton

@Singleton
class SimpleApplication2 extends ApiClient {

    @Override
    void run() {
        Folder folder = folderApi.listAll().items.first()

        if(folder) {
            (1..10).each {index ->
                dataModelApi.create(folder.id, DataModel.build {
                    label "Data Model $index"
                })
            }
            dataModelApi.list(folder.id).each {dataModel ->
                log.info(dataModel.label)
            }
        }
    }
}
```
This application connects to a Mauro instance and retrieves the list of folders, taking the first one.  If there is such a folder, it creates 
ten data models in that folder, with different labels.  It then retrieves all the data models in that folder and prints their labels.

Note that this uses the Mauro DSL to build a DataModel (`DataModel.build{}`) - there are other ways to do this. 


## Optional - configure the logging outputs

The default logging is very verbose.  The following `logback.xml` file in `src/main/resources` will calm things down:

```xml
<configuration>
    <appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%cyan(%d{HH:mm:ss.SSS}) %green([%thread]) %highlight(%-5level) %magenta(%logger{36}) - %msg%n</pattern>
        </encoder>
    </appender>

    <root level="error">
        <appender-ref ref="STDOUT" />
    </root>

    <logger name="org.maurodata" level="info" />
</configuration>
```
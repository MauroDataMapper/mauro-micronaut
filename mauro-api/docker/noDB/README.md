# Docker

## Building the image
With your Docker Engine running, create an image via:

    # ./gradlew dockerBuildNoDatabase

for a local architecture image, or

    # ./gradlew dockerBuildMultiArchNoDatabase

for a dual amd64/arm64 image

## Running the container with configuration
The container is configured to have these mount points:

    /opt/init
    /var/logs

*/opt/init* is where the container start-up can be controlled from.
There is one sub-directory under */opt/init*:

    micronaut/

which will be used for the start-up of that service.

From *micronaut/* any *.sh* scripts will be run, *.jar* files will be added to the classpath,
and any other files will be copied to Micronaut's *resources/* directory. Use this mechanism to
include any plugins as *.jar* files, and set your own version of *application.yml*

### Example /opt/init

    /opt/init
    | - micronaut
        | - application.yml
        | - myplugin.jar

### Bootstrapping users, groups and api keys

Before you can sign in to Mauro, you need configure a user.
This is done via the application.yml file that is passed to the container via */opt/init/micronaut/application.yml*
to configure Micronaut. See above.

    users:
        -   email: admin@maurodatamapper.com
            first-name: admin
            last-name: admin
            temp-password: mypassword
    groups:
        -   name: Administrators
            isAdmin: true
            members:
                - admin@maurodatamapper.com
    api-keys:
        -   name: My first API Key
            email: admin@maurodatamapper.com
            refreshable: true
            expiry: 2025-12-31

### Running the image

Point the container at your *init/* directory, and expose the port micronaut is running on:

    # docker run --rm -p 8080:8080 -v /path/to/your/init:/opt/init:ro -it maurodatamapper/mauro:0.0.2-beta

## Pointing the container to a database

...
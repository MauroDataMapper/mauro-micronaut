# Docker

## Building the container
With your Docker Engine running, create a container via:

    # ./gradlew dockerBuild

## Running the container with configuration
The container is configured to have these mount points:

    /opt/init
    /var/lib/postgresql/data
    /var/logs
    /var/logs/postgres
    /database


*/opt/init* is where the container start-up can be controlled from.
There are two sub-directories under */opt/init*:

    postgres/
    micronaut/

which will be used for the start-up of those services.

From *postgres/* any *.sh* scripts will be run, and any *.sql* will be run in postgres.

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

### Running the container

Point the container at your *init/* directory, and expose the port micronaut is running on:

    # docker run --rm -p 8080:8080 -v /path/to/your/init:/opt/init:ro -it mauro-api:latest

To persist the data between shutdown and startup, you must also connect the container to */var/lib/postgresql/data* as read/write.

## Running the container with existing data

Either import *.sql* scripts are present in */opt/init/postgres* to be imported at startup, or a pre-existing postgres database
is present in */var/lib/postgresql/data*. In both these cases, make sure that the datasource for postgres matches up with the
*application.yml* configuration


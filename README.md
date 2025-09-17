# Mauro Data Mapper
 
This is the backend for the Mauro Data Mapper application. 
For more information about the application, see the
[Mauro Data Mapper website](https://maurodatamapper.github.io/).

If you are interested in installing an instance of the application, 
please follow the instructions on the website for installing a sandbox or 
production-ready version via Docker.

## Development

The `mauro-micronaut` repository is a Gradle project, with code written in Groovy.  It uses 
the [Micronaut](https://micronaut.io/) framework, including:

- [Micronaut Data](https://micronaut-projects.github.io/micronaut-data/latest/guide/) 
- for persisting data to a Postgres database (using [JDBC](https://micronaut-projects.github.io/micronaut-data/latest/guide/#dbc))
- [Micronaut Security](https://micronaut-projects.github.io/micronaut-security/latest/guide/) 
for authentication and authorization
- [Micronaut HTTP Client](https://docs.micronaut.io/latest/guide/index.html#httpClient) 
for building an API client
- [Micronaut Flyway](https://micronaut-projects.github.io/micronaut-flyway/latest/guide/) 
for database migrations
- [Micronaut Test](https://micronaut-projects.github.io/micronaut-test/latest/guide/) 
for testing
- [Micronaut OpenAPI](https://micronaut-projects.github.io/micronaut-openapi/latest/guide/)
  for generating OpenAPI documentation


### Architecture

The back-end application comprises four subprojects:

- `mauro-domain`: The 'domain layer' of Mauro which defines the underlying data model and 
additional Mauro concepts such as plugins, profiles, etc.
- `mauro-persistence`: The code for persisting Mauro objects into a Postgres database, 
including an in-memory caching layer
- `mauro-client`: Interfaces that Micronaut uses to generate client / SDK code for interacting 
with a Mauro API
- `mauro-api`: Controllers and business logic for defining the Mauro API: this is the 
'application layer' of Mauro.

### Running the application

To change the configuration of the application, for example the database connection, the 
port for the application to run on, security settings, etc., edit the `application.yml` file in 
the `src/main/resources` directory of the `mauro-api` project.

You will need a Postgres database - you can specify the connection details in the `application.yml` 
file as described above.

You can use [SDKMan](https://sdkman.io) to ensure that you're running the correct version of 
Java, Groovy and Gradle:

```
sdk env
```

To run the application locally, run the following command from the top-level directory:

```
gradle run
```
If you want to run the web interface, [mdm-ui](https://github.com/MauroDataMapper/mdm-ui), 
you will need to follow the instructions in that repository.

### Status

[![Java CI with Gradle](https://github.com/MauroDataMapper/mauro-micronaut/actions/workflows/gradle.yml/badge.svg)](https://github.com/MauroDataMapper/mauro-micronaut/actions/workflows/gradle.yml)
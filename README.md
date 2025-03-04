## Micronaut 3.8.7 Documentation

- [User Guide](https://docs.micronaut.io/3.8.7/guide/index.html)
- [API Reference](https://docs.micronaut.io/3.8.7/api/index.html)
- [Configuration Reference](https://docs.micronaut.io/3.8.7/guide/configurationreference.html)
- [Micronaut Guides](https://guides.micronaut.io/index.html)
---

- [Shadow Gradle Plugin](https://plugins.gradle.org/plugin/com.github.johnrengelman.shadow)
## Feature data-r2dbc documentation

- [Micronaut Data R2DBC documentation](https://micronaut-projects.github.io/micronaut-data/latest/guide/#dbc)

- [https://r2dbc.io](https://r2dbc.io)


## Feature test-resources documentation

- [Micronaut Test Resources documentation](https://micronaut-projects.github.io/micronaut-test-resources/latest/guide/)


## Feature http-client documentation

- [Micronaut HTTP Client documentation](https://docs.micronaut.io/latest/guide/index.html#httpClient)


## Feature reactor documentation

- [Micronaut Reactor documentation](https://micronaut-projects.github.io/micronaut-reactor/snapshot/guide/index.html)


## Feature r2dbc documentation

- [Micronaut R2DBC documentation](https://micronaut-projects.github.io/micronaut-r2dbc/latest/guide/)

- [https://r2dbc.io](https://r2dbc.io)



## Feature http-security documentation

- [Micronaut Security documentation](https://micronaut-projects.github.io/micronaut-security/latest/guide/)
- Testing OAUTH login/keycloak -
- Re KeycloakIntegrationSpec. -this uses local keycloak standalone instance with client,realm, users. Test is disabled for CI. 
- To run locally, remove @Ignore, bring up keycloak instance via:
-   docker compose up -d  (note docker-compose script may need to be moved to under root )
- This will instantiate keycloak instance with data in imported realm 'realm.json'. KeycloakIntegration test will run successfully.

To bring down local keycloak: 
  docker compose down
Remember to reinstate @Ignore for CI builds

  


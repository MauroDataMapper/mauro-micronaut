package uk.ac.ox.softeng.mauro.persistence


import groovy.transform.AnnotationCollector
import io.micronaut.context.annotation.Property
import io.micronaut.test.extensions.spock.annotation.MicronautTest

import java.lang.annotation.ElementType
import java.lang.annotation.Inherited
import java.lang.annotation.Target

@MicronautTest(startApplication = true)
@Property(name = "datasources.default.driver-class-name",
    value = "org.testcontainers.jdbc.ContainerDatabaseDriver")
@Property(name = "datasources.default.url",
    value = "jdbc:tc:postgresql:16-alpine:///db")
@AnnotationCollector
@Target(ElementType.TYPE)
@Inherited
@interface ContainerizedTest {}

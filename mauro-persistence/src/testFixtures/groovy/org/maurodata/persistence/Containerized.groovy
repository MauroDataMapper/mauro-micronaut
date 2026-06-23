package org.maurodata.persistence

import groovy.transform.AnnotationCollector
import io.micronaut.context.annotation.Property

import java.lang.annotation.ElementType
import java.lang.annotation.Inherited
import java.lang.annotation.Target

@Property(name = "datasources.default.driver-class-name",
    value = "org.testcontainers.jdbc.ContainerDatabaseDriver")
@Property(name = "datasources.default.url",
    value = "jdbc:tc:pgvector:pg16:///db?TC_INITFUNCTION=org.maurodata.persistence.PgVectorTestDatabase::createVectorExtension")
@AnnotationCollector()
@Target(ElementType.TYPE)
@Inherited
@interface Containerized {
}

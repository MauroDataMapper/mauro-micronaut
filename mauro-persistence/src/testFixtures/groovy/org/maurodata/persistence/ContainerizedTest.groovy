package org.maurodata.persistence

import groovy.transform.AnnotationCollector
import io.micronaut.test.extensions.spock.annotation.MicronautTest

import java.lang.annotation.ElementType
import java.lang.annotation.Inherited
import java.lang.annotation.Target

@MicronautTest(startApplication = true)
@Containerized
@AnnotationCollector()
@Target(ElementType.TYPE)
@Inherited
@interface ContainerizedTest {}

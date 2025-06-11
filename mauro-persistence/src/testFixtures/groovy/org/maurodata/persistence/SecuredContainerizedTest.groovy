package org.maurodata.persistence

import groovy.transform.AnnotationCollector
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import jakarta.inject.Singleton

import java.lang.annotation.ElementType
import java.lang.annotation.Inherited
import java.lang.annotation.Target

@MicronautTest(startApplication = true, environments = 'secured')
@Containerized
@AnnotationCollector()
@Target(ElementType.TYPE)
@Inherited
@interface SecuredContainerizedTest {}

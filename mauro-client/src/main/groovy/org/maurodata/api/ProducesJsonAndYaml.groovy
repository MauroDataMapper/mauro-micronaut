package org.maurodata.api

import groovy.transform.CompileStatic
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.Produces

import java.lang.annotation.ElementType
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy
import java.lang.annotation.Target

@CompileStatic
@Produces([MediaType.APPLICATION_JSON, MediaType.APPLICATION_YAML])
@Retention(RetentionPolicy.RUNTIME)
@Target([ElementType.METHOD, ElementType.TYPE])
@interface ProducesJsonAndYaml {
}

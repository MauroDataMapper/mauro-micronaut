package uk.ac.ox.softeng.mauro.api

import groovy.transform.CompileStatic
import io.micronaut.context.annotation.Replaces
import io.micronaut.http.annotation.Header
import io.micronaut.http.client.annotation.Client

import java.lang.annotation.ElementType
import java.lang.annotation.Target


@CompileStatic
@Client('mauro')
//@Client(value = '${micronaut.http.services.mauro.url}', path = '${micronaut.http.services.mauro.path}')
@Header(name='apiKey', value = '${micronaut.http.services.mauro.apikey:}')
@Target(ElementType.TYPE)
@Replaces
@interface MauroApi {
}



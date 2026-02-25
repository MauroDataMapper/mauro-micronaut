package org.maurodata.controller

import groovy.transform.CompileStatic
import io.micronaut.core.async.publisher.Publishers
import io.micronaut.core.order.Ordered
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpStatus
import io.micronaut.http.MutableHttpResponse
import io.micronaut.http.annotation.Filter
import io.micronaut.http.filter.HttpServerFilter
import io.micronaut.http.filter.ServerFilterChain
import io.micronaut.http.hateoas.JsonError
import io.micronaut.http.hateoas.Resource
import org.reactivestreams.Publisher

@CompileStatic
@Filter("/**")
class ErrorSanitisingFilter implements HttpServerFilter {

    @Override
    int getOrder() {
        return Ordered.LOWEST_PRECEDENCE
    }

    @Override
    Publisher<MutableHttpResponse<?>> doFilter(HttpRequest<?> request,
                                               ServerFilterChain chain) {

        return Publishers.map(chain.proceed(request)) {response ->

            if (response.status == HttpStatus.INTERNAL_SERVER_ERROR) {
                response.body.ifPresent {body ->

                    if (body instanceof JsonError) {
                        sanitiseJsonError(body as JsonError, "Internal Server Error")
                    }
                }
            }

            return response
        } as Publisher<MutableHttpResponse<?>>
    }

    private static void sanitiseJsonError(JsonError error, String message) {
        if (error.getMessage() && HttpStatusExceptionHandler.toTrap(error.getMessage())) {
            error.setMessage(message)
        }

        error.getEmbedded().values().forEach {List<Resource> resources ->
            resources.each {resource ->
                if (resource instanceof JsonError) {
                    sanitiseJsonError(resource as JsonError, 'No details')
                }
            }
        }
    }
}
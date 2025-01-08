package org.maurodata

import io.micronaut.http.HttpResponse
import io.micronaut.http.MutableHttpRequest
import io.micronaut.http.annotation.ClientFilter
import io.micronaut.http.filter.ClientFilterChain
import io.micronaut.http.filter.HttpClientFilter
import io.micronaut.http.uri.UriBuilder
import org.reactivestreams.Publisher

//@ClientFilter // Disabled for now
class ApiClientFilter implements HttpClientFilter {

    ApiClientFilter() {
        System.err.println("Initialising Filter!")
    }

    @Override
    Publisher<? extends HttpResponse<?>> doFilter(MutableHttpRequest<?> request, ClientFilterChain chain) {
        System.err.println("Filtering...")
        UriBuilder builder = UriBuilder.of(request.getUri())
        builder.host("localhost")
        builder.port(8080)


        chain.proceed(request.uri(builder.build()))

    }
}

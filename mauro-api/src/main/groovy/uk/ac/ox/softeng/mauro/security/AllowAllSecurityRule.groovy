//package uk.ac.ox.softeng.mauro.security
//
//import groovy.transform.CompileStatic
//import io.micronaut.core.annotation.Nullable
//import io.micronaut.http.HttpRequest
//import io.micronaut.security.authentication.Authentication
//import io.micronaut.security.rules.SecurityRule
//import io.micronaut.security.rules.SecurityRuleResult
//import jakarta.inject.Singleton
//import org.reactivestreams.Publisher
//import reactor.core.publisher.Mono
//
//@CompileStatic
//@Singleton
//class AllowAllSecurityRule implements SecurityRule<HttpRequest<?>> {
//    @Override
//    Publisher<SecurityRuleResult> check(@Nullable HttpRequest<?> request, @Nullable Authentication authentication) {
//        Mono.just(SecurityRuleResult.ALLOWED)
//    }
//
//    @Override
//    int getOrder() {
//        HIGHEST_PRECEDENCE
//    }
//}

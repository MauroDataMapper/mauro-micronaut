package org.maurodata.security

import groovy.transform.CompileStatic
import io.micronaut.context.annotation.Replaces
import io.micronaut.core.annotation.NonNull
import io.micronaut.http.HttpRequest
import io.micronaut.inject.ExecutableMethod
import io.micronaut.management.endpoint.EndpointSensitivityProcessor
import io.micronaut.security.authentication.Authentication
import io.micronaut.security.rules.SecurityRuleResult
import io.micronaut.security.rules.SensitiveEndpointRule
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.reactivestreams.Publisher
import reactor.core.publisher.Mono

@CompileStatic
@Singleton
@Replaces(SensitiveEndpointRule)
class AdminSensitiveEndpointRule extends SensitiveEndpointRule {

    @Inject
    AccessControlService accessControlService

    AdminSensitiveEndpointRule(EndpointSensitivityProcessor endpointSensitivityProcessor) {
        super(endpointSensitivityProcessor)
    }

    @Override
    @NonNull
    protected Publisher<SecurityRuleResult> checkSensitiveAuthenticated(@NonNull HttpRequest<?> request,
                                                                        @NonNull Authentication authentication,
                                                                        @NonNull ExecutableMethod<?, ?> method) {
        return Mono.just(accessControlService.isAdministrator() ? SecurityRuleResult.ALLOWED : SecurityRuleResult.REJECTED)
    }
}
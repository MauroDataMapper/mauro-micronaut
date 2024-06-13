package uk.ac.ox.softeng.mauro.web

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.Introspected
import io.micronaut.core.annotation.Nullable

@CompileStatic
@Introspected
class ChangePassword {

    String newPassword

    @Nullable
    String oldPassword

    @Nullable
    UUID resetToken
}

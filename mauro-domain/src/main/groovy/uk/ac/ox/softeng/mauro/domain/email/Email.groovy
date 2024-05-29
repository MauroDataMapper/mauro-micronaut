package uk.ac.ox.softeng.mauro.domain.email

import groovy.transform.AutoClone
import groovy.transform.CompileStatic
import groovy.transform.MapConstructor
import io.micronaut.core.annotation.Introspected
import io.micronaut.core.annotation.Nullable
import io.micronaut.data.annotation.GeneratedValue
import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity
import io.micronaut.data.annotation.Version

import java.time.OffsetDateTime

@CompileStatic
@AutoClone
@Introspected
@MappedEntity(schema = 'core')
@MapConstructor(includeSuperFields = true, includeSuperProperties = true, noArg = true)
class Email {

    /**
     * The identity of an object.  UUIDs should be universally unique.
     * Identities are usually created when the object is saved in the database, but can be manually set beforehand.
     */
    @Id
    @GeneratedValue
    UUID id

    /**
     * The version of an object - this is an internal number used for persistence purposes
     */
    @Version
    Integer version


    String sentToEmailAddress
    String subject
    String body
    String emailServiceUsed
    OffsetDateTime dateTimeSent
    Boolean successfullySent

    @Nullable
    String failureReason


    /****
     * Methods for building a tree-like DSL
     */
    static Email build(
            Map args,
            @DelegatesTo(value = Email, strategy = Closure.DELEGATE_FIRST) Closure closure = { }) {
        new Email(args).tap(closure)
    }

    static Email build(@DelegatesTo(value = Email, strategy = Closure.DELEGATE_FIRST) Closure closure = { }) {
        build [:], closure
    }

    String sentToEmailAddress(String emailAddress) {
        this.sentToEmailAddress = emailAddress
        this.sentToEmailAddress
    }

    String subject(String subject) {
        this.subject = subject
        this.subject
    }

    String body(String body) {
        this.body = body
        this.body
    }

    String emailServiceUsed(String emailServiceUsed) {
        this.emailServiceUsed = emailServiceUsed
        this.emailServiceUsed
    }

    OffsetDateTime dateTimeSent(OffsetDateTime dateTimeSent) {
        this.dateTimeSent = dateTimeSent
        this.dateTimeSent
    }

    Boolean successfullySent(Boolean successfullySent) {
        this.successfullySent = successfullySent
        this.successfullySent
    }

    String failureReason(String failureReason) {
        this.failureReason = failureReason
        this.failureReason
    }

}

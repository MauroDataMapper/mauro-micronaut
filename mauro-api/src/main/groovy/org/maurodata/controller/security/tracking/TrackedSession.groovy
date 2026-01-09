package org.maurodata.controller.security.tracking

import groovy.transform.CompileStatic

import java.time.Instant

@CompileStatic
class TrackedSession {
    final String id
    final Instant creationDateTime
    volatile Instant lastAccessedDateTime
    volatile String userEmailAddress
    volatile String lastAccessedUrl

    static final String UNLOGGED_USER_EMAIL="unlogged_user@mdm-core.com"

    TrackedSession(String id) {
        this.id = id
        this.creationDateTime = Instant.now()
        this.lastAccessedDateTime = this.creationDateTime
        this.userEmailAddress = UNLOGGED_USER_EMAIL
        this.lastAccessedUrl = ""
    }
}

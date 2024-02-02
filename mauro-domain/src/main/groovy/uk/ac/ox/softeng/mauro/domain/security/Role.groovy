package uk.ac.ox.softeng.mauro.domain.security

import groovy.transform.CompileStatic

@CompileStatic
enum Role {
    READER,
    REVIEWER,
    AUTHOR,
    EDITOR,
    CONTAINER_ADMIN
}
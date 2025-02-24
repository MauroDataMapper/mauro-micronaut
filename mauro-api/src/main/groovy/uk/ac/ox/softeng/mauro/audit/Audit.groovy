package uk.ac.ox.softeng.mauro.audit

import uk.ac.ox.softeng.mauro.domain.facet.EditType

import io.micronaut.aop.Around

import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy

@Around
@Retention(RetentionPolicy.RUNTIME)
@interface Audit {
    String description()

    EditType title()

}
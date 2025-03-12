package uk.ac.ox.softeng.mauro.audit

import uk.ac.ox.softeng.mauro.domain.facet.EditType
import uk.ac.ox.softeng.mauro.domain.model.AdministeredItem

import io.micronaut.aop.Around

import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy

@Around
@Retention(RetentionPolicy.RUNTIME)
@interface Audit {
    String description() default ''
    EditType title() default EditType.NO_TYPE


    String parentDomainType() default ''

}
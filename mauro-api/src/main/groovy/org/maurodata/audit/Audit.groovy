package org.maurodata.audit

import org.maurodata.domain.facet.EditType
import org.maurodata.domain.model.AdministeredItem

import io.micronaut.aop.Around

import java.lang.annotation.ElementType
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy
import java.lang.annotation.Target


/*
This is the annotation that enables and configures audit logging for each endpoint
By default:

GET -       will be logged to file, and only if `mauro.audit.scope` is set to `ALL`.
            Don't add this annotation if you don't want logging
POST, PUT - will be logged with a database entry by default, as well as a file entry.
            To just log to file, choose `FILE_ONLY`; to never audit use the `NEVER_AUDIT` option
            Change the description and title as appropriate
DELETE    - As above for POST / PUT.  When logging to the database, choose the parent domain type
            so that the appropriate item can be logged against.
*/

@Around
@Retention(RetentionPolicy.RUNTIME)
@interface Audit {

    enum AuditLevel {
        NEVER_AUDIT,  // For POST / PUT / DELETE to positively confirm that no logging is necessary
        FILE_ONLY, // For POST / PUT / DELETE where no database logging is required
        FILE_AND_DB_ENTRY // Default, no need to set
    }

    // For POST / PUT / DELETE where no database logging, or logging of any kind, is required
    AuditLevel level() default AuditLevel.FILE_AND_DB_ENTRY

    // For POST / PUT / DELETE where database logging is required
    String description() default ''
    EditType title() default EditType.VIEW

    // where and how to add an edit when deleting something
    Class deletedObjectDomainType() default Object
    Class parentDomainType() default Object
    String parentIdParamName() default "parentId"
}
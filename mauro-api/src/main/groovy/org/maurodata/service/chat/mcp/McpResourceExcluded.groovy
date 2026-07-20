package org.maurodata.service.chat.mcp

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.Indexed
import java.lang.annotation.ElementType
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy
import java.lang.annotation.Target

@Retention(RetentionPolicy.RUNTIME)
@Target([ElementType.TYPE, ElementType.METHOD])
@Indexed(McpResourceExcluded)
@CompileStatic
@interface McpResourceExcluded {
    String value() default ''
}

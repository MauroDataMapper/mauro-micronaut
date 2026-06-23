package org.maurodata.service.chat.mcp

import io.micronaut.core.annotation.Indexed

import java.lang.annotation.ElementType
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy
import java.lang.annotation.Target

@Retention(RetentionPolicy.RUNTIME)
@Target([ElementType.TYPE, ElementType.METHOD])
@Indexed(McpResourceExcluded)
@interface McpResourceExcluded {
    String value() default ''
}

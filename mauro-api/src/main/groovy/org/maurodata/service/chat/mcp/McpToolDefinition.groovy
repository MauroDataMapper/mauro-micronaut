package org.maurodata.service.chat.mcp

import java.lang.annotation.ElementType
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy
import java.lang.annotation.Target

@Retention(RetentionPolicy.RUNTIME)
@Target([ElementType.TYPE, ElementType.METHOD])
@interface McpToolDefinition {
    String name()
    String description()
    String inputSchema() default '{}'
    String purpose() default ''
    String[] useWhen() default []
    String[] avoidWhen() default []
    String[] examples() default []
    String[] syntax() default []
    String[] filtering() default []
    String[] paging() default []
    String[] limitations() default []
}

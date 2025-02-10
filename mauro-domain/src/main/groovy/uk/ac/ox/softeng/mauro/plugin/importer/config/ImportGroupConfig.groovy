package uk.ac.ox.softeng.mauro.plugin.importer.config

import io.micronaut.core.order.Ordered

import java.lang.annotation.Documented
import java.lang.annotation.ElementType
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy
import java.lang.annotation.Target

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@interface ImportGroupConfig {

    String name() default "Miscellaneous";

    int order() default Ordered.LOWEST_PRECEDENCE;
}

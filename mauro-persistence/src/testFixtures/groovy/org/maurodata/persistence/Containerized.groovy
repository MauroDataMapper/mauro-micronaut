package org.maurodata.persistence

import java.lang.annotation.ElementType
import java.lang.annotation.Inherited
import java.lang.annotation.Target

/** Marks a test that uses the configurable Testcontainers PostgreSQL test datasource. */
@Target(ElementType.TYPE)
@Inherited
@interface Containerized {
}

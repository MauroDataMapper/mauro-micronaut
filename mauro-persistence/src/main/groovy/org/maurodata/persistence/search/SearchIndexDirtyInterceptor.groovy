package org.maurodata.persistence.search

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.micronaut.aop.InterceptorBean
import io.micronaut.aop.MethodInterceptor
import io.micronaut.aop.MethodInvocationContext
import io.micronaut.context.event.ApplicationEventPublisher
import io.micronaut.data.annotation.Repository
import org.maurodata.persistence.facet.EditRepository

@InterceptorBean(Repository) // bind to Micronaut Data repository beans
@CompileStatic
@Slf4j
class SearchIndexDirtyInterceptor implements MethodInterceptor<Object, Object> {

    final ApplicationEventPublisher publisher

    SearchIndexDirtyInterceptor(ApplicationEventPublisher publisher) {
        this.publisher = publisher
    }

    final Set<Class> EXCLUDED_CLASSES = [EditRepository] as Set<Class>

    @Override
    Object intercept(MethodInvocationContext<Object, Object> context) {
        Object result = context.proceed() // perform the DB write

        Class runtimeType = context.getTarget().getClass()

        if (EXCLUDED_CLASSES.find {it.isAssignableFrom(runtimeType)}) {
            return result
        }

        if(isWriteMethod(context.getMethodName())) {
            log.info("Marking database as dirty...")
            log.trace(context.getDeclaringType().toString())
            log.trace(context.getMethodName())
            publisher.publishEvent(new DataChangeEvent(context.getTarget().getClass(), context.methodName, context.arguments))
        }
        return result
    }

    private static boolean isWriteMethod(String method) {
        method.startsWith("save") || method.startsWith("update") ||
        method.startsWith("delete") || method.startsWith("insert")
    }
}

package org.maurodata.persistence.search

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.micronaut.aop.InterceptorBean
import io.micronaut.aop.MethodInterceptor
import io.micronaut.aop.MethodInvocationContext
import io.micronaut.data.annotation.Repository
import org.maurodata.persistence.facet.EditRepository

@InterceptorBean(Repository) // bind to Micronaut Data repository beans
@CompileStatic
@Slf4j
class SearchIndexDirtyInterceptor implements MethodInterceptor<Object, Object> {


    final Set<Class> EXCLUDED_CLASSES = [EditRepository] as Set<Class>

    @Override
    Object intercept(MethodInvocationContext<Object, Object> context) {

        Class runtimeType = context.getTarget().getClass()

        if (EXCLUDED_CLASSES.find {it.isAssignableFrom(runtimeType)}) {
            return context.proceed()
        }

        String method = context.getMethodName()

        // simple heuristic — adjust to your repo naming/usage
        boolean isWrite = method.startsWith("save")
            || method.startsWith("update")
            || method.startsWith("delete")
            || method.startsWith("insert")

        Object result
        try {
            result = context.proceed() // perform the DB write
        } catch (RuntimeException ex) {
            // don't mark dirty on failure
            throw ex
        }

        if (isWrite) {
            // publish a domain event; listener will mark dirty/rebuild index.
            System.err.println("Marking database as dirty...")
            System.err.println(context.getDeclaringType())
            System.err.println(context.getMethodName())
        }

        return result
    }
}
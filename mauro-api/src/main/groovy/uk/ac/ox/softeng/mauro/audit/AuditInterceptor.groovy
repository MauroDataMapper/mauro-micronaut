package uk.ac.ox.softeng.mauro.audit

import uk.ac.ox.softeng.mauro.controller.facet.MetadataController
import uk.ac.ox.softeng.mauro.domain.facet.Edit
import uk.ac.ox.softeng.mauro.domain.facet.EditType
import uk.ac.ox.softeng.mauro.domain.facet.Metadata
import uk.ac.ox.softeng.mauro.domain.model.AdministeredItem
import uk.ac.ox.softeng.mauro.domain.security.CatalogueUser
import uk.ac.ox.softeng.mauro.persistence.cache.FacetCacheableRepository
import uk.ac.ox.softeng.mauro.persistence.facet.EditRepository
import uk.ac.ox.softeng.mauro.persistence.facet.MetadataRepository
import uk.ac.ox.softeng.mauro.security.AccessControlService

import groovy.text.GStringTemplateEngine
import groovy.util.logging.Slf4j
import io.micronaut.aop.InterceptorBean
import io.micronaut.aop.MethodInterceptor
import io.micronaut.aop.MethodInvocationContext
import io.micronaut.context.annotation.Property
import io.micronaut.context.annotation.Value
import io.micronaut.http.annotation.Delete
import io.micronaut.http.annotation.Post
import io.micronaut.http.annotation.Put
import jakarta.inject.Inject
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.slf4j.event.LoggingEvent
import org.slf4j.spi.LoggingEventBuilder


@Slf4j
@InterceptorBean(Audit.class)
class AuditInterceptor implements MethodInterceptor<Object, Object>{

    enum AuditScope {
        ALL, WRITE_ONLY, NONE
    }

    @Value('${mauro.audit.scope}')
    protected AuditScope auditScope = AuditScope.NONE

    Logger logger = LoggerFactory.getLogger("audit")

    @Inject
    EditRepository editRepository

    @Inject
    FacetCacheableRepository.EditCacheableRepository editCacheableRepository

    @Inject
    AccessControlService accessControlService

    @Inject
    MetadataController metadataController


    @Override
    Object intercept(MethodInvocationContext<Object, Object> context) {

        try {
            boolean doFileLog = (auditScope == AuditScope.ALL
                || (auditScope == AuditScope.WRITE_ONLY &&
                    (
                        context.getAnnotation(Post) || context.getAnnotation(Put) || context.getAnnotation(Delete)
                    ))
            )
            if (doFileLog) {
                fileLog(context)
            }
        } catch(Exception e) {
            e.printStackTrace()
        }

        Object result = context.proceed()

        EditType title = context.getAnnotation(Audit).enumValue('title', EditType).get()
        String description = context.getAnnotation(Audit).stringValue('description').get()

        if(result instanceof AdministeredItem && accessControlService.user) {
            editCacheableRepository.save(new Edit(
                multiFacetAwareItemDomainType: result.domainType,
                multiFacetAwareItemId: result.id,
                title: title,
                description: description,
                catalogueUser: new CatalogueUser(id: accessControlService.user.id, emailAddress: accessControlService.user.emailAddress)
            ))
        } else if(!accessControlService.user) {
            log.error("Trying to insert a new edit log but an edit has been made with no user record present!")
        }
        return result
    }

    void fileLog(MethodInvocationContext<Object, Object> context) {
        EditType title = context.getAnnotation(Audit).enumValue('title', EditType).get()
        String description = context.getAnnotation(Audit).stringValue('description').get()
//        String path = ""
//        if(context.getAnnotation(Post) && context.getAnnotation(Post).stringValue('value').isPresent()) {
//            path = context.getAnnotation(Post).stringValue('value').get()
//        }
//        if(context.getAnnotation(Put) && context.getAnnotation(Put).stringValue('value').isPresent()) {
//            path = context.getAnnotation(Put).stringValue('value').get()
//        }
//        if(context.getAnnotation(Delete) && context.getAnnotation(Delete).stringValue('value').isPresent()) {
//            path = context.getAnnotation(Delete).stringValue('value').get()
//        }

        LoggingEventBuilder loggingEventBuilder = logger.atInfo()
//        loggingEventBuilder.addKeyValue('path', path)
        loggingEventBuilder.addKeyValue('className', context.getDeclaringType().simpleName)
        loggingEventBuilder.addKeyValue('methodName', context.getExecutableMethod().name)
        loggingEventBuilder.addKeyValue('title', title)
        loggingEventBuilder.addKeyValue('description', description)
        loggingEventBuilder = loggingEventBuilder.addKeyValue('parameters', context.parameterValueMap)

        if(accessControlService.user) {
            String userEmailAddress = accessControlService.user.emailAddress
            String userId = accessControlService.user.id.toString()
            loggingEventBuilder.addKeyValue('userEmailAddress', userEmailAddress)
            loggingEventBuilder.addKeyValue('userId', userId)
            loggingEventBuilder.addKeyValue('anonymous', false)
        } else {
            loggingEventBuilder.addKeyValue('anonymous', true)
        }
        loggingEventBuilder.log("${context.getDeclaringType().simpleName} : ${context.getExecutableMethod().name}")
    }
}

package uk.ac.ox.softeng.mauro.audit

import uk.ac.ox.softeng.mauro.domain.facet.Edit
import uk.ac.ox.softeng.mauro.domain.facet.EditType
import uk.ac.ox.softeng.mauro.domain.facet.Facet
import uk.ac.ox.softeng.mauro.domain.model.AdministeredItem
import uk.ac.ox.softeng.mauro.domain.security.CatalogueUser
import uk.ac.ox.softeng.mauro.persistence.cache.FacetCacheableRepository
import uk.ac.ox.softeng.mauro.persistence.facet.EditRepository
import uk.ac.ox.softeng.mauro.security.AccessControlService
import uk.ac.ox.softeng.mauro.web.ListResponse

import groovy.util.logging.Slf4j
import io.micronaut.aop.InterceptorBean
import io.micronaut.aop.MethodInterceptor
import io.micronaut.aop.MethodInvocationContext
import io.micronaut.context.annotation.Value
import io.micronaut.http.annotation.Delete
import io.micronaut.http.annotation.Post
import io.micronaut.http.annotation.Put
import jakarta.inject.Inject
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.slf4j.spi.LoggingEventBuilder


@Slf4j
@InterceptorBean(Audit)
class AuditInterceptor implements MethodInterceptor<Object, Object>{

    enum AuditScope {
        ALL, WRITE_ONLY, NONE
    }

    @Value('${mauro.audit.scope}')
    protected AuditScope auditScope = AuditScope.NONE

    Logger auditLogger = LoggerFactory.getLogger("audit")

    @Inject
    EditRepository editRepository

    @Inject
    FacetCacheableRepository.EditCacheableRepository editCacheableRepository

    @Inject
    AccessControlService accessControlService

    @Override
    Object intercept(MethodInvocationContext<Object, Object> context) {
        log.trace("Intercepting...")

        Object result = context.proceed()

        Map<CharSequence, Object> auditConfig = context.getAnnotation(Audit).values
        Boolean neverAudit = (Boolean) auditConfig['neverAudit']
        if(neverAudit) {
            return result
        }

        EditType title = ((EditType) auditConfig['title']) ?: defaultEditType(context)
        String description = (String) auditConfig['description'] ?: defaultDescription(context, result, auditConfig)

        // Do the file log before the method is even called
        boolean doFileLog =
            auditScope == AuditScope.ALL ||
            (auditScope == AuditScope.WRITE_ONLY && isAnUpdateMethod(context))

        if (doFileLog) {
            fileLog(context, title.toString(), description)
        }


        Audit.AuditLevel auditLevel = (Audit.AuditLevel) auditConfig['level']?: Audit.AuditLevel.FILE_AND_DB_ENTRY
        boolean doDbLog =
            isAnUpdateMethod(context) &&
            auditScope != AuditScope.NONE &&
            auditLevel == Audit.AuditLevel.FILE_AND_DB_ENTRY

        if(doDbLog) {
            if (accessControlService.user) {
                // TODO - check that thing is actually the parent
                if (context.getAnnotation(Delete)) {
                    String parentDomainType = ((Class) auditConfig['parentDomainType'])?.simpleName ?:(String) context.getParameterValueMap()["domainType"]
                    UUID parentId = (UUID) (context.getParameterValueMap()["parentId"] ?: context.getParameterValueMap()["domainId"])
                    if(parentDomainType && parentId) {
                        dbLog(parentDomainType, parentId , title.toString(), description)
                    } else {
                        log.error("No logging data found for delete method")
                    }
                } else if (result instanceof AdministeredItem) {
                    dbLog(result.domainType, result.id, title.toString(), description)
                } else if (result instanceof Facet) {
                    UUID domainId = (UUID) context.getParameterValueMap()["domainId"]
                    String domainType = (String) context.getParameterValueMap()["domainType"]
                    dbLog(domainType, domainId, title.toString(), description)
                } else if (result instanceof List<AdministeredItem>) {
                    result.each { item ->
                        if (item instanceof AdministeredItem) {
                            dbLog(item.domainType, item.id, title.toString(), description)
                        } else {
                            log.error("Trying to insert a new edit log but no understandable return type found in list!")
                        }
                    }
                } else if (result instanceof ListResponse) {
                    result.items.each { item ->
                        if (item instanceof AdministeredItem) {
                            dbLog(item.domainType, item.id, title.toString(), description)
                        } else {
                            log.error("Trying to insert a new edit log but no understandable return type found in list response!")
                        }
                    }
                } else {
                    log.error("Trying to insert a new edit log but no understandable return type found!")
                    log.error(result.class.simpleName)
                }
            } else if (!accessControlService.user) {
                log.error("Trying to insert a new edit log but an edit has been made with no user record present!")
            }
        }
        return result
    }

    private void fileLog(MethodInvocationContext<Object, Object> context, String title, String description) {
        String path = ""
        if(context.getAnnotation(Post) && context.getAnnotation(Post).stringValue('value').isPresent()) {
            path = context.getAnnotation(Post).stringValue('value').get()
        }
        if(context.getAnnotation(Put) && context.getAnnotation(Put).stringValue('value').isPresent()) {
            path = context.getAnnotation(Put).stringValue('value').get()
        }
        if(context.getAnnotation(Delete) && context.getAnnotation(Delete).stringValue('value').isPresent()) {
            path = context.getAnnotation(Delete).stringValue('value').get()
        }
        LoggingEventBuilder loggingEventBuilder = auditLogger.atInfo()
        loggingEventBuilder.addKeyValue('path', path)
        loggingEventBuilder.addKeyValue('className', context.getDeclaringType().simpleName)
        loggingEventBuilder.addKeyValue('methodName', context.getExecutableMethod().name)
        loggingEventBuilder.addKeyValue('title', title)
        loggingEventBuilder.addKeyValue('description', description)
        loggingEventBuilder = loggingEventBuilder.addKeyValue('parameters', context.parameterValueMap)

        if(accessControlService.enabled && accessControlService.isUserAuthenticated() && accessControlService.user) {
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

    private void dbLog(String domainType, UUID itemId, String title, String description) {

        // TODO: replace this with something better!
        String actualDomainType = domainType
        if(actualDomainType == "PrimitiveType" || actualDomainType == "EnumerationType") {
            actualDomainType = "DataType"
        }
        System.err.println(actualDomainType)
        editCacheableRepository.save(new Edit(
            multiFacetAwareItemDomainType: actualDomainType,
            multiFacetAwareItemId: itemId,
            title: title,
            description: description,
            catalogueUser: new CatalogueUser(id: accessControlService.user.id, emailAddress: accessControlService.user.emailAddress)
        ))

    }

    private static boolean isAnUpdateMethod(MethodInvocationContext<Object, Object> context) {
        (
            context.getAnnotation(Post) || context.getAnnotation(Put) || context.getAnnotation(Delete)
        )
    }

    private static EditType defaultEditType(MethodInvocationContext<Object, Object> context) {
        switch(annotationType(context)) {
            case "delete": return EditType.DELETE
            case "post": return EditType.CREATE
            case "put": return EditType.UPDATE
            default: return EditType.VIEW
        }
    }

    private static String defaultDescription(MethodInvocationContext<Object, Object> context, Object result, Map<CharSequence, Object> auditConfig) {
        if(!result) {
            return "Call to ${context.methodName}"
        }
        String resultType = result.class.simpleName
        switch(annotationType(context)) {
            case "delete":
                Class deletedObjectDomainType = (Class) auditConfig['deletedObjectDomainType']
                if(deletedObjectDomainType) {
                    return "Deleted ${deletedObjectDomainType.simpleName}"
                }
                return "Deleted $resultType"

            case "post": return "Created $resultType"
            case "put": return "Updated $resultType"
            default: return "Viewed $resultType"
        }
    }

    private static String annotationType(MethodInvocationContext<Object, Object> context) {
        if (context.getAnnotation(Delete)) {
            return "delete"
        }
        if (context.getAnnotation(Post)) {
            return "post"
        }
        if (context.getAnnotation(Put)) {
            return "put"
        }
        return "get"

    }

}

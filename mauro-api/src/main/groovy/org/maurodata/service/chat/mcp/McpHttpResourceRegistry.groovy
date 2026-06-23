package org.maurodata.service.chat.mcp

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.AnnotationMetadata
import io.micronaut.core.type.Argument
import io.micronaut.http.HttpMethod
import io.micronaut.http.MediaType
import io.micronaut.web.router.Router
import io.micronaut.web.router.UriRouteInfo
import jakarta.inject.Singleton

@CompileStatic
@Singleton
class McpHttpResourceRegistry {

    static final String URI_PREFIX = 'mauro-api://http-get'
    static final String OPERATION_URI_PREFIX = 'mauro-api://operation'
    private static final String OPEN_API_OPERATION = 'io.swagger.v3.oas.annotations.Operation'

    private static final List<String> EXCLUDED_PACKAGE_PREFIXES = [
        'org.maurodata.controller.chat.',
        'org.maurodata.controller.admin.',
        'org.maurodata.controller.bootstrap.',
        'org.maurodata.controller.config.',
        'org.maurodata.controller.security.',
        'org.maurodata.controller.importer.',
        'org.maurodata.controller.jobs.',
        'org.maurodata.controller.domainexports.'
    ].asImmutable() as List<String>

    private static final List<String> EXCLUDED_PATH_PREFIXES = [
        '/mcp',
        '/api/chat',
        '/api/admin',
        '/api/session',
        '/api/apiKeys',
        '/oauth',
        '/swagger',
        '/swagger-ui',
        '/health'
    ].asImmutable() as List<String>

    private final Router router

    McpHttpResourceRegistry(Router router) {
        this.router = router
    }

    List<McpHttpResource> listResourceTemplates() {
        listHttpGetResources()
            .findAll {McpHttpResource resource -> resource.template}
    }

    List<McpHttpResource> listConcreteResources() {
        listHttpGetResources()
            .findAll {McpHttpResource resource -> !resource.template}
    }

    List<McpHttpOperation> listOperations() {
        discoverOperations()
    }

    List<McpHttpOperation> listOperations(String resourceType, String operationKind = null) {
        List<McpHttpOperation> operations = discoverOperations()
        if (resourceType != null && !resourceType.trim().isEmpty()) {
            String lower = resourceType.toLowerCase(Locale.ROOT)
            operations = operations.findAll {McpHttpOperation operation ->
                operation.resourceType?.toLowerCase(Locale.ROOT) == lower ||
                    operation.name?.toLowerCase(Locale.ROOT)?.startsWith(lower + '.') ||
                    operation.path?.toLowerCase(Locale.ROOT)?.contains('/' + lower + 's')
            } as List<McpHttpOperation>
        }
        if (operationKind != null && !operationKind.trim().isEmpty()) {
            String lowerKind = operationKind.toLowerCase(Locale.ROOT)
            operations = operations.findAll {McpHttpOperation operation ->
                operation.operationKind?.toLowerCase(Locale.ROOT) == lowerKind
            } as List<McpHttpOperation>
        }
        operations
    }

    McpHttpOperation findOperationByUri(String uri) {
        if (uri == null || !uri.startsWith(OPERATION_URI_PREFIX)) {
            return null
        }
        String remainder = uri.substring(OPERATION_URI_PREFIX.length())
        int slash = remainder.indexOf('/', 1)
        if (!remainder.startsWith('/') || slash < 0) {
            return null
        }
        String methodName = remainder.substring(1, slash).toUpperCase(Locale.ROOT)
        String path = pathWithoutQuery(remainder.substring(slash))
        discoverOperations().find {McpHttpOperation operation ->
            operation.httpMethod == methodName && (operation.path == path || pathMatchesTemplate(path, operation.path))
        }
    }

    McpHttpOperation findOperationByPathAndMethod(String path, String httpMethod) {
        if (path == null || httpMethod == null) {
            return null
        }
        String normalizedPath = pathWithoutQuery(path)
        String normalizedMethod = httpMethod.toUpperCase(Locale.ROOT)
        discoverOperations().find {McpHttpOperation operation ->
            operation.httpMethod == normalizedMethod &&
                (operation.path == normalizedPath || pathMatchesTemplate(normalizedPath, operation.path))
        }
    }

    McpHttpResource findByUri(String uri) {
        String path = pathFromResourceUri(uri)
        if (path == null || path.trim().isEmpty()) {
            return null
        }
        String routePath = pathWithoutQuery(path)
        listHttpGetResources()
            .find {McpHttpResource resource ->
                resource.path == routePath ||
                    resource.resourceUri == uri ||
                    resource.uriTemplate == uri ||
                    (resource.template && pathMatchesTemplate(routePath, resource.path))
            }
    }

    private List<McpHttpResource> listHttpGetResources() {
        discoverOperations()
            .findAll {McpHttpOperation operation -> operation.httpMethod == HttpMethod.GET.name()}
            .collect {McpHttpOperation operation ->
                new McpHttpResource(
                    path: operation.path,
                    resourceUri: URI_PREFIX + operation.path,
                    uriTemplate: URI_PREFIX + operation.path,
                    name: operation.name,
                    description: operation.description,
                    mimeType: operation.mimeType,
                    controller: operation.controller,
                    method: operation.method,
                    template: operation.template
                )
            } as List<McpHttpResource>
    }

    private List<McpHttpOperation> discoverOperations() {
        List<McpHttpOperation> operations = new ArrayList<McpHttpOperation>()
        router.uriRoutes()
            .filter {UriRouteInfo<?, ?> route -> supportedHttpMethod(route.httpMethod)}
            .forEach {UriRouteInfo<?, ?> route ->
                if (isExcluded(route)) {
                    return
                }
                String path = route.uriMatchTemplate.toString()
                String httpMethod = route.httpMethod.name()
                String controllerName = normalizedControllerName(route.declaringType)
                String methodName = route.targetMethod.methodName
                AnnotationMetadata metadata = route.targetMethod.annotationMetadata
                String operationId = annotationString(metadata, 'operationId')
                String summary = annotationString(metadata, 'summary')
                String operationDescription = annotationString(metadata, 'description')
                String kind = operationKind(route, operationId, summary)
                String resourceType = inferResourceType(controllerName, operationId, methodName, path)
                operations.add(new McpHttpOperation(
                    path: path,
                    operationUri: OPERATION_URI_PREFIX + '/' + httpMethod.toLowerCase(Locale.ROOT) + path,
                    name: resourceName(route, path),
                    description: operationDescription ?: resourceDescription(route, path),
                    mimeType: preferredMimeType(route),
                    controller: route.declaringType.name,
                    method: methodName,
                    template: path.contains('{'),
                    httpMethod: httpMethod,
                    operationKind: kind,
                    resourceType: resourceType,
                    operationId: operationId,
                    summary: summary,
                    consumes: mediaTypes(route.consumes),
                    produces: mediaTypes(route.produces),
                    pathParameters: pathParameterNames(path),
                    parameters: argumentSchemas(route.targetMethod.executableMethod.arguments),
                    body: bodySchema(route),
                    responseType: typeName(route.returnType.asArgument())
                ))
            }
        operations
            .unique {McpHttpOperation operation -> "${operation.httpMethod} ${operation.path} ${operation.name}".toString() }
            .sort {McpHttpOperation left, McpHttpOperation right ->
                int pathCompare = left.path <=> right.path
                pathCompare != 0 ? pathCompare : left.httpMethod <=> right.httpMethod
            } as List<McpHttpOperation>
    }

    private static boolean supportedHttpMethod(HttpMethod method) {
        method in [HttpMethod.GET, HttpMethod.POST, HttpMethod.PUT, HttpMethod.DELETE]
    }

    private static String annotationString(AnnotationMetadata metadata, String member) {
        metadata.stringValue(OPEN_API_OPERATION, member).orElse(null)
    }

    private static List<String> mediaTypes(List<MediaType> mediaTypes) {
        (mediaTypes ?: []).collect {MediaType mediaType -> mediaType.toString()} as List<String>
    }

    private static List<String> pathParameterNames(String path) {
        List<String> names = new ArrayList<String>()
        java.util.regex.Matcher matcher = (path =~ /\{([^}]+)}/)
        while (matcher.find()) {
            names.add(matcher.group(1))
        }
        names
    }

    private static List<Map<String, Object>> argumentSchemas(Argument<?>[] arguments) {
        List<Map<String, Object>> schemas = new ArrayList<Map<String, Object>>()
        for (Argument<?> argument : arguments ?: Argument.ZERO_ARGUMENTS) {
            schemas.add([
                name: argument.name,
                type: typeName(argument),
                body: argument.annotationMetadata.hasAnnotation('io.micronaut.http.annotation.Body'),
                query: argument.annotationMetadata.hasAnnotation('io.micronaut.http.annotation.QueryValue'),
                nullable: argument.annotationMetadata.hasAnnotation('io.micronaut.core.annotation.Nullable') ||
                    argument.annotationMetadata.hasAnnotation('jakarta.annotation.Nullable')
            ] as Map<String, Object>)
        }
        schemas
    }

    private static Map<String, Object> bodySchema(UriRouteInfo<?, ?> route) {
        Optional<Argument<?>> bodyArgument = route.bodyArgument
        if (!bodyArgument.present) {
            bodyArgument = route.requestBodyType
        }
        if (!bodyArgument.present) {
            return null
        }
        Argument<?> argument = bodyArgument.get()
        [
            name: argument.name,
            type: typeName(argument),
            required: !argument.annotationMetadata.hasAnnotation('io.micronaut.core.annotation.Nullable') &&
                !argument.annotationMetadata.hasAnnotation('jakarta.annotation.Nullable')
        ] as Map<String, Object>
    }

    private static String typeName(Argument<?> argument) {
        if (argument == null) {
            return null
        }
        String simple = argument.type.simpleName ?: argument.type.name
        Argument<?>[] parameters = argument.typeParameters
        if (parameters == null || parameters.length == 0) {
            return simple
        }
        String parameterText = parameters.collect {Argument<?> child -> typeName(child)}.join(', ')
        "${simple}<${parameterText}>"
    }

    private static String operationKind(UriRouteInfo<?, ?> route, String operationId, String summary) {
        String method = route.httpMethod.name()
        String text = [
            route.targetMethod.methodName,
            operationId,
            summary
        ].findAll {String value -> value != null}.join(' ').toLowerCase(Locale.ROOT)
        if (method == 'GET') {
            if (text.contains('list') || route.targetMethod.methodName in ['list', 'listAll', 'index']) {
                return 'list'
            }
            return 'get'
        }
        if (method == 'POST' && (text.contains('create') || route.targetMethod.methodName == 'create')) {
            return 'create'
        }
        if (method == 'PUT' && (text.contains('update') || route.targetMethod.methodName == 'update')) {
            return 'update'
        }
        if (method == 'DELETE' && (text.contains('delete') || route.targetMethod.methodName == 'delete')) {
            return 'delete'
        }
        'other'
    }

    private static String inferResourceType(String controllerName, String operationId, String methodName, String path) {
        String base = controllerName
        if (base) {
            return base
        }
        for (String value : [operationId, methodName]) {
            if (value == null) {
                continue
            }
            java.util.regex.Matcher matcher = (value =~ /(?:listAll|list|show|get|create|update|delete)([A-Z][A-Za-z0-9]+)/)
            if (matcher.find()) {
                return matcher.group(1)
            }
        }
        String[] parts = path.split('/')
        for (String part : parts) {
            if (part && !part.startsWith('{') && part != 'api') {
                return singularize(part.capitalize())
            }
        }
        null
    }

    private static String singularize(String text) {
        text?.endsWith('ies') ? text.substring(0, text.length() - 3) + 'y' :
            (text?.endsWith('s') ? text.substring(0, text.length() - 1) : text)
    }

    private static boolean isExcluded(UriRouteInfo<?, ?> route) {
        Class<?> declaringType = route.declaringType
        String className = declaringType.name
        for (String prefix : EXCLUDED_PACKAGE_PREFIXES) {
            if (className.startsWith(prefix)) {
                return true
            }
        }
        String path = route.uriMatchTemplate.toString()
        for (String prefix : EXCLUDED_PATH_PREFIXES) {
            if (path == prefix || path.startsWith(prefix + '/') || path.startsWith(prefix + '{')) {
                return true
            }
        }
        AnnotationMetadata routeMetadata = route.annotationMetadata
        if (routeMetadata.hasAnnotation(McpResourceExcluded)) {
            return true
        }
        route.targetMethod.annotationMetadata.hasAnnotation(McpResourceExcluded)
    }

    private static String resourceName(UriRouteInfo<?, ?> route, String path) {
        String controllerName = normalizedControllerName(route.declaringType)
        "${controllerName}.${route.targetMethod.methodName}"
    }

    private static String resourceDescription(UriRouteInfo<?, ?> route, String path) {
        "HTTP GET ${path} (${normalizedControllerName(route.declaringType)}.${route.targetMethod.methodName})"
    }

    private static String normalizedControllerName(Class<?> declaringType) {
        String simpleName = declaringType.simpleName ?: declaringType.name
        java.util.regex.Matcher matcher = (simpleName =~ /([A-Za-z0-9_]+Controller)/)
        if (matcher.find()) {
            simpleName = matcher.group(1)
        }
        simpleName.replaceAll(/Controller$/, '')
    }

    private static String preferredMimeType(UriRouteInfo<?, ?> route) {
        List<MediaType> produces = route.produces ?: []
        if (produces.any {MediaType mediaType -> mediaType == MediaType.APPLICATION_JSON_TYPE }) {
            return MediaType.APPLICATION_JSON
        }
        produces ? produces.first().toString() : MediaType.APPLICATION_JSON
    }

    private static String pathFromResourceUri(String uri) {
        if (uri == null || !uri.startsWith(URI_PREFIX)) {
            return null
        }
        uri.substring(URI_PREFIX.length())
    }

    private static String pathWithoutQuery(String path) {
        int queryAt = path.indexOf('?')
        queryAt >= 0 ? path.substring(0, queryAt) : path
    }

    private static boolean pathMatchesTemplate(String path, String template) {
        StringBuilder regex = new StringBuilder('^')
        int index = 0
        while (index < template.length()) {
            int open = template.indexOf('{', index)
            if (open < 0) {
                regex.append(java.util.regex.Pattern.quote(template.substring(index)))
                break
            }
            regex.append(java.util.regex.Pattern.quote(template.substring(index, open)))
            int close = template.indexOf('}', open)
            if (close < 0) {
                regex.append(java.util.regex.Pattern.quote(template.substring(open)))
                break
            }
            regex.append('[^/]+')
            index = close + 1
        }
        regex.append('$')
        path ==~ regex.toString()
    }

    static class McpHttpResource {
        String path
        String resourceUri
        String uriTemplate
        String name
        String description
        String mimeType
        String controller
        String method
        boolean template
    }

    static class McpHttpOperation {
        String path
        String operationUri
        String name
        String description
        String mimeType
        String controller
        String method
        boolean template
        String httpMethod
        String operationKind
        String resourceType
        String operationId
        String summary
        List<String> consumes = []
        List<String> produces = []
        List<String> pathParameters = []
        List<Map<String, Object>> parameters = []
        Map<String, Object> body
        String responseType
    }
}

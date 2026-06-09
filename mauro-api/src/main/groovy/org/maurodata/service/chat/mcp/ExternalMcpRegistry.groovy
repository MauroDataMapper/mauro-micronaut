package org.maurodata.service.chat.mcp

import groovy.transform.CompileStatic
import io.micronaut.http.HttpStatus
import io.micronaut.http.exceptions.HttpStatusException
import jakarta.inject.Singleton
import org.maurodata.api.chat.McpServerDto
import org.maurodata.api.chat.UpsertMcpServerRequest

@CompileStatic
@Singleton
class ExternalMcpRegistry {

    private final Map<String, HttpMcpServerConfig> servers = new LinkedHashMap<String, HttpMcpServerConfig>()

    List<McpServerDto> listServers() {
        synchronized (servers) {
            servers.values()
                .findAll {HttpMcpServerConfig config -> config.enabled}
                .collect {HttpMcpServerConfig config -> toDto(config)}
        }
    }

    boolean canHandle(String toolName) {
        toolName != null && toolName.contains('.')
    }

    Map<String, Object> invoke(String toolName, Map<String, Object> arguments) {
        throw new HttpStatusException(HttpStatus.BAD_REQUEST, "External MCP tool invocation is not implemented yet: ${toolName}")
    }

    McpServerDto addServer(UpsertMcpServerRequest request) {
        HttpMcpServerConfig config = configFromRequest(request, request?.id ?: UUID.randomUUID().toString())
        synchronized (servers) {
            if (servers.containsKey(config.id)) {
                throw new HttpStatusException(HttpStatus.CONFLICT, "MCP server already exists: ${config.id}")
            }
            servers.put(config.id, config)
        }
        toDto(config)
    }

    McpServerDto updateServer(String serverId, UpsertMcpServerRequest request) {
        String resolvedId = requiredId(serverId)
        HttpMcpServerConfig config = configFromRequest(request, resolvedId)
        synchronized (servers) {
            if (!servers.containsKey(resolvedId)) {
                throw new HttpStatusException(HttpStatus.NOT_FOUND, "MCP server not found: ${resolvedId}")
            }
            servers.put(resolvedId, config)
        }
        toDto(config)
    }

    void removeServer(String serverId) {
        String resolvedId = requiredId(serverId)
        synchronized (servers) {
            if (servers.remove(resolvedId) == null) {
                throw new HttpStatusException(HttpStatus.NOT_FOUND, "MCP server not found: ${resolvedId}")
            }
        }
    }

    private static HttpMcpServerConfig configFromRequest(UpsertMcpServerRequest request, String id) {
        if (request == null) {
            throw new HttpStatusException(HttpStatus.BAD_REQUEST, 'MCP server request is required')
        }
        String resolvedId = requiredId(id)
        String name = requiredText(request.name, 'name')
        URI uri = parseHttpUri(request.url)
        new HttpMcpServerConfig(
            id: resolvedId,
            name: name,
            url: uri.toString(),
            enabled: request.enabled != Boolean.FALSE,
            metadata: request.metadata ?: [:]
        )
    }

    private static McpServerDto toDto(HttpMcpServerConfig config) {
        new McpServerDto(
            id: config.id,
            name: config.name,
            transport: 'HTTP',
            url: config.url,
            level: 'GLOBAL',
            status: config.enabled ? 'DISCONNECTED' : 'DISABLED',
            tools: []
        )
    }

    private static URI parseHttpUri(String url) {
        String value = requiredText(url, 'url')
        try {
            URI uri = new URI(value)
            if (!['http', 'https'].contains(uri.scheme?.toLowerCase(Locale.ROOT))) {
                throw new HttpStatusException(HttpStatus.BAD_REQUEST, 'Only HTTP MCP server URLs are supported')
            }
            if (uri.host == null || uri.host.trim().isEmpty()) {
                throw new HttpStatusException(HttpStatus.BAD_REQUEST, 'MCP server URL must include a host')
            }
            uri
        } catch (HttpStatusException e) {
            throw e
        } catch (URISyntaxException ignored) {
            throw new HttpStatusException(HttpStatus.BAD_REQUEST, 'Invalid MCP server URL')
        }
    }

    private static String requiredId(String id) {
        requiredText(id, 'serverId')
    }

    private static String requiredText(String value, String field) {
        if (value == null || value.trim().isEmpty()) {
            throw new HttpStatusException(HttpStatus.BAD_REQUEST, "MCP server ${field} is required")
        }
        value.trim()
    }

    private static class HttpMcpServerConfig {
        String id
        String name
        String url
        boolean enabled
        Map<String, Object> metadata = [:]
    }
}

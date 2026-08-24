package org.maurodata.service.chat.llm.langchain4j

import dev.langchain4j.agent.tool.ToolExecutionRequest
import dev.langchain4j.agent.tool.ToolSpecification
import dev.langchain4j.data.message.AiMessage
import dev.langchain4j.data.message.ChatMessage
import dev.langchain4j.data.message.SystemMessage
import dev.langchain4j.data.message.ToolExecutionResultMessage
import dev.langchain4j.data.message.UserMessage
import dev.langchain4j.model.chat.request.ChatRequest
import dev.langchain4j.model.chat.request.json.JsonArraySchema
import dev.langchain4j.model.chat.request.json.JsonBooleanSchema
import dev.langchain4j.model.chat.request.json.JsonEnumSchema
import dev.langchain4j.model.chat.request.json.JsonIntegerSchema
import dev.langchain4j.model.chat.request.json.JsonNumberSchema
import dev.langchain4j.model.chat.request.json.JsonObjectSchema
import dev.langchain4j.model.chat.request.json.JsonSchemaElement
import dev.langchain4j.model.chat.request.json.JsonStringSchema
import groovy.transform.CompileStatic
import org.maurodata.service.chat.llm.ProviderMessage
import org.maurodata.service.chat.llm.ProviderRequest

@CompileStatic
class LangChain4jRequestMapper {

    ChatRequest toChatRequest(ProviderRequest request, List<ChatMessage> messages) {
        ChatRequest.Builder builder = ChatRequest.builder()
            .modelName(request.model)
            .messages(messages)
        List<ToolSpecification> toolSpecifications = toToolSpecifications(request.tools)
        if (!toolSpecifications.isEmpty()) {
            builder.toolSpecifications(toolSpecifications)
        }
        applyRequestOptions(builder, request.options ?: Collections.<String, Object>emptyMap())
        builder.build()
    }

    List<ChatMessage> toChatMessages(List<ProviderMessage> providerMessages) {
        List<ChatMessage> messages = new ArrayList<ChatMessage>()
        for (ProviderMessage message : providerMessages ?: Collections.<ProviderMessage>emptyList()) {
            ChatMessage mapped = toChatMessage(message)
            if (mapped != null) {
                messages.add(mapped)
            }
        }
        messages
    }

    AiMessage toAiMessage(List<ToolExecutionRequest> requests, String visibleText) {
        AiMessage.builder().text(visibleText ?: '').toolExecutionRequests(requests).build()
    }

    ToolExecutionResultMessage toToolResultMessage(String id, String toolName, String text, boolean error) {
        ToolExecutionResultMessage.builder()
            .id(id)
            .toolName(toolName)
            .text(text ?: '')
            .isError(Boolean.valueOf(error))
            .build()
    }

    Map<String, Object> toProviderMessageMap(ChatMessage message) {
        Map<String, Object> out = new LinkedHashMap<String, Object>()
        if (message instanceof SystemMessage) {
            out.put('role', 'system')
            out.put('content', ((SystemMessage) message).text() ?: '')
        } else if (message instanceof UserMessage) {
            out.put('role', 'user')
            out.put('content', ((UserMessage) message).hasSingleText() ? ((UserMessage) message).singleText() : '')
        } else if (message instanceof AiMessage) {
            AiMessage ai = (AiMessage) message
            out.put('role', 'assistant')
            out.put('content', ai.text() ?: '')
            if (ai.hasToolExecutionRequests()) {
                out.put('toolCalls', ai.toolExecutionRequests().collect {ToolExecutionRequest request ->
                    [
                        id      : request.id(),
                        type    : 'function',
                        function: [
                            name     : request.name(),
                            arguments: request.arguments()
                        ]
                    ] as Map<String, Object>
                })
            }
        } else if (message instanceof ToolExecutionResultMessage) {
            ToolExecutionResultMessage tool = (ToolExecutionResultMessage) message
            out.put('role', 'tool')
            out.put('content', tool.text() ?: '')
            out.put('toolCallId', tool.id())
            out.put('name', tool.toolName())
        }
        out
    }

    private ChatMessage toChatMessage(ProviderMessage message) {
        final String role = message.role
        if ('system'.equals(role)) {
            return SystemMessage.from(message.content ?: '')
        }
        if ('user'.equals(role)) {
            return UserMessage.from(message.content ?: '')
        }
        if ('assistant'.equals(role)) {
            List<ToolExecutionRequest> requests = toToolExecutionRequests(message.toolCalls)
            return requests.isEmpty() ? AiMessage.from(message.content ?: '') : toAiMessage(requests, message.content)
        }
        if ('tool'.equals(role)) {
            return toToolResultMessage(message.toolCallId, message.name, message.content, false)
        }
        null
    }

    private static void applyRequestOptions(ChatRequest.Builder builder, Map<String, Object> options) {
        Double temperature = doubleValue(options.get('temperature'))
        if (temperature != null) {
            builder.temperature(temperature)
        }
        Double topP = doubleValue(options.get('topP') ?: options.get('top_p'))
        if (topP != null) {
            builder.topP(topP)
        }
        Integer maxOutputTokens = integerValue(options.get('maxOutputTokens') ?: options.get('max_tokens') ?: options.get('maxCompletionTokens'))
        if (maxOutputTokens != null) {
            builder.maxOutputTokens(maxOutputTokens)
        }
    }

    private List<ToolSpecification> toToolSpecifications(List<Map<String, Object>> tools) {
        List<ToolSpecification> out = new ArrayList<ToolSpecification>()
        for (Map<String, Object> tool : tools ?: Collections.<Map<String, Object>>emptyList()) {
            Map<String, Object> function = asMap(tool.get('function'))
            String name = stringValue(function.get('name'))
            if (name == null || name.trim().isEmpty()) {
                continue
            }
            out.add(ToolSpecification.builder()
                .name(name)
                .description(stringValue(function.get('description')) ?: '')
                .parameters(toObjectSchema(asMap(function.get('parameters'))))
                .build())
        }
        out
    }

    private List<ToolExecutionRequest> toToolExecutionRequests(List<Map<String, Object>> toolCalls) {
        List<ToolExecutionRequest> out = new ArrayList<ToolExecutionRequest>()
        for (Map<String, Object> call : toolCalls ?: Collections.<Map<String, Object>>emptyList()) {
            Map<String, Object> function = asMap(call.get('function'))
            String name = stringValue(function.get('name'))
            if (name == null || name.trim().isEmpty()) {
                continue
            }
            out.add(ToolExecutionRequest.builder()
                .id(stringValue(call.get('id')) ?: UUID.randomUUID().toString())
                .name(name)
                .arguments(stringValue(function.get('arguments')) ?: '{}')
                .build())
        }
        out
    }

    private JsonObjectSchema toObjectSchema(Map<String, Object> schema) {
        JsonObjectSchema.Builder builder = JsonObjectSchema.builder()
        String description = stringValue(schema.get('description'))
        if (description != null) {
            builder.description(description)
        }
        Map<String, Object> properties = asMap(schema.get('properties'))
        for (Map.Entry<String, Object> entry : properties.entrySet()) {
            builder.addProperty(entry.key, toSchemaElement(entry.value))
        }
        List<String> required = stringList(schema.get('required'))
        if (!required.isEmpty()) {
            builder.required(required)
        }
        Object additionalProperties = schema.get('additionalProperties')
        if (additionalProperties instanceof Boolean) {
            builder.additionalProperties((Boolean) additionalProperties)
        }
        builder.build()
    }

    private JsonSchemaElement toSchemaElement(Object raw) {
        Map<String, Object> schema = asMap(raw)
        String type = stringValue(schema.get('type')) ?: 'string'
        String description = stringValue(schema.get('description'))
        List<String> enumValues = stringList(schema.get('enum'))
        if (!enumValues.isEmpty()) {
            JsonEnumSchema.Builder builder = JsonEnumSchema.builder().enumValues(enumValues)
            if (description != null) {
                builder.description(description)
            }
            return builder.build()
        }
        if ('object'.equals(type)) {
            return toObjectSchema(schema)
        }
        if ('array'.equals(type)) {
            JsonArraySchema.Builder builder = JsonArraySchema.builder()
            if (description != null) {
                builder.description(description)
            }
            builder.items(toSchemaElement(schema.get('items') ?: [type: 'string']))
            return builder.build()
        }
        if ('integer'.equals(type)) {
            JsonIntegerSchema.Builder builder = JsonIntegerSchema.builder()
            if (description != null) {
                builder.description(description)
            }
            return builder.build()
        }
        if ('number'.equals(type)) {
            JsonNumberSchema.Builder builder = JsonNumberSchema.builder()
            if (description != null) {
                builder.description(description)
            }
            return builder.build()
        }
        if ('boolean'.equals(type)) {
            JsonBooleanSchema.Builder builder = JsonBooleanSchema.builder()
            if (description != null) {
                builder.description(description)
            }
            return builder.build()
        }
        JsonStringSchema.Builder builder = JsonStringSchema.builder()
        if (description != null) {
            builder.description(description)
        }
        builder.build()
    }

    private static Map<String, Object> asMap(Object value) {
        value instanceof Map ? new LinkedHashMap<String, Object>((Map<String, Object>) value) : Collections.<String, Object>emptyMap()
    }

    private static String stringValue(Object value) {
        value == null ? null : String.valueOf(value)
    }

    private static List<String> stringList(Object value) {
        if (!(value instanceof Collection)) {
            return Collections.<String>emptyList()
        }
        ((Collection<?>) value).collect {Object item -> stringValue(item)}.findAll {String item -> item != null && !item.trim().isEmpty()} as List<String>
    }

    private static Double doubleValue(Object value) {
        if (value == null) {
            return null
        }
        value instanceof Number ? Double.valueOf(((Number) value).doubleValue()) : Double.valueOf(String.valueOf(value))
    }

    private static Integer integerValue(Object value) {
        if (value == null) {
            return null
        }
        value instanceof Number ? Integer.valueOf(((Number) value).intValue()) : Integer.valueOf(String.valueOf(value))
    }
}

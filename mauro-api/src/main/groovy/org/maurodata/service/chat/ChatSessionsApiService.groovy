package org.maurodata.service.chat

import groovy.json.JsonOutput
import groovy.util.logging.Slf4j
import io.micronaut.context.annotation.Value
import io.micronaut.http.HttpStatus
import io.micronaut.http.exceptions.HttpStatusException
import jakarta.inject.Singleton
import org.maurodata.api.chat.ChatEventDto
import org.maurodata.api.chat.CreateSessionRequest
import org.maurodata.api.chat.ListSessionMessagesResponseDto
import org.maurodata.api.chat.MessageDto
import org.maurodata.api.chat.SendMessageRequest
import org.maurodata.api.chat.SessionDto
import org.maurodata.api.chat.UpdateSessionSkillsRequest
import org.maurodata.service.chat.llm.ProviderMessage
import org.maurodata.service.chat.llm.ProviderRegistry
import org.maurodata.service.chat.llm.ProviderRequest
import org.reactivestreams.Publisher
import reactor.core.publisher.Flux

import java.time.Instant

@Slf4j
@Singleton
class ChatSessionsApiService implements ChatSessionService {

    private final ChatInMemoryStore store
    private final ProviderRegistry providerRegistry
    private final ChatMcpService chatMcpService
    private final ChatSkillService chatSkillService
    private final ChatPromptResourceService promptResourceService
    private final String defaultModel

    ChatSessionsApiService(
        ChatInMemoryStore store,
        ProviderRegistry providerRegistry,
        ChatMcpService chatMcpService,
        ChatSkillService chatSkillService,
        ChatPromptResourceService promptResourceService,
        @Value('${chat.providers.default-model:llama3.1}') String defaultModel
    ) {
        this.store = store
        this.providerRegistry = providerRegistry
        this.chatMcpService = chatMcpService
        this.chatSkillService = chatSkillService
        this.promptResourceService = promptResourceService
        this.defaultModel = defaultModel
    }

    @Override
    SessionDto createSession(CreateSessionRequest request) {
        Instant now = ChatInMemoryStore.now()
        String id = UUID.randomUUID().toString()
        SessionDto session = new SessionDto(
            id: id,
            workspaceId: request.workspaceId,
            title: request.title,
            status: 'ACTIVE',
            model: request.model ?: defaultModel,
            skillIds: request.skillIds ?: [],
            createdAt: now,
            updatedAt: now
        )
        store.sessions[id] = session
        log.info('createSession sessionId={} workspaceId={}', session.id, session.workspaceId)
        session
    }

    @Override
    SessionDto getSession(String sessionId) {
        SessionDto session = store.sessions[sessionId]
        if (!session) {
            throw new HttpStatusException(HttpStatus.NOT_FOUND, "Session not found: ${sessionId}")
        }
        session
    }

    @Override
    Publisher<ChatEventDto> sendMessage(String sessionId, SendMessageRequest request) {
        long start = System.currentTimeMillis()
        SessionDto session = getSession(sessionId)
        String assistantMessageId = UUID.randomUUID().toString()
        Instant now = ChatInMemoryStore.now()
        List<MessageDto> timeline = store.messagesForSession(sessionId)
        MessageDto userMessage = new MessageDto(
            id: request.messageId ?: UUID.randomUUID().toString(),
            sessionId: sessionId,
            role: 'user',
            content: request.content ?: '',
            status: 'complete',
            thinkingContent: '',
            createdAt: now.toString(),
            updatedAt: now.toString(),
            metadata: [attachments: request.attachments ?: [], contextRefs: request.contextRefs ?: []]
        )
        timeline.add(userMessage)
        MessageDto assistantMessage = new MessageDto(
            id: assistantMessageId,
            sessionId: sessionId,
            role: 'assistant',
            content: '',
            status: 'streaming',
            thinkingContent: '',
            createdAt: now.toString(),
            updatedAt: now.toString(),
            metadata: [provider: '', model: session.model ?: '']
        )
        timeline.add(assistantMessage)
        MessageDto messageStartEvent = new MessageDto(
            id: UUID.randomUUID().toString(),
            sessionId: sessionId,
            role: 'assistant',
            content: '',
            status: 'event',
            thinkingContent: '',
            createdAt: now.toString(),
            updatedAt: now.toString(),
            metadata: [eventType: 'message_start', messageId: assistantMessageId]
        )
        timeline.add(messageStartEvent)
        log.info('sendMessage sessionId={} requestMessageId={}', sessionId, request.messageId)
        try {
            def provider = providerRegistry.byModel(session.model)
            assistantMessage.metadata.put('provider', provider.id())
            List<Map<String, Object>> tools = chatMcpService.listServers()
                .collectMany {it.tools}
	                .collect {[
	                    type    : 'function',
	                    function: [
	                        name       : it.name,
	                        description: it.description ?: '',
	                        parameters : it.inputSchema ?: [type: 'object']
	                    ],
	                    routing : it.routing ?: [:]
	                ]}
            String skillInstruction = chatSkillService.listSkills()
                .findAll {session.skillIds.contains(it.id)}
                .collect {"${it.name}: ${it.description}"}
                .join('\n')
            List<ChatSkillDefinition> skillDefinitions = chatSkillService.listSkillDefinitions()
            String personaInstruction = buildPersonaInstruction(chatSkillService.listPersonaDefinitions())
            String toolInstruction = buildToolInstruction(tools, promptResourceService)
            String routingInstruction = buildRoutingInstruction(skillDefinitions, tools)

            List<ProviderMessage> historyMessages = buildProviderHistory(timeline)

            ProviderRequest providerRequest = new ProviderRequest(
                sessionId: session.id,
                messageId: assistantMessageId,
                model: session.model,
                tools: tools,
                options: request.options ?: [:],
                messages: [
                    personaInstruction ? new ProviderMessage(role: 'system', content: personaInstruction) : null,
                    toolInstruction ? new ProviderMessage(role: 'system', content: toolInstruction) : null,
                    routingInstruction ? new ProviderMessage(role: 'system', content: routingInstruction) : null,
                    skillInstruction ? new ProviderMessage(role: 'system', content: skillInstruction) : null
                ].findAll {it != null}
            )
            providerRequest.messages.addAll(historyMessages)

            Flux<ChatEventDto> stream = Flux.from(provider.streamChat(providerRequest))
                .filter {chunk -> !['done', 'message_complete'].contains(chunk.type)}
                .map {chunk ->
                    String eventType = chunk.type
                    synchronized (assistantMessage) {
                        if (eventType == 'token') {
                            assistantMessage.content = (assistantMessage.content ?: '') + (chunk.content ?: '')
                        } else if (eventType == 'thinking_token') {
                            assistantMessage.thinkingContent = (assistantMessage.thinkingContent ?: '') + (chunk.content ?: '')
                        } else if (eventType == 'error') {
                            assistantMessage.status = 'error'
                            assistantMessage.metadata.put('error', chunk.content ?: 'Unknown error')
                        } else if (eventType == 'tool_call' || eventType == 'tool_result') {
                            List<Map<String, Object>> toolEvents = (List<Map<String, Object>>) assistantMessage.metadata.get('toolEvents')
                            if (toolEvents == null) {
                                toolEvents = new ArrayList<Map<String, Object>>()
                                assistantMessage.metadata.put('toolEvents', toolEvents)
                            }
                            toolEvents.add([
                                type      : eventType,
                                at        : ChatInMemoryStore.now().toString(),
                                attributes: chunk.metadata ?: [:]
                            ])
                        }
                        assistantMessage.updatedAt = ChatInMemoryStore.now().toString()
                    }
                    new ChatEventDto(
                        type: eventType,
                        messageId: chunk.messageId ?: assistantMessageId,
                        role: 'assistant',
                        content: chunk.type == 'error' ? "Provider error: ${chunk.content ?: 'Unknown error'}" : (chunk.content ?: ''),
                        done: false,
                        metadata: chunk.metadata ?: [:]
                    )
                }

            session.updatedAt = ChatInMemoryStore.now()
            Flux.concat(
                Flux.just(new ChatEventDto(
                    type: 'message_start',
                    messageId: assistantMessageId,
                    role: 'assistant',
                    content: '',
                    done: false,
                    metadata: [sessionId: session.id, provider: provider.id()]
                )),
                stream,
                Flux.just(new ChatEventDto(
                    type: 'message_complete',
                    messageId: assistantMessageId,
                    role: 'assistant',
                    content: '',
                    done: false,
                    metadata: [timestamp: ChatInMemoryStore.now().toString()]
                )),
                Flux.just(new ChatEventDto(
                    type: 'done',
                    messageId: assistantMessageId,
                    role: 'assistant',
                    content: '',
                    done: true,
                    metadata: [:]
                ))
            ).doFinally {
                synchronized (assistantMessage) {
                    if (assistantMessage.status != 'error') {
                        assistantMessage.status = 'complete'
                    }
                    assistantMessage.updatedAt = ChatInMemoryStore.now().toString()
                }
                Instant finishedAt = ChatInMemoryStore.now()
                timeline.add(new MessageDto(
                    id: UUID.randomUUID().toString(),
                    sessionId: sessionId,
                    role: 'assistant',
                    content: '',
                    status: 'event',
                    thinkingContent: '',
                    createdAt: finishedAt.toString(),
                    updatedAt: finishedAt.toString(),
                    metadata: [eventType: 'message_complete', messageId: assistantMessageId]
                ))
                timeline.add(new MessageDto(
                    id: UUID.randomUUID().toString(),
                    sessionId: sessionId,
                    role: 'assistant',
                    content: '',
                    status: 'event',
                    thinkingContent: '',
                    createdAt: finishedAt.toString(),
                    updatedAt: finishedAt.toString(),
                    metadata: [eventType: 'done', messageId: assistantMessageId]
                ))
            }
        } finally {
            log.info('sendMessage completed sessionId={} durationMs={}', sessionId, System.currentTimeMillis() - start)
        }
    }

    @Override
    ListSessionMessagesResponseDto listSessionMessages(String sessionId, Integer limit, String beforeMessageId) {
        getSession(sessionId)
        int resolvedLimit = (limit == null || limit < 1) ? 200 : Math.min(limit, 500)
        List<MessageDto> timeline = store.messagesForSession(sessionId)
        List<MessageDto> snapshot = new ArrayList<MessageDto>(timeline)

        if (beforeMessageId) {
            int beforeIndex = snapshot.findIndexOf {it.id == beforeMessageId}
            if (beforeIndex >= 0) {
                snapshot = snapshot.subList(0, beforeIndex)
            }
        }

        boolean truncated = snapshot.size() > resolvedLimit
        List<MessageDto> page = truncated
            ? new ArrayList<MessageDto>(snapshot.subList(snapshot.size() - resolvedLimit, snapshot.size()))
            : snapshot

        ListSessionMessagesResponseDto response = new ListSessionMessagesResponseDto()
        response.items = page.collect {MessageDto message ->
            new MessageDto(
                id: message.id,
                sessionId: message.sessionId,
                role: message.role,
                content: message.content,
                status: message.status,
                thinkingContent: message.thinkingContent,
                createdAt: message.createdAt,
                updatedAt: message.updatedAt,
                metadata: message.metadata ? new LinkedHashMap<String, Object>(message.metadata) : [:]
            )
        }
        response.nextPageToken = truncated && !page.isEmpty() ? page.first().id : null
        response
    }

    @Override
    void updateSessionSkills(String sessionId, UpdateSessionSkillsRequest request) {
        SessionDto session = getSession(sessionId)
        session.skillIds = request.skillIds ?: []
        session.updatedAt = ChatInMemoryStore.now()
        log.info('updateSessionSkills sessionId={} skillCount={}', sessionId, session.skillIds.size())
    }

    private static String buildToolInstruction(
        List<Map<String, Object>> tools,
        ChatPromptResourceService promptResourceService
    ) {
        if (!tools) {
            return ''
        }
        promptResourceService.getPrompt(ChatPromptResourceService.TOOL_POLICY)
    }

    private static String buildPersonaInstruction(List<ChatSkillDefinition> personas) {
        if (!personas) {
            return ''
        }
        StringBuilder builder = new StringBuilder(1024)
        for (int i = 0; i < personas.size(); i++) {
            ChatSkillDefinition persona = personas.get(i)
            if (persona.instruction != null && !persona.instruction.trim().isEmpty()) {
                if (builder.length() > 0) {
                    builder.append('\n\n')
                }
                builder.append(persona.instruction.trim())
            }
        }
        builder.toString()
    }

    private static String buildRoutingInstruction(List<ChatSkillDefinition> skills, List<Map<String, Object>> tools) {
        StringBuilder builder = new StringBuilder(2048)
        builder.append('Routing index for available Mauro assistance. Use this index to choose whether to call a tool or retrieve a skill. Individual skills own their own routing; do not rely on the persona to list every skill. Prefer the most specific matching skill route. Use GENERAL or FALLBACK routes only when no SPECIFIC route matches well.')

        List<String> skillRoutes = buildSkillRoutes(skills)
        if (!skillRoutes.isEmpty()) {
            builder.append('\n\nSkill routes:')
            for (String route : skillRoutes) {
                builder.append('\n- ')
                    .append(route)
            }
        }

        List<String> toolRoutes = buildToolRoutes(tools)
        if (!toolRoutes.isEmpty()) {
            builder.append('\n\nTool routes:')
            for (String route : toolRoutes) {
                builder.append('\n- ')
                    .append(route)
            }
        }
        builder.toString()
    }

    private static List<String> buildSkillRoutes(List<ChatSkillDefinition> skills) {
        if (!skills) {
            return []
        }
        List<ChatSkillDefinition> routedSkills = skills
            .findAll {ChatSkillDefinition skill ->
                !'PERSONA'.equalsIgnoreCase(skill.type) &&
                    skill.routing != null &&
                    (!(skill.routing.useWhen ?: []).isEmpty() ||
                        !(skill.routing.avoidWhen ?: []).isEmpty() ||
                        !(skill.routing.examples ?: []).isEmpty())
            }
            .sort {ChatSkillDefinition left, ChatSkillDefinition right ->
                Integer leftPriority = left.priority ?: Integer.valueOf(1000)
                Integer rightPriority = right.priority ?: Integer.valueOf(1000)
                int priorityCompare = leftPriority <=> rightPriority
                priorityCompare != 0 ? priorityCompare : left.id <=> right.id
            }

        List<String> routes = new ArrayList<String>()
        for (ChatSkillDefinition skill : routedSkills) {
            SkillRouting routing = skill.routing
            StringBuilder route = new StringBuilder(512)
            route.append(skill.name)
                .append(' (')
                .append(skill.id)
                .append('): ')
                .append(skill.description)
                .append(' Specificity: ')
                .append(routing.specificity ?: 'NORMAL')
                .append('.')
            if (!(routing.useWhen ?: []).isEmpty()) {
                route.append(' Use when: ')
                    .append(routing.useWhen.join('; '))
                    .append('.')
            }
            if (!(routing.avoidWhen ?: []).isEmpty()) {
                route.append(' Avoid when: ')
                    .append(routing.avoidWhen.join('; '))
                    .append('.')
            }

            String toolName = routing.toolName ?: 'skill_lookup'
            Map<String, Object> toolArguments = routing.toolArguments && !routing.toolArguments.isEmpty()
                ? routing.toolArguments
                : [id: skill.id, includeInstruction: true] as Map<String, Object>
            route.append(' Retrieve with ')
                .append(toolName)
                .append(' using arguments ')
                .append(JsonOutput.toJson(toolArguments))
                .append('.')

            if (!(routing.examples ?: []).isEmpty()) {
                route.append(' Examples: ')
                    .append(routing.examples.take(3).join(' | '))
                    .append('.')
            }
            routes.add(route.toString())
        }
        routes
    }

    private static List<String> buildToolRoutes(List<Map<String, Object>> tools) {
        if (!tools) {
            return []
        }
        List<String> routes = new ArrayList<String>()
        for (Map<String, Object> tool : tools) {
            Object functionObj = tool.get('function')
            if (!(functionObj instanceof Map)) {
                continue
            }
            @SuppressWarnings('unchecked')
            Map<String, Object> function = (Map<String, Object>) functionObj
            String name = asString(function.get('name'))
            String description = asString(function.get('description'))
            if (name == null || name.trim().isEmpty() || description == null || description.trim().isEmpty()) {
                continue
            }
            Map<String, Object> routing = getMap(tool.get('routing'))
            if (routing.isEmpty()) {
                routes.add("${name}: ${description}")
            } else {
                routes.add(buildToolRoute(name, description, routing))
            }
        }
        routes
    }

    private static String buildToolRoute(String name, String description, Map<String, Object> routing) {
        StringBuilder route = new StringBuilder(768)
        route.append(name)
            .append(': ')
            .append(description)
        appendStringSection(route, ' Purpose: ', routing.get('purpose'))
        appendListSection(route, ' Use when: ', routing.get('useWhen'))
        appendListSection(route, ' Avoid when: ', routing.get('avoidWhen'))
        appendListSection(route, ' Search syntax: ', routing.get('syntax'))
        appendListSection(route, ' Filtering: ', routing.get('filtering'))
        appendListSection(route, ' Paging: ', routing.get('paging'))
        appendListSection(route, ' Limitations: ', routing.get('limitations'))
        appendListSection(route, ' Examples: ', routing.get('examples'))
        route.toString()
    }

    private static void appendStringSection(StringBuilder builder, String label, Object value) {
        String text = asString(value)
        if (text != null && !text.trim().isEmpty()) {
            builder.append(label)
                .append(text)
                .append('.')
        }
    }

    private static void appendListSection(StringBuilder builder, String label, Object value) {
        List<String> values = asStringList(value)
        if (!values.isEmpty()) {
            builder.append(label)
                .append(values.join('; '))
                .append('.')
        }
    }

    private static Map<String, Object> getMap(Object value) {
        if (!(value instanceof Map)) {
            return [:] as Map<String, Object>
        }
        @SuppressWarnings('unchecked')
        Map<String, Object> typed = (Map<String, Object>) value
        typed
    }

    private static List<ProviderMessage> buildProviderHistory(List<MessageDto> timeline) {
        List<ProviderMessage> history = new ArrayList<ProviderMessage>()
        for (MessageDto message : timeline) {
            if (message == null || message.status == 'event') {
                continue
            }
            if (message.role == 'user' || message.role == 'assistant' || message.role == 'tool') {
                if (message.content != null && !message.content.trim().isEmpty()) {
                    history.add(new ProviderMessage(role: message.role, content: message.content))
                }
            }
            String toolMemory = buildToolHistoryMemory(message)
            if (toolMemory != null && !toolMemory.trim().isEmpty()) {
                history.add(new ProviderMessage(role: 'system', content: toolMemory))
            }
        }
        history
    }

    private static String buildToolHistoryMemory(MessageDto message) {
        if (message == null || message.role != 'assistant' || !message.metadata) {
            return ''
        }
        Object eventsObj = message.metadata.get('toolEvents')
        if (!(eventsObj instanceof List)) {
            return ''
        }

        String latestCatalogueSearchMemory = ''
        for (Object eventObj : (List<?>) eventsObj) {
            if (!(eventObj instanceof Map)) {
                continue
            }
            @SuppressWarnings('unchecked')
            Map<String, Object> event = (Map<String, Object>) eventObj
            if (event.get('type') != 'tool_result') {
                continue
            }
            Object attributesObj = event.get('attributes')
            if (!(attributesObj instanceof Map)) {
                continue
            }
            @SuppressWarnings('unchecked')
            Map<String, Object> attributes = (Map<String, Object>) attributesObj
            String memory = buildCatalogueSearchToolMemory(attributes)
            if (memory != null && !memory.trim().isEmpty()) {
                latestCatalogueSearchMemory = memory
            }
        }
        latestCatalogueSearchMemory
    }

    private static String buildCatalogueSearchToolMemory(Map<String, Object> toolResultAttributes) {
        Object wrappedOutputObj = toolResultAttributes.get('output')
        if (!(wrappedOutputObj instanceof Map)) {
            return ''
        }
        @SuppressWarnings('unchecked')
        Map<String, Object> wrappedOutput = (Map<String, Object>) wrappedOutputObj
        if (asString(wrappedOutput.get('tool')) != 'catalogue_search') {
            return ''
        }

        Object outputObj = wrappedOutput.get('output')
        if (!(outputObj instanceof Map)) {
            return ''
        }
        @SuppressWarnings('unchecked')
        Map<String, Object> output = (Map<String, Object>) outputObj

        String searchTerm = asString(output.get('searchTerm'))
        List<String> domainTypes = asStringList(output.get('domainTypes'))
        Integer count = asInteger(output.get('count'))
        Integer max = asInteger(output.get('max'))
        Integer offset = asInteger(output.get('offset'))
        Integer nextOffset = asInteger(output.get('nextOffset'))
        boolean hasMore = Boolean.TRUE.equals(output.get('hasMore'))
        if (searchTerm == null || searchTerm.trim().isEmpty() || max == null || offset == null || nextOffset == null) {
            return ''
        }

        StringBuilder builder = new StringBuilder(512)
        builder.append('Previous catalogue_search result memory: searchTerm "')
            .append(searchTerm)
            .append('"')
        if (count != null) {
            builder.append(', total matching catalogue items ')
                .append(count)
        }
        if (!domainTypes.isEmpty()) {
            builder.append(', domainTypes ')
                .append(domainTypes.join(', '))
        }
        builder.append(', returned page offset ')
            .append(offset)
            .append(', max ')
            .append(max)
            .append(', hasMore ')
            .append(hasMore)
            .append('.')
        builder.append(' If the user asks to count, filter, narrow, or ask how many of all previous results match an additional condition, do not count only the visible page. Call catalogue_search again with the same domainTypes and a refined searchTerm that combines the prior search with the new condition, then answer from the returned total count. If the prior searchTerm uses OR, distribute the new condition across the alternatives, for example age education OR weight education.')

        if (hasMore) {
            Map<String, Object> nextPageToolCall = [
                name     : 'catalogue_search',
                arguments: [
                    searchTerm: searchTerm,
                    domainTypes: domainTypes,
                    max       : max,
                    offset    : nextOffset
                ] as Map<String, Object>
            ] as Map<String, Object>
            builder.append(' If the user asks for more results or the next page of this search, use this exact tool call: ')
                .append(JsonOutput.toJson(nextPageToolCall))
                .append('.')
        }
        builder.toString()
    }

    private static String asString(Object value) {
        value == null ? null : value.toString()
    }

    private static Integer asInteger(Object value) {
        if (value instanceof Number) {
            return ((Number) value).intValue()
        }
        if (value == null) {
            return null
        }
        try {
            return Integer.valueOf(value.toString())
        } catch (NumberFormatException ignored) {
            return null
        }
    }

    private static List<String> asStringList(Object value) {
        if (value == null) {
            return Collections.emptyList()
        }
        List<String> values = new ArrayList<String>()
        if (value instanceof Collection) {
            for (Object item : (Collection<?>) value) {
                String text = asString(item)
                if (text != null && !text.trim().isEmpty()) {
                    values.add(text)
                }
            }
        } else {
            String text = asString(value)
            if (text != null && !text.trim().isEmpty()) {
                values.add(text)
            }
        }
        values
    }
}

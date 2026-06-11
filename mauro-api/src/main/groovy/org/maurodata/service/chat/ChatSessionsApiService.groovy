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
        String userMessageId = request.messageId ?: UUID.randomUUID().toString()
        appendChatEvent(timeline, sessionId, new ChatEventDto(
            type: 'ui_user_message',
            messageId: userMessageId,
            role: 'user',
            content: request.content ?: '',
            done: true,
            metadata: [attachments: request.attachments ?: [], contextRefs: request.contextRefs ?: []]
        ))
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
        appendChatEvent(timeline, sessionId, new ChatEventDto(
            type: 'message_start',
            messageId: assistantMessageId,
            role: 'assistant',
            content: '',
            done: false,
            metadata: [sessionId: sessionId]
        ))
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
            List<ChatSkillDefinition> skillDefinitions = chatSkillService.listSkillDefinitions()
            String personaInstruction = buildPersonaInstruction(chatSkillService.listPersonaDefinitions())
            String toolInstruction = buildToolInstruction(tools, promptResourceService)
            String routingInstruction = buildRoutingInstruction(skillDefinitions, tools)
            String prerequisiteInstruction = buildPrerequisiteSkillInstruction(skillDefinitions, request.content ?: '')

            List<ProviderMessage> historyMessages = buildProviderHistory(timeline)
            List<ProviderMessage> currentContextMessages = new ArrayList<ProviderMessage>()
            List<Map<String, Object>> currentContextReplayMetadata = new ArrayList<Map<String, Object>>()
            addProviderContextMessage(currentContextMessages, currentContextReplayMetadata, personaInstruction, 'persona', 'substitute', 'persona:active')
            addProviderContextMessage(currentContextMessages, currentContextReplayMetadata, prerequisiteInstruction, 'skill_prerequisite', 'replay', null)
            addProviderContextMessage(currentContextMessages, currentContextReplayMetadata, toolInstruction, 'tool_policy', 'substitute', 'tool_policy:active')
            addProviderContextMessage(currentContextMessages, currentContextReplayMetadata, routingInstruction, 'routing', 'substitute', 'routing:index')
            ProviderRequest providerRequest = new ProviderRequest(
                sessionId: session.id,
                messageId: assistantMessageId,
                model: session.model,
                tools: tools,
                options: request.options ?: [:],
                messages: currentContextMessages
            )
            providerRequest.messages.addAll(historyMessages)
            List<ChatEventDto> initialProviderRequestEvents = appendProviderRequestMessages(
                timeline,
                sessionId,
                assistantMessageId,
                provider.id(),
                session.model ?: '',
                providerRequest.messages,
                providerReplayMetadata(currentContextReplayMetadata, historyMessages.size())
            )

            Flux<ChatEventDto> stream = Flux.from(provider.streamChat(providerRequest))
                .doOnNext {chunk ->
                    if (chunk.type == 'provider_request_message') {
                        storeProviderMessage(assistantMessage, chunk.metadata)
                    }
                }
                .filter {chunk -> !['done', 'message_complete'].contains(chunk.type)}
                .map {chunk ->
                    String eventType = eventTypeForChunk(chunk)
                    Map<String, Object> eventMetadata = chunk.metadata ? new LinkedHashMap<String, Object>(chunk.metadata) : [:]
                    if (chunk.type == 'provider_request_message' && !eventMetadata.containsKey('source')) {
                        eventMetadata.put('source', 'tool_loop')
                    }
                    if (chunk.type == 'provider_request_message' && !eventMetadata.containsKey('replayMode')) {
                        eventMetadata.put('replayMode', 'replay')
                    }
                    synchronized (assistantMessage) {
                        if (chunk.type == 'token') {
                            assistantMessage.content = (assistantMessage.content ?: '') + (chunk.content ?: '')
                        } else if (chunk.type == 'error') {
                            assistantMessage.status = 'error'
                            assistantMessage.metadata.put('error', chunk.content ?: 'Unknown error')
                        } else if (chunk.type == 'tool_call' || chunk.type == 'tool_result') {
                            List<Map<String, Object>> toolEvents = (List<Map<String, Object>>) assistantMessage.metadata.get('toolEvents')
                            if (toolEvents == null) {
                                toolEvents = new ArrayList<Map<String, Object>>()
                                assistantMessage.metadata.put('toolEvents', toolEvents)
                            }
                            toolEvents.add([
                                type      : chunk.type,
                                at        : ChatInMemoryStore.now().toString(),
                                attributes: eventMetadata
                            ])
                        }
                        assistantMessage.updatedAt = ChatInMemoryStore.now().toString()
                    }
                    ChatEventDto event = new ChatEventDto(
                        type: eventType,
                        messageId: chunk.messageId ?: assistantMessageId,
                        role: roleForChunk(chunk),
                        content: contentForChunk(chunk),
                        done: false,
                        metadata: eventMetadata
                    )
                    appendChatEvent(timeline, sessionId, event)
                    event
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
                Flux.fromIterable(initialProviderRequestEvents),
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
                    assistantMessage.thinkingContent = ''
                    assistantMessage.updatedAt = ChatInMemoryStore.now().toString()
                }
                appendChatEvent(timeline, sessionId, new ChatEventDto(
                    type: 'message_complete',
                    messageId: assistantMessageId,
                    role: 'assistant',
                    content: '',
                    done: false,
                    metadata: [timestamp: ChatInMemoryStore.now().toString()]
                ))
                appendChatEvent(timeline, sessionId, new ChatEventDto(
                    type: 'done',
                    messageId: assistantMessageId,
                    role: 'assistant',
                    content: '',
                    done: true,
                    metadata: [:]
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

    private static String buildPrerequisiteSkillInstruction(List<ChatSkillDefinition> skills, String userContent) {
        if (!skills || userContent == null || userContent.trim().isEmpty()) {
            return ''
        }

        List<ChatSkillDefinition> matchedSkills = new ArrayList<ChatSkillDefinition>()
        for (ChatSkillDefinition skill : sortSkillsByPriority(skills)) {
            if (skill == null || 'PERSONA'.equalsIgnoreCase(skill.type) || (skill.instruction ?: '').trim().isEmpty()) {
                continue
            }
            if (requiredApplicabilityMatches(skill, userContent) && !matchedSkills.any {ChatSkillDefinition existing -> existing.id == skill.id}) {
                matchedSkills.add(skill)
            }
        }
        if (matchedSkills.isEmpty()) {
            return ''
        }

        StringBuilder builder = new StringBuilder(2048)
        builder.append('Required Mauro skill context for this turn. ')
            .append('The backend selected this context from skill-owned tool prerequisites before tool use. ')
            .append('Apply it when choosing tool arguments and domainTypes; do not ask the user to confirm use of these skills.')

        for (ChatSkillDefinition skill : matchedSkills) {
            builder.append('\n\n## ')
                .append(skill.name ?: skill.id)
                .append(' (')
                .append(skill.id)
                .append(')\n')
                .append(skill.instruction.trim())
        }
        builder.toString()
    }

    private static boolean requiredApplicabilityMatches(ChatSkillDefinition skill, String userContent) {
        for (SkillToolApplicability applicability : skill.toolApplicability ?: []) {
            if (applicability == null || !isRequiredPrerequisite(applicability.relationship)) {
                continue
            }
            // Notice: these are specific term matches rather than semantic matches
            List<String> triggerTerms = applicability.triggerTerms ?: []
            if (triggerTerms.isEmpty()) {
                triggerTerms = skill.keywords ?: []
            }
            if (anyTermMatches(userContent, triggerTerms)) {
                return true
            }
        }
        false
    }

    private static boolean isRequiredPrerequisite(String relationship) {
        String value = relationship == null ? '' : relationship.trim().toUpperCase(Locale.ROOT)
        value == 'REQUIRED_PREREQUISITE' || value == 'REQUIRED_WHEN_MATCHES'
    }

    private static boolean anyTermMatches(String text, List<String> terms) {
        if (text == null || terms == null || terms.isEmpty()) {
            return false
        }
        String lowerText = text.toLowerCase(Locale.ROOT)
        for (String term : terms) {
            if (termMatches(lowerText, term)) {
                return true
            }
        }
        false
    }

    private static boolean termMatches(String lowerText, String term) {
        if (term == null || term.trim().isEmpty()) {
            return false
        }
        String lowerTerm = term.toLowerCase(Locale.ROOT).trim()
        if (lowerTerm.contains(' ')) {
            return lowerText.contains(lowerTerm)
        }
        lowerText ==~ /(?s).*(^|[^a-z0-9])${java.util.regex.Pattern.quote(lowerTerm)}([^a-z0-9]|$).*/
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

        List<String> toolApplicabilityRoutes = buildToolApplicabilityRoutes(skills)
        if (!toolApplicabilityRoutes.isEmpty()) {
            builder.append('\n\nTool prerequisite/context routes:')
            builder.append('\nBefore calling a tool, check these skill-owned routes. REQUIRED_PREREQUISITE skills must be retrieved first when their useWhen matches. RECOMMENDED_PREREQUISITE and RECOMMENDED_CONTEXT skills should be retrieved when their useWhen matches unless the needed context is already present.')
            for (String route : toolApplicabilityRoutes) {
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

    private static List<ChatSkillDefinition> sortSkillsByPriority(List<ChatSkillDefinition> skills) {
        new ArrayList<ChatSkillDefinition>(skills ?: [])
            .sort {ChatSkillDefinition left, ChatSkillDefinition right ->
                Integer leftPriority = left.priority != null ? left.priority : Integer.valueOf(1000)
                Integer rightPriority = right.priority != null ? right.priority : Integer.valueOf(1000)
                int priorityCompare = leftPriority <=> rightPriority
                priorityCompare != 0 ? priorityCompare : (left.id ?: '') <=> (right.id ?: '')
            } as List<ChatSkillDefinition>
    }

    private static List<String> buildSkillRoutes(List<ChatSkillDefinition> skills) {
        if (!skills) {
            return []
        }
        List<ChatSkillDefinition> routedSkills = sortSkillsByPriority(skills)
            .findAll {ChatSkillDefinition skill ->
                !'PERSONA'.equalsIgnoreCase(skill.type) &&
                    skill.routing != null &&
                    (!(skill.routing.useWhen ?: []).isEmpty() ||
                        !(skill.routing.avoidWhen ?: []).isEmpty() ||
                        !(skill.routing.examples ?: []).isEmpty())
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
            if (!(skill.seeAlso ?: []).isEmpty()) {
                route.append(' See also: ')
                    .append(skill.seeAlso.join(', '))
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

    private static List<String> buildToolApplicabilityRoutes(List<ChatSkillDefinition> skills) {
        if (!skills) {
            return []
        }

        List<ChatSkillDefinition> applicableSkills = sortSkillsByPriority(skills)
            .findAll {ChatSkillDefinition skill ->
                !'PERSONA'.equalsIgnoreCase(skill.type) && !(skill.toolApplicability ?: []).isEmpty()
            }

        List<String> routes = new ArrayList<String>()
        for (ChatSkillDefinition skill : applicableSkills) {
            for (SkillToolApplicability applicability : skill.toolApplicability ?: []) {
                if (applicability == null || applicability.tool == null || applicability.tool.trim().isEmpty()) {
                    continue
                }
                StringBuilder route = new StringBuilder(768)
                route.append(applicability.tool)
                    .append(' <- ')
                    .append(skill.name)
                    .append(' (')
                    .append(skill.id)
                    .append(', ')
                    .append(applicability.relationship ?: 'RECOMMENDED_PREREQUISITE')
                    .append('): ')
                    .append(skill.description)
                    .append('.')
                if (!(applicability.useWhen ?: []).isEmpty()) {
                    route.append(' Use when: ')
                        .append(applicability.useWhen.join('; '))
                        .append('.')
                }
                if (!(applicability.triggerTerms ?: []).isEmpty()) {
                    route.append(' Trigger terms: ')
                        .append(applicability.triggerTerms.join(', '))
                        .append('.')
                }
                if (!(applicability.avoidWhen ?: []).isEmpty()) {
                    route.append(' Avoid when: ')
                        .append(applicability.avoidWhen.join('; '))
                        .append('.')
                }
                if (!(applicability.instructions ?: []).isEmpty()) {
                    route.append(' Instructions: ')
                        .append(applicability.instructions.join('; '))
                        .append('.')
                }
                route.append(' Retrieve with skill_lookup using arguments ')
                    .append(JsonOutput.toJson([id: skill.id, includeInstruction: true]))
                    .append('.')
                if (!(applicability.examples ?: []).isEmpty()) {
                    route.append(' Examples: ')
                        .append(applicability.examples.take(3).join(' | '))
                        .append('.')
                }
                routes.add(route.toString())
            }
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
        StringBuilder assistantBuffer = new StringBuilder(1024)
        for (MessageDto message : timeline) {
            if (message == null) {
                continue
            }
            if (message.status == 'event') {
                String eventType = asString(message.metadata?.get('eventType'))
                if (eventType == 'ui_user_message') {
                    if (assistantBuffer.length() > 0) {
                        history.add(new ProviderMessage(role: 'assistant', content: assistantBuffer.toString()))
                        assistantBuffer.setLength(0)
                    }
                    history.add(new ProviderMessage(role: 'user', content: message.content ?: ''))
                    continue
                }
                if (eventType == 'token') {
                    assistantBuffer.append(message.content ?: '')
                    continue
                }
                if (eventType == 'message_complete') {
                    if (assistantBuffer.length() > 0) {
                        history.add(new ProviderMessage(role: 'assistant', content: assistantBuffer.toString()))
                        assistantBuffer.setLength(0)
                    }
                    continue
                }
                if (eventType == 'provider_request_message' && shouldReplayProviderRequestMessage(message.metadata)) {
                    history.addAll(providerMessagesFromMaps([message.metadata?.get('providerMessage')]))
                }
                continue
            }
            if (assistantBuffer.length() > 0) {
                history.add(new ProviderMessage(role: 'assistant', content: assistantBuffer.toString()))
                assistantBuffer.setLength(0)
            }
            if (message.role == 'user') {
                history.addAll(providerMessagesFromMaps(message.metadata?.get('providerMessagesBefore')))
            }
            if (message.role == 'assistant') {
                history.addAll(providerMessagesFromMaps(message.metadata?.get('providerMessages')))
            }
            if (message.role == 'user' || message.role == 'assistant' || message.role == 'tool') {
                if (message.content != null && !message.content.trim().isEmpty()) {
                    history.add(new ProviderMessage(role: message.role, content: message.content))
                }
            }
            String toolMemory = buildToolHistoryMemory(message)
            if (message.metadata?.get('providerMessages') == null && toolMemory != null && !toolMemory.trim().isEmpty()) {
                history.add(new ProviderMessage(role: 'system', content: toolMemory))
            }
        }
        if (assistantBuffer.length() > 0) {
            history.add(new ProviderMessage(role: 'assistant', content: assistantBuffer.toString()))
        }
        history
    }

    private static boolean shouldReplayProviderRequestMessage(Map<String, Object> metadata) {
        if (metadata == null) {
            return false
        }
        String replayMode = asString(metadata.get('replayMode'))
        if (replayMode != null && !replayMode.trim().isEmpty()) {
            return 'replay'.equalsIgnoreCase(replayMode)
        }
        // Legacy event logs recorded only source. Tool-loop provider messages were
        // atomic LLM-visible messages; initial_request rows were whole request snapshots.
        asString(metadata.get('source')) != 'initial_request'
    }

    private static void appendChatEvent(List<MessageDto> timeline, String sessionId, ChatEventDto event) {
        if (timeline == null || event == null || event.type == null || event.type.trim().isEmpty()) {
            return
        }
        MessageDto last = timeline.isEmpty() ? null : timeline.get(timeline.size() - 1)
        if (canCollate(last, event)) {
            last.content = (last.content ?: '') + (event.content ?: '')
            last.updatedAt = ChatInMemoryStore.now().toString()
            return
        }

        Instant now = ChatInMemoryStore.now()
        Map<String, Object> metadata = event.metadata ? new LinkedHashMap<String, Object>(event.metadata) : [:]
        metadata.put('eventType', event.type)
        metadata.put('messageId', event.messageId)
        metadata.put('done', Boolean.TRUE.equals(event.done))
        timeline.add(new MessageDto(
            id: UUID.randomUUID().toString(),
            sessionId: sessionId,
            role: event.role ?: 'assistant',
            content: event.content ?: '',
            status: 'event',
            thinkingContent: '',
            createdAt: now.toString(),
            updatedAt: now.toString(),
            metadata: metadata
        ))
    }

    private static void addProviderContextMessage(
        List<ProviderMessage> messages,
        List<Map<String, Object>> replayMetadata,
        String content,
        String source,
        String replayMode,
        String substitutionKey
    ) {
        if (content == null || content.trim().isEmpty()) {
            return
        }
        messages.add(new ProviderMessage(role: 'system', content: content))
        Map<String, Object> metadata = [
            source    : source,
            replayMode: replayMode
        ] as Map<String, Object>
        if (substitutionKey != null && !substitutionKey.trim().isEmpty()) {
            metadata.put('substitutionKey', substitutionKey)
        }
        replayMetadata.add(metadata)
    }

    private static List<Map<String, Object>> providerReplayMetadata(
        List<Map<String, Object>> currentContextReplayMetadata,
        int historyMessageCount
    ) {
        List<Map<String, Object>> replayMetadata = new ArrayList<Map<String, Object>>()
        for (Map<String, Object> metadata : currentContextReplayMetadata ?: []) {
            replayMetadata.add(new LinkedHashMap<String, Object>(metadata ?: [:]))
        }
        for (int i = 0; i < historyMessageCount; i++) {
            replayMetadata.add([
                source    : 'projected_history',
                replayMode: 'omit'
            ] as Map<String, Object>)
        }
        replayMetadata
    }

    private static List<ChatEventDto> appendProviderRequestMessages(
        List<MessageDto> timeline,
        String sessionId,
        String assistantMessageId,
        String provider,
        String model,
        List<ProviderMessage> messages,
        List<Map<String, Object>> replayMetadata
    ) {
        if (messages == null || messages.isEmpty()) {
            return []
        }
        List<ChatEventDto> events = new ArrayList<ChatEventDto>(messages.size())
        for (int i = 0; i < messages.size(); i++) {
            ProviderMessage message = messages.get(i)
            Map<String, Object> providerMessage = providerMessageToMap(message)
            if (providerMessage.isEmpty()) {
                continue
            }
            ChatEventDto event = new ChatEventDto(
                type: 'provider_request_message',
                messageId: assistantMessageId,
                role: String.valueOf(providerMessage.get('role') ?: 'assistant'),
                content: String.valueOf(providerMessage.get('content') ?: ''),
                done: true,
                metadata: ([
                    provider            : provider,
                    model               : model,
                    providerMessageIndex: i,
                    providerMessage     : providerMessage
                ] + providerReplayMetadataForIndex(replayMetadata, i)) as Map<String, Object>
            )
            appendChatEvent(timeline, sessionId, event)
            events.add(event)
        }
        events
    }

    private static Map<String, Object> providerReplayMetadataForIndex(List<Map<String, Object>> replayMetadata, int index) {
        if (replayMetadata != null && index >= 0 && index < replayMetadata.size() && replayMetadata.get(index) != null) {
            return new LinkedHashMap<String, Object>(replayMetadata.get(index))
        }
        [
            source    : 'provider_request',
            replayMode: 'omit'
        ] as Map<String, Object>
    }

    private static boolean canCollate(MessageDto last, ChatEventDto event) {
        if (last == null || last.status != 'event' || !['token', 'thinking_token'].contains(event.type)) {
            return false
        }
        last.metadata?.get('eventType') == event.type &&
            last.role == (event.role ?: 'assistant') &&
            last.metadata?.get('messageId') == event.messageId &&
            Boolean.TRUE.equals(last.metadata?.get('done')) == Boolean.TRUE.equals(event.done)
    }

    private static void storeProviderMessage(MessageDto assistantMessage, Map<String, Object> metadata) {
        if (assistantMessage == null || metadata == null) {
            return
        }
        Object providerMessageObj = metadata.get('providerMessage')
        if (!(providerMessageObj instanceof Map)) {
            return
        }
        @SuppressWarnings('unchecked')
        Map<String, Object> providerMessage = (Map<String, Object>) providerMessageObj
        synchronized (assistantMessage) {
            List<Map<String, Object>> providerMessages = (List<Map<String, Object>>) assistantMessage.metadata.get('providerMessages')
            if (providerMessages == null) {
                providerMessages = new ArrayList<Map<String, Object>>()
                assistantMessage.metadata.put('providerMessages', providerMessages)
            }
            providerMessages.add(new LinkedHashMap<String, Object>(providerMessage))
            assistantMessage.updatedAt = ChatInMemoryStore.now().toString()
        }
    }

    private static String eventTypeForChunk(org.maurodata.service.chat.llm.ProviderChunk chunk) {
        if (chunk == null || chunk.type != 'provider_request_message') {
            return chunk?.type
        }
        'provider_request_message'
    }

    private static String roleForChunk(org.maurodata.service.chat.llm.ProviderChunk chunk) {
        if (chunk != null && chunk.type == 'provider_request_message') {
            Map<String, Object> providerMessage = providerMessageFromMetadata(chunk.metadata)
            return String.valueOf(providerMessage.get('role') ?: 'assistant')
        }
        'assistant'
    }

    private static String contentForChunk(org.maurodata.service.chat.llm.ProviderChunk chunk) {
        if (chunk == null) {
            return ''
        }
        if (chunk.type == 'error') {
            return "Provider error: ${chunk.content ?: 'Unknown error'}"
        }
        if (chunk.type == 'provider_request_message') {
            Map<String, Object> providerMessage = providerMessageFromMetadata(chunk.metadata)
            return String.valueOf(providerMessage.get('content') ?: '')
        }
        chunk.content ?: ''
    }

    private static Map<String, Object> providerMessageFromMetadata(Map<String, Object> metadata) {
        if (metadata == null) {
            return [:] as Map<String, Object>
        }
        Object providerMessageObj = metadata.get('providerMessage')
        if (providerMessageObj instanceof Map) {
            @SuppressWarnings('unchecked')
            Map<String, Object> providerMessage = (Map<String, Object>) providerMessageObj
            return new LinkedHashMap<String, Object>(providerMessage)
        }
        if (metadata.containsKey('role') || metadata.containsKey('content') || metadata.containsKey('toolCalls')) {
            return new LinkedHashMap<String, Object>(metadata)
        }
        [:] as Map<String, Object>
    }

    private static List<Map<String, Object>> providerMessagesToMaps(List<ProviderMessage> messages) {
        if (messages == null || messages.isEmpty()) {
            return []
        }
        List<Map<String, Object>> out = new ArrayList<Map<String, Object>>(messages.size())
        for (ProviderMessage message : messages) {
            Map<String, Object> map = providerMessageToMap(message)
            if (!map.isEmpty()) {
                out.add(map)
            }
        }
        out
    }

    private static Map<String, Object> providerMessageToMap(ProviderMessage message) {
        if (message == null || message.role == null || message.role.trim().isEmpty()) {
            return [:] as Map<String, Object>
        }
        Map<String, Object> out = new LinkedHashMap<String, Object>()
        out.put('role', message.role)
        out.put('content', message.content ?: '')
        if (message.toolCallId != null) {
            out.put('toolCallId', message.toolCallId)
        }
        if (message.name != null) {
            out.put('name', message.name)
        }
        if (message.toolCalls != null && !message.toolCalls.isEmpty()) {
            out.put('toolCalls', message.toolCalls)
        }
        out
    }

    private static List<ProviderMessage> providerMessagesFromMaps(Object value) {
        if (!(value instanceof List)) {
            return []
        }
        List<ProviderMessage> messages = new ArrayList<ProviderMessage>()
        for (Object item : (List<?>) value) {
            if (!(item instanceof Map)) {
                continue
            }
            @SuppressWarnings('unchecked')
            Map<String, Object> map = (Map<String, Object>) item
            String role = asString(map.get('role'))
            if (role == null || role.trim().isEmpty()) {
                continue
            }
            ProviderMessage message = new ProviderMessage(role: role, content: asString(map.get('content')) ?: '')
            message.toolCallId = asString(map.get('toolCallId'))
            message.name = asString(map.get('name'))
            Object toolCallsObj = map.get('toolCalls')
            if (toolCallsObj instanceof List) {
                @SuppressWarnings('unchecked')
                List<Map<String, Object>> toolCalls = (List<Map<String, Object>>) toolCallsObj
                message.toolCalls = toolCalls
            }
            messages.add(message)
        }
        messages
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

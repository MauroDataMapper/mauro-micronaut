package org.maurodata.service.chat

import org.maurodata.service.chat.llm.ProviderChunk

import groovy.json.JsonOutput
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.micronaut.context.annotation.Value
import io.micronaut.http.HttpHeaders
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpStatus
import io.micronaut.http.exceptions.HttpStatusException
import jakarta.inject.Singleton
import org.maurodata.plugin.chat.api.chat.ChatEventDto
import org.maurodata.plugin.chat.api.chat.CreateSessionRequest
import org.maurodata.plugin.chat.api.chat.ListSessionMessagesResponseDto
import org.maurodata.plugin.chat.api.chat.MessageDto
import org.maurodata.plugin.chat.api.chat.SendMessageRequest
import org.maurodata.plugin.chat.api.chat.SessionDto
import org.maurodata.plugin.chat.api.chat.UpdateSessionRequest
import org.maurodata.service.chat.agent.AgentSupervisorService
import org.maurodata.service.chat.llm.LlmProvider
import org.maurodata.service.chat.llm.ProviderMessage
import org.maurodata.service.chat.llm.ProviderRegistry
import org.maurodata.service.chat.llm.ProviderRequest
import org.reactivestreams.Publisher
import reactor.core.publisher.Flux

import java.time.Instant
import java.util.regex.Pattern

@Slf4j
@Singleton
@CompileStatic
class ChatSessionsApiService implements ChatSessionService {

    private final ChatInMemoryStore store
    private final ProviderRegistry providerRegistry
    private final ChatMcpService chatMcpService
    private final ChatPromptAssetService promptAssetService
    private final ChatPromptResourceService promptResourceService
    private final AgentSupervisorService agentSupervisorService
    private final String defaultModel
    private final String defaultMode

    ChatSessionsApiService(
        ChatInMemoryStore store,
        ProviderRegistry providerRegistry,
        ChatMcpService chatMcpService,
        ChatPromptAssetService promptAssetService,
        ChatPromptResourceService promptResourceService,
        AgentSupervisorService agentSupervisorService,
        @Value('${chat.providers.default-model:llama3.1}') String defaultModel,
        @Value('${chat.agent.default-mode:chat}') String defaultMode
    ) {
        this.store = store
        this.providerRegistry = providerRegistry
        this.chatMcpService = chatMcpService
        this.promptAssetService = promptAssetService
        this.promptResourceService = promptResourceService
        this.agentSupervisorService = agentSupervisorService
        this.defaultModel = defaultModel
        this.defaultMode = defaultMode ?: 'chat'
    }

    ChatSessionsApiService(
        ChatInMemoryStore store,
        ProviderRegistry providerRegistry,
        ChatMcpService chatMcpService,
        ChatPromptAssetService promptAssetService,
        ChatPromptResourceService promptResourceService,
        AgentSupervisorService agentSupervisorService,
        String defaultModel
    ) {
        this(store, providerRegistry, chatMcpService, promptAssetService, promptResourceService, agentSupervisorService, defaultModel, 'chat')
    }

    ChatSessionsApiService(
        ChatInMemoryStore store,
        ProviderRegistry providerRegistry,
        ChatMcpService chatMcpService,
        ChatPromptAssetService promptAssetService,
        ChatPromptResourceService promptResourceService,
        String defaultModel
    ) {
        this(store, providerRegistry, chatMcpService, promptAssetService, promptResourceService, null, defaultModel, 'chat')
    }

    @Override
    SessionDto createSession(CreateSessionRequest request) {
        Instant now = ChatInMemoryStore.now()
        String id = UUID.randomUUID().toString()
        String requestedTitle = request.title == null || request.title.trim().isEmpty() ? null : request.title.trim()
        SessionDto session = new SessionDto(
            id: id,
            workspaceId: request.workspaceId,
            title: requestedTitle,
            status: 'ACTIVE',
            model: request.model ?: defaultModel,
            createdAt: now,
            updatedAt: now,
            metadata: [
                titleSetByUser: requestedTitle != null,
                titleSource   : requestedTitle == null ? null : 'user'
            ] as Map<String, Object>
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
    SessionDto updateSession(String sessionId, UpdateSessionRequest request) {
        SessionDto session = getSession(sessionId)
        String title = request?.title
        session.title = title == null || title.trim().isEmpty() ? null : title.trim()
        session.metadata = session.metadata ?: [:]
        session.metadata.put('titleSetByUser', session.title != null)
        session.metadata.put('titleSource', session.title == null ? null : 'user')
        session.updatedAt = ChatInMemoryStore.now()
        session
    }

    @Override
    Publisher<ChatEventDto> sendMessage(String sessionId, SendMessageRequest request) {
        sendMessage(sessionId, request, null)
    }

    @Override
    Publisher<ChatEventDto> sendMessage(String sessionId, SendMessageRequest request, HttpRequest<?> httpRequest) {
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
            metadata: [attachments: request.attachments ?: [], contextRefs: request.contextRefs ?: []] as Map<String, Object>
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
            metadata: [provider: '', model: session.model ?: ''] as Map<String, Object>
        )
        appendChatEvent(timeline, sessionId, new ChatEventDto(
            type: 'message_start',
            messageId: assistantMessageId,
            role: 'assistant',
            content: '',
            done: false,
            metadata: [sessionId: sessionId] as Map<String, Object>
        ))
        log.info('sendMessage sessionId={} requestMessageId={}', sessionId, request.messageId)
        try {
            def provider = providerRegistry.byModel(session.model)
            assistantMessage.metadata.put('provider', provider.id())
            if (agentMode(request)) {
                if (agentSupervisorService == null) {
                    throw new IllegalStateException('Agent mode is enabled but AgentSupervisorService is not available')
                }
                Flux<ChatEventDto> agentStream = Flux.from(agentSupervisorService.streamAgentRun(
                    session,
                    request,
                    assistantMessageId,
                    timeline,
                    httpRequest
                )).map {ChatEventDto event ->
                    if (event.type == 'token') {
                        synchronized (assistantMessage) {
                            assistantMessage.content = (assistantMessage.content ?: '') + (event.content ?: '')
                            assistantMessage.updatedAt = ChatInMemoryStore.now().toString()
                        }
                    } else if (event.type == 'error') {
                        synchronized (assistantMessage) {
                            assistantMessage.status = 'error'
                            assistantMessage.metadata.put('error', event.content ?: 'Unknown error')
                            assistantMessage.updatedAt = ChatInMemoryStore.now().toString()
                        }
                    }
                    event
                }

                session.updatedAt = ChatInMemoryStore.now()
                return Flux.concat(
                    Flux.just(new ChatEventDto(
                        type: 'message_start',
                        messageId: assistantMessageId,
                        role: 'assistant',
                        content: '',
                        done: false,
                        metadata: [sessionId: session.id, provider: provider.id(), mode: 'agent'] as Map<String, Object>
                    )),
                    agentStream,
                    Flux.defer {
                        ChatEventDto titleEvent = generateInitialTitleEvent(timeline, session, assistantMessageId, provider, request.content ?: '', assistantMessage.content ?: '')
                        titleEvent == null ? Flux.empty() : Flux.just(titleEvent)
                    },
                    Flux.just(new ChatEventDto(
                        type: 'message_complete',
                        messageId: assistantMessageId,
                        role: 'assistant',
                        content: '',
                        done: false,
                        metadata: [timestamp: ChatInMemoryStore.now().toString(), mode: 'agent'] as Map<String, Object>
                    )),
                    Flux.just(new ChatEventDto(
                        type: 'done',
                        messageId: assistantMessageId,
                        role: 'assistant',
                        content: '',
                        done: true,
                        metadata: [mode: 'agent'] as Map<String, Object>
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
                        metadata: [timestamp: ChatInMemoryStore.now().toString(), mode: 'agent'] as Map<String, Object>
                    ))
                    appendChatEvent(timeline, sessionId, new ChatEventDto(
                        type: 'done',
                        messageId: assistantMessageId,
                        role: 'assistant',
                        content: '',
                        done: true,
                        metadata: [mode: 'agent'] as Map<String, Object>
                    ))
                } as Publisher<ChatEventDto>
            }
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
	                ]} as List<Map<String, Object>>
            List<ChatPromptAssetDefinition> skillDefinitions = promptAssetService.listAssetsByType('SKILL') ?: []
            String personaInstruction = buildPersonaInstruction(promptAssetService.listAssetsByType('PERSONA') ?: [])
            String toolInstruction = buildToolInstruction(tools, promptResourceService)
            String routingInstruction = buildRoutingInstruction(skillDefinitions, tools)
            String prerequisiteInstruction = buildPrerequisiteSkillInstruction(skillDefinitions, request.content ?: '')

            List<ProviderMessage> historyMessages = buildProviderHistory(timeline)
            List<ProviderMessage> providerMessages = new ArrayList<ProviderMessage>()
            List<Map<String, Object>> providerMessageReplayMetadata = new ArrayList<Map<String, Object>>()
            addProviderContextMessage(providerMessages, providerMessageReplayMetadata, personaInstruction, 'persona', 'substitute', 'persona:active')
            addProviderContextMessage(providerMessages, providerMessageReplayMetadata, toolInstruction, 'tool_policy', 'substitute', 'tool_policy:active')
            addProviderContextMessage(providerMessages, providerMessageReplayMetadata, routingInstruction, 'routing', 'substitute', 'routing:index')
            int currentUserPromptIndex = lastCurrentUserPromptIndex(historyMessages, request.content ?: '')
            if (currentUserPromptIndex >= 0) {
                addProjectedHistoryMessages(providerMessages, providerMessageReplayMetadata, historyMessages, 0, currentUserPromptIndex)
                addProviderContextMessage(providerMessages, providerMessageReplayMetadata, prerequisiteInstruction, 'skill_prerequisite', 'replay', null)
                addProjectedHistoryMessages(providerMessages, providerMessageReplayMetadata, historyMessages, currentUserPromptIndex, historyMessages.size())
            } else {
                addProjectedHistoryMessages(providerMessages, providerMessageReplayMetadata, historyMessages, 0, historyMessages.size())
                addProviderContextMessage(providerMessages, providerMessageReplayMetadata, prerequisiteInstruction, 'skill_prerequisite', 'replay', null)
            }
            ProviderRequest providerRequest = new ProviderRequest(
                sessionId: session.id,
                messageId: assistantMessageId,
                model: session.model,
                tools: tools,
                options: providerOptions(request.options ?: [:], httpRequest),
                messages: providerMessages
            )
            List<ChatEventDto> initialProviderRequestEvents = appendProviderRequestMessages(
                timeline,
                sessionId,
                assistantMessageId,
                provider.id(),
                session.model ?: '',
                providerRequest.messages,
                providerMessageReplayMetadata
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
                    metadata: [sessionId: session.id, provider: provider.id()] as Map<String, Object>
                )),
                Flux.fromIterable(initialProviderRequestEvents),
                stream,
                Flux.defer {
                    ChatEventDto titleEvent = generateInitialTitleEvent(timeline, session, assistantMessageId, provider, request.content ?: '', assistantMessage.content ?: '')
                    titleEvent == null ? Flux.empty() : Flux.just(titleEvent)
                },
                Flux.just(new ChatEventDto(
                    type: 'message_complete',
                    messageId: assistantMessageId,
                    role: 'assistant',
                    content: '',
                    done: false,
                    metadata: [timestamp: ChatInMemoryStore.now().toString()] as Map<String, Object>
                )),
                Flux.just(new ChatEventDto(
                    type: 'done',
                    messageId: assistantMessageId,
                    role: 'assistant',
                    content: '',
                    done: true,
                    metadata: [:] as Map<String, Object>
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
                    metadata: [timestamp: ChatInMemoryStore.now().toString()] as Map<String, Object>
                ))
                appendChatEvent(timeline, sessionId, new ChatEventDto(
                    type: 'done',
                    messageId: assistantMessageId,
                    role: 'assistant',
                    content: '',
                    done: true,
                    metadata: [:] as Map<String, Object>
                ))
            } as Publisher<ChatEventDto>
        } finally {
            log.info('sendMessage completed sessionId={} durationMs={}', sessionId, System.currentTimeMillis() - start)
        }
    }

    private boolean agentMode(SendMessageRequest request) {
        Object mode = request?.options?.get('mode')
        String resolvedMode = mode == null || String.valueOf(mode).trim().isEmpty() ? defaultMode : String.valueOf(mode)
        'agent'.equalsIgnoreCase(resolvedMode)
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

    private static String buildPersonaInstruction(List<ChatPromptAssetDefinition> personas) {
        if (!personas) {
            return ''
        }
        StringBuilder builder = new StringBuilder(1024)
        for (int i = 0; i < personas.size(); i++) {
            ChatPromptAssetDefinition persona = personas.get(i)
            if (persona.instruction != null && !persona.instruction.trim().isEmpty()) {
                if (builder.length() > 0) {
                    builder.append('\n\n')
                }
                builder.append(persona.instruction.trim())
            }
        }
        builder.toString()
    }

    private static Map<String, Object> providerOptions(Map<String, Object> requestOptions, HttpRequest<?> httpRequest) {
        Map<String, Object> options = new LinkedHashMap<String, Object>(requestOptions ?: [:])
        Map<String, List<String>> headers = forwardedHeaders(httpRequest)
        if (!headers.isEmpty()) {
            options.put('_mauroForwardHeaders', headers)
        }
        options
    }

    private static Map<String, List<String>> forwardedHeaders(HttpRequest<?> httpRequest) {
        Map<String, List<String>> headers = new LinkedHashMap<String, List<String>>()
        if (httpRequest == null) {
            return headers
        }
        copyForwardedHeader(httpRequest, headers, 'apiKey')
        copyForwardedHeader(httpRequest, headers, HttpHeaders.COOKIE)
        copyForwardedHeader(httpRequest, headers, HttpHeaders.AUTHORIZATION)
        headers
    }

    private static void copyForwardedHeader(HttpRequest<?> httpRequest, Map<String, List<String>> headers, String name) {
        List<String> values = httpRequest.headers.getAll(name)
            .findAll {String value -> value != null && !value.trim().isEmpty()} as List<String>
        if (!values.isEmpty()) {
            headers.put(name, values)
        }
    }

    private static String buildPrerequisiteSkillInstruction(List<ChatPromptAssetDefinition> skills, String userContent) {
        if (!skills || userContent == null || userContent.trim().isEmpty()) {
            return ''
        }

        List<ChatPromptAssetDefinition> matchedSkills = new ArrayList<ChatPromptAssetDefinition>()
        for (ChatPromptAssetDefinition skill : sortSkillsByPriority(skills)) {
            if (skill == null || 'PERSONA'.equalsIgnoreCase(skill.type) || (skill.instruction ?: '').trim().isEmpty()) {
                continue
            }
            if (requiredApplicabilityMatches(skill, userContent) && !matchedSkills.any {ChatPromptAssetDefinition existing -> existing.id == skill.id}) {
                matchedSkills.add(skill)
            }
        }
        if (matchedSkills.isEmpty()) {
            return ''
        }

        StringBuilder builder = new StringBuilder(2048)
        builder.append('# Required skill')
            .append('\n- The following skill has been selected; informing the use of the available tools.')
            .append('\n- Apply this skill when choosing tool arguments.')
            .append('\n- Use these selected skills directly when choosing tool arguments.')

        for (ChatPromptAssetDefinition skill : matchedSkills) {
            builder.append('\n\n## ')
                .append(skill.name ?: skill.id)
                .append(' (')
                .append(skill.id)
                .append(')\n')
                .append(skill.instruction.trim())
        }
        builder.toString()
    }

    private static boolean requiredApplicabilityMatches(ChatPromptAssetDefinition skill, String userContent) {
        for (SkillToolApplicability applicability : (skill.toolApplicability ?: [] as List<SkillToolApplicability>)) {
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
        lowerText ==~ /(?s).*(^|[^a-z0-9])${Pattern.quote(lowerTerm)}([^a-z0-9]|$).*/
    }

    private static String buildRoutingInstruction(List<ChatPromptAssetDefinition> skills, List<Map<String, Object>> tools) {
        StringBuilder builder = new StringBuilder(2048)
        builder.append('# Routing index')
        builder.append('\n- Use this index to choose whether to call a tool or retrieve a skill.')
        builder.append('\n- Not every skill is listed here.')
        builder.append('\n- Skills may be GENERAL, SPECIFIC, or FALLBACK.')
        builder.append('\n- Prefer the SPECIFIC skill route, using GENERAL or FALLBACK routes only when no SPECIFIC route matches well.')

        List<String> skillRoutes = buildSkillRoutes(skills)
        if (!skillRoutes.isEmpty()) {
            builder.append('\n\n## Skill routes')
            for (String route : skillRoutes) {
                builder.append('\n\n')
                    .append(route)
            }
        }

        List<String> toolApplicabilityRoutes = buildToolApplicabilityRoutes(skills)
        if (!toolApplicabilityRoutes.isEmpty()) {
            builder.append('\n\n## Tool prerequisite/context routes')
            builder.append('\n- Before calling a tool, check these skill-owned routes.')
            builder.append('\n- REQUIRED_PREREQUISITE skills must be retrieved first when their Use when conditions match.')
            builder.append('\n- RECOMMENDED_PREREQUISITE and RECOMMENDED_CONTEXT skills should be retrieved when their Use when conditions match unless the needed context is already present.')
            for (String route : toolApplicabilityRoutes) {
                builder.append('\n\n')
                    .append(route)
            }
        }

        List<String> toolRoutes = buildToolRoutes(tools)
        if (!toolRoutes.isEmpty()) {
            builder.append('\n\n## Tool routes')
            for (String route : toolRoutes) {
                builder.append('\n\n')
                    .append(route)
            }
        }
        builder.toString()
    }

    private static List<ChatPromptAssetDefinition> sortSkillsByPriority(List<ChatPromptAssetDefinition> skills) {
        new ArrayList<ChatPromptAssetDefinition>(skills ?: [])
            .sort {ChatPromptAssetDefinition left, ChatPromptAssetDefinition right ->
                Integer leftPriority = left.priority != null ? left.priority : Integer.valueOf(1000)
                Integer rightPriority = right.priority != null ? right.priority : Integer.valueOf(1000)
                int priorityCompare = leftPriority <=> rightPriority
                priorityCompare != 0 ? priorityCompare : (left.id ?: '') <=> (right.id ?: '')
            } as List<ChatPromptAssetDefinition>
    }

    private static List<ChatPromptAssetDefinition> sortSkillsByRouteOrder(List<ChatPromptAssetDefinition> skills) {
        new ArrayList<ChatPromptAssetDefinition>(skills ?: [])
            .sort {ChatPromptAssetDefinition left, ChatPromptAssetDefinition right ->
                int specificityCompare = specificityRank(left?.routing?.specificity) <=> specificityRank(right?.routing?.specificity)
                if (specificityCompare != 0) {
                    return specificityCompare
                }
                Integer leftPriority = left.priority != null ? left.priority : Integer.valueOf(1000)
                Integer rightPriority = right.priority != null ? right.priority : Integer.valueOf(1000)
                int priorityCompare = leftPriority <=> rightPriority
                priorityCompare != 0 ? priorityCompare : (left.id ?: '') <=> (right.id ?: '')
            } as List<ChatPromptAssetDefinition>
    }

    private static int specificityRank(String specificity) {
        String value = specificity == null ? '' : specificity.trim().toUpperCase(Locale.ROOT)
        if (value == 'GENERAL') {
            return 0
        }
        if (value == 'SPECIFIC') {
            return 1
        }
        if (value == 'FALLBACK') {
            return 2
        }
        1
    }

    private static List<String> buildSkillRoutes(List<ChatPromptAssetDefinition> skills) {
        if (!skills) {
            return []
        }
        List<ChatPromptAssetDefinition> routedSkills = sortSkillsByRouteOrder(skills)
            .findAll {ChatPromptAssetDefinition skill ->
                !'PERSONA'.equalsIgnoreCase(skill.type) &&
                    skill.routing != null &&
                    (!(skill.routing.useWhen ?: []).isEmpty() ||
                        !(skill.routing.avoidWhen ?: []).isEmpty() ||
                        !(skill.routing.examples ?: []).isEmpty())
            }

        List<String> routes = new ArrayList<String>()
        for (ChatPromptAssetDefinition skill : routedSkills) {
            SkillRouting routing = skill.routing
            String toolName = routing.toolName ?: 'mauro_skill'
            Map<String, Object> toolArguments = routing.toolArguments && !routing.toolArguments.isEmpty()
                ? routing.toolArguments
                : [id: skill.id, includeInstruction: true] as Map<String, Object>
            StringBuilder route = new StringBuilder(768)
            route.append('### ')
                .append(skill.name)
                .append('\n')
            route.append('id: ')
                .append(skill.id)
                .append('\n')
            route.append('description: ')
                .append(skill.description ?: '')
                .append('\n')
            route.append('Specificity: ')
                .append(routing.specificity ?: 'NORMAL')
            appendMarkdownList(route, 'Use when', routing.useWhen)
            appendMarkdownListOrNone(route, 'Avoid when', routing.avoidWhen)
            appendMarkdownList(route, 'See also', skill.seeAlso)
            appendMarkdownList(route, 'Examples', (routing.examples ?: []).take(3) as List<String>)
            route.append('\nFull instructions:')
                .append('\nCall the tool ')
                .append(toolName)
                .append(' using arguments ')
                .append(JsonOutput.toJson(toolArguments))
                .append('.')
            routes.add(route.toString())
        }
        routes
    }

    private static List<String> buildToolApplicabilityRoutes(List<ChatPromptAssetDefinition> skills) {
        if (!skills) {
            return []
        }

        List<ChatPromptAssetDefinition> applicableSkills = sortSkillsByRouteOrder(skills)
            .findAll {ChatPromptAssetDefinition skill ->
                !'PERSONA'.equalsIgnoreCase(skill.type) && !(skill.toolApplicability ?: []).isEmpty()
            }

        List<String> routes = new ArrayList<String>()
        for (ChatPromptAssetDefinition skill : applicableSkills) {
            for (SkillToolApplicability applicability : (skill.toolApplicability ?: [] as List<SkillToolApplicability>)) {
                if (applicability == null || applicability.tool == null || applicability.tool.trim().isEmpty()) {
                    continue
                }
                StringBuilder route = new StringBuilder(768)
                route.append('### ')
                    .append(applicability.tool)
                    .append(' <- ')
                    .append(skill.name)
                    .append('\n')
                route.append('skill id: ')
                    .append(skill.id)
                    .append('\n')
                route.append('description: ')
                    .append(skill.description ?: '')
                    .append('\n')
                route.append('Specificity: ')
                    .append(skill.routing?.specificity ?: 'NORMAL')
                    .append('\n')
                route.append('Relationship: ')
                    .append(applicability.relationship ?: 'RECOMMENDED_PREREQUISITE')
                appendMarkdownList(route, 'Use when', applicability.useWhen)
                appendMarkdownList(route, 'Trigger terms', applicability.triggerTerms)
                appendMarkdownListOrNone(route, 'Avoid when', applicability.avoidWhen)
                appendMarkdownList(route, 'Instructions', applicability.instructions)
                appendMarkdownList(route, 'Examples', (applicability.examples ?: []).take(3) as List<String>)
                route.append('\nFull instructions:')
                    .append('\nCall the tool mauro_skill using arguments ')
                    .append(JsonOutput.toJson([id: skill.id, includeInstruction: true]))
                    .append('.')
                routes.add(route.toString())
            }
        }
        routes
    }

    private static List<String> buildToolRoutes(List<Map<String, Object>> tools) {
        if (!tools) {
            return []
        }
        Set<String> hiddenFromDefaultRouting = ['mauro_keyword_search', 'mauro_semantic_search'] as Set<String>
        List<String> routes = new ArrayList<String>()
        List<Map<String, Object>> sortedTools = new ArrayList<Map<String, Object>>(tools)
            .sort {Map<String, Object> left, Map<String, Object> right ->
                toolNameForRoute(left) <=> toolNameForRoute(right)
            } as List<Map<String, Object>>
        for (Map<String, Object> tool : sortedTools) {
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
            if (hiddenFromDefaultRouting.contains(name)) {
                continue
            }
            Map<String, Object> routing = getMap(tool.get('routing'))
            if (routing.isEmpty()) {
                routes.add(buildToolRoute(name, description, [:] as Map<String, Object>))
            } else {
                routes.add(buildToolRoute(name, description, routing))
            }
        }
        routes
    }

    private static String buildToolRoute(String name, String description, Map<String, Object> routing) {
        StringBuilder route = new StringBuilder(768)
        route.append('### ')
            .append(name)
            .append('\n')
        route.append('description: ')
            .append(description ?: '')
        appendStringSection(route, 'Purpose', routing.get('purpose'))
        appendMarkdownList(route, 'Use when', asStringList(routing.get('useWhen')))
        appendMarkdownListOrNone(route, 'Avoid when', asStringList(routing.get('avoidWhen')))
        appendMarkdownList(route, 'Search syntax', asStringList(routing.get('syntax')))
        appendMarkdownList(route, 'Filtering', asStringList(routing.get('filtering')))
        appendMarkdownList(route, 'Paging', asStringList(routing.get('paging')))
        appendMarkdownList(route, 'Limitations', asStringList(routing.get('limitations')))
        appendMarkdownList(route, 'Examples', asStringList(routing.get('examples')).take(3))
        route.toString()
    }

    private static String toolNameForRoute(Map<String, Object> tool) {
        if (tool == null) {
            return ''
        }
        Object functionObj = tool.get('function')
        if (!(functionObj instanceof Map)) {
            return ''
        }
        @SuppressWarnings('unchecked')
        Map<String, Object> function = (Map<String, Object>) functionObj
        asString(function.get('name')) ?: ''
    }

    private static void appendStringSection(StringBuilder builder, String label, Object value) {
        String text = asString(value)
        if (text != null && !text.trim().isEmpty()) {
            builder.append('\n\n')
                .append(label)
                .append(': ')
                .append(text)
        }
    }

    private static void appendMarkdownList(StringBuilder builder, String label, Object value) {
        appendMarkdownList(builder, label, asStringList(value))
    }

    private static void appendMarkdownList(StringBuilder builder, String label, List<String> values) {
        if (!values.isEmpty()) {
            builder.append('\n\n')
                .append(label)
                .append(':')
            for (String item : values) {
                builder.append('\n- ')
                    .append(item)
            }
        }
    }

    private static void appendMarkdownListOrNone(StringBuilder builder, String label, Object value) {
        appendMarkdownListOrNone(builder, label, asStringList(value))
    }

    private static void appendMarkdownListOrNone(StringBuilder builder, String label, List<String> values) {
        builder.append('\n\n')
            .append(label)
            .append(':')
        if (values.isEmpty()) {
            builder.append('\n- None specified.')
            return
        }
        for (String item : values) {
            builder.append('\n- ')
                .append(item)
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

    private static ChatEventDto generateInitialTitleEvent(
        List<MessageDto> timeline,
        SessionDto session,
        String assistantMessageId,
        LlmProvider provider,
        String userContent,
        String assistantContent
    ) {
        if (session == null || provider == null || session.title != null || Boolean.TRUE == session.metadata?.get('titleSetByUser')) {
            return null
        }
        String generatedTitle = generateSessionTitle(session, provider, userContent, assistantContent)
        if (generatedTitle == null || generatedTitle.trim().isEmpty()) {
            generatedTitle = fallbackSessionTitle(userContent)
        }
        if (generatedTitle == null || generatedTitle.trim().isEmpty()) {
            return null
        }
        session.title = generatedTitle
        session.metadata = session.metadata ?: [:]
        session.metadata.put('titleSetByUser', false)
        session.metadata.put('titleSource', 'llm')
        session.updatedAt = ChatInMemoryStore.now()
        ChatEventDto event = new ChatEventDto(
            type: 'session_title',
            messageId: assistantMessageId,
            role: 'system',
            content: generatedTitle,
            done: true,
            metadata: [
                sessionId: session.id,
                title    : generatedTitle,
                source   : 'llm'
            ] as Map<String, Object>
        )
        appendChatEvent(timeline, session.id, event)
        event
    }

    private static String generateSessionTitle(SessionDto session, LlmProvider provider, String userContent, String assistantContent) {
        try {
            ProviderRequest titleRequest = new ProviderRequest(
                sessionId: session.id,
                messageId: UUID.randomUUID().toString(),
                model: session.model,
                tools: [],
                options: [
                    purpose    : 'session_title',
                    think      : false,
                    num_predict: 32
                ] as Map<String, Object>,
                messages: [
                    new ProviderMessage(
                        role: 'system',
                        content: 'Create a concise title for this chat session. Return only the title. Use at most six words. Do not use quotation marks, markdown, or a trailing full stop.'
                    ),
                    new ProviderMessage(
                        role: 'user',
                        content: "User asked:\n${userContent ?: ''}\n\nAssistant answered:\n${assistantContent ?: ''}"
                    )
                ]
            )
            StringBuilder title = new StringBuilder(80)
            Flux.from(provider.streamChat(titleRequest))
                .filter {chunk -> chunk != null && chunk.type == 'token' && chunk.content != null}
                .map {chunk -> chunk.content}
                .collectList()
                .block()
                ?.each {String token -> title.append(token)}
            String sanitized = sanitizeSessionTitle(title.toString())
            looksLikeTitle(sanitized) ? sanitized : fallbackSessionTitle(userContent)
        } catch (Exception ignored) {
            fallbackSessionTitle(userContent)
        }
    }

    private static String sanitizeSessionTitle(String title) {
        if (title == null) {
            return null
        }
        String source = title
        int thinkClose = source.lastIndexOf('</think>')
        if (thinkClose >= 0) {
            source = source.substring(thinkClose + '</think>'.length())
        }
        String cleaned = source
            .replaceAll(/[\r\n]+/, ' ')
            .replaceAll(/^["'`*_#\s]+|["'`*_\s.]+$/, '')
            .replaceAll(/\s+/, ' ')
            .trim()
        if (cleaned.length() > 80) {
            cleaned = cleaned.substring(0, 80).trim()
        }
        cleaned
    }

    private static boolean looksLikeTitle(String title) {
        if (title == null || title.trim().isEmpty()) {
            return false
        }
        String cleaned = title.trim()
        String lower = cleaned.toLowerCase(Locale.ROOT)
        if (lower.contains('</think>') ||
            lower.startsWith('okay,') ||
            lower.startsWith('okay ') ||
            lower.startsWith('let me ') ||
            lower.startsWith('i need ') ||
            lower.startsWith('i should ') ||
            lower.startsWith('the user ') ||
            lower.contains('the user wants') ||
            lower.contains('looking at the conversation') ||
            lower.contains('concise title') ||
            lower.contains('chat session')) {
            return false
        }
        cleaned.split(/\s+/).length <= 8
    }

    private static String fallbackSessionTitle(String userContent) {
        if (userContent == null) {
            return null
        }
        String cleaned = userContent.replaceAll(/[\r\n]+/, ' ').replaceAll(/\s+/, ' ').trim()
        if (cleaned.length() > 40) {
            cleaned = cleaned.substring(0, 40).trim()
        }
        cleaned
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
        metadata.put('done', Boolean.TRUE == event.done)
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

    private static void addProjectedHistoryMessages(
        List<ProviderMessage> messages,
        List<Map<String, Object>> replayMetadata,
        List<ProviderMessage> historyMessages,
        int fromIndex,
        int toIndex
    ) {
        if (historyMessages == null || historyMessages.isEmpty()) {
            return
        }
        int start = Math.max(fromIndex, 0)
        int end = Math.min(toIndex, historyMessages.size())
        for (int i = start; i < end; i++) {
            messages.add(historyMessages.get(i))
            replayMetadata.add([
                source    : 'projected_history',
                replayMode: 'omit'
            ] as Map<String, Object>)
        }
    }

    private static int lastCurrentUserPromptIndex(List<ProviderMessage> historyMessages, String userContent) {
        if (historyMessages == null || historyMessages.isEmpty()) {
            return -1
        }
        String content = userContent ?: ''
        for (int i = historyMessages.size() - 1; i >= 0; i--) {
            ProviderMessage message = historyMessages.get(i)
            if (message?.role == 'user' && (message.content ?: '') == content) {
                return i
            }
        }
        -1
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
        Boolean.TRUE == last.metadata?.get('done') == (Boolean.TRUE == event.done)
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

    private static String eventTypeForChunk(ProviderChunk chunk) {
        if (chunk == null || chunk.type != 'provider_request_message') {
            return chunk?.type
        }
        'provider_request_message'
    }

    private static String roleForChunk(ProviderChunk chunk) {
        if (chunk != null && chunk.type == 'provider_request_message') {
            Map<String, Object> providerMessage = providerMessageFromMetadata(chunk.metadata)
            return String.valueOf(providerMessage.get('role') ?: 'assistant')
        }
        'assistant'
    }

    private static String contentForChunk(ProviderChunk chunk) {
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
        String toolName = asString(wrappedOutput.get('tool'))
        if (!(toolName in ['mauro_search', 'mauro_keyword_search'])) {
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
        boolean hasMore = Boolean.TRUE == output.get('hasMore')
        if (searchTerm == null || searchTerm.trim().isEmpty() || max == null || offset == null || nextOffset == null) {
            return ''
        }

        StringBuilder builder = new StringBuilder(512)
        builder.append('Previous ')
            .append(toolName)
            .append(' result memory: searchTerm "')
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
        builder.append(' If the user asks to count, filter, narrow, or ask how many of all previous results match an additional condition, do not count only the visible page. Call ')
            .append(toolName)
            .append(' again with the same domainTypes and a refined searchTerm that combines the prior search with the new condition, then answer from the returned total count. If the prior searchTerm uses OR, distribute the new condition across the alternatives, for example age education OR weight education.')

        if (hasMore) {
            Map<String, Object> nextPageToolCall = [
                name     : toolName,
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

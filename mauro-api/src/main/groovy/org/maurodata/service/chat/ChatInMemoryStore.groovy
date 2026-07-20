package org.maurodata.service.chat

import groovy.transform.CompileStatic
import jakarta.inject.Singleton
import org.maurodata.plugin.chat.api.chat.MessageDto
import org.maurodata.plugin.chat.api.chat.ProviderKeyStatusDto
import org.maurodata.plugin.chat.api.chat.SessionDto
import org.maurodata.service.chat.agent.AgentActionRecord
import org.maurodata.service.chat.agent.AgentContextRecord
import org.maurodata.service.chat.agent.AgentEvidenceRecord
import org.maurodata.service.chat.agent.AgentGuidanceRecord
import org.maurodata.service.chat.agent.AgentOperationRecord
import org.maurodata.service.chat.agent.AgentPlanRecord
import org.maurodata.service.chat.agent.AgentRunRecord
import org.maurodata.service.chat.agent.AgentStepRecord

import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

@Singleton
@CompileStatic
class ChatInMemoryStore {
    final Map<String, SessionDto> sessions = new ConcurrentHashMap<>()
    final Map<String, ProviderKeyStatusDto> providerKeyStatus = new ConcurrentHashMap<>()
    final Map<String, List<MessageDto>> sessionMessages = new ConcurrentHashMap<>()
    final Map<String, AgentRunRecord> agentRuns = new ConcurrentHashMap<>()
    final Map<String, AgentContextRecord> agentContexts = new ConcurrentHashMap<>()
    final Map<String, AgentPlanRecord> agentPlans = new ConcurrentHashMap<>()
    final Map<String, AgentStepRecord> agentSteps = new ConcurrentHashMap<>()
    final Map<String, AgentActionRecord> agentActions = new ConcurrentHashMap<>()
    final Map<String, AgentOperationRecord> agentOperations = new ConcurrentHashMap<>()
    final Map<String, AgentEvidenceRecord> agentEvidence = new ConcurrentHashMap<>()
    final Map<String, AgentGuidanceRecord> agentGuidance = new ConcurrentHashMap<>()

    static Instant now() {
        Instant.now()
    }

    List<MessageDto> messagesForSession(String sessionId) {
        sessionMessages.computeIfAbsent(sessionId) { new CopyOnWriteArrayList<MessageDto>() }
    }
}

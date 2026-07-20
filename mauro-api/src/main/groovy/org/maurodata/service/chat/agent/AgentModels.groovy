package org.maurodata.service.chat.agent

import groovy.transform.CompileStatic

import java.time.Instant

@CompileStatic
class AgentRunRecord {
    String id
    String sessionId
    String messageId
    String goal
    String status = 'queued'
    String model
    String currentPlanId
    Instant createdAt
    Instant updatedAt
    Map<String, Object> metadata = [:]
}

@CompileStatic
class AgentPlanRecord {
    String id
    String runId
    Integer version = 1
    String status = 'active'
    String fitness = 'draft'
    String goalRestatement
    List<String> successCriteria = []
    List<String> assumptions = []
    List<String> risks = []
    List<AgentStepRecord> steps = []
    Instant createdAt
    Map<String, Object> metadata = [:]
}

@CompileStatic
class AgentContextRecord {
    String id
    String runId
    Integer version = 1
    String status = 'resolved'
    String goalRestatement
    String followUpInterpretation
    Map<String, Object> goalFrame = [:]
    List<String> domainContext = []
    List<Map<String, Object>> relevantTools = []
    List<Map<String, Object>> recommendedSkills = []
    List<Map<String, Object>> relevantResources = []
    List<Map<String, Object>> instructions = []
    List<Map<String, Object>> resolvedReferences = []
    List<Map<String, Object>> resolvedResources = []
    List<Map<String, Object>> priorEvidenceToReuse = []
    List<Map<String, Object>> priorGuidanceToFollow = []
    List<Map<String, Object>> contextRequests = []
    List<String> planningHints = []
    List<String> constraints = []
    Instant createdAt
    Map<String, Object> metadata = [:]
}

@CompileStatic
class AgentSessionContinuityContext {
    String sessionId
    String currentRunId
    List<Map<String, Object>> priorRuns = []
    List<Map<String, Object>> reusableEvidence = []
    List<Map<String, Object>> linkedGuidance = []
    List<Map<String, Object>> pagingState = []
    List<String> openLimitations = []
    Instant createdAt
    Map<String, Object> metadata = [:]

    boolean hasContent() {
        priorRuns || reusableEvidence || linkedGuidance || pagingState || openLimitations
    }
}

@CompileStatic
class AgentStepRecord {
    String id
    String runId
    String planId
    Integer ordinal
    String title
    String objective
    String kind = 'tool'
    String status = 'pending'
    List<String> allowedTools = []
    String guard = 'always'
    String guardReason
    Boolean optional = false
    String expectedOutput
    List<String> successCriteria = []
    Integer attemptCount = 0
    String resultSummary
    List<String> evidenceRefs = []
    Instant startedAt
    Instant completedAt
    Map<String, Object> metadata = [:]
}

@CompileStatic
class AgentActionRecord {
    String id
    String runId
    String stepId
    String kind = 'model'
    String status = 'in_progress'
    String toolName
    String callId
    Map<String, Object> arguments = [:]
    String resultRef
    String error
    Boolean blocked = false
    Instant startedAt
    Instant completedAt
    Map<String, Object> metadata = [:]
}

@CompileStatic
class AgentOperationRecord {
    String id
    String runId
    String planId
    String stepId
    String parentOperationId
    String role
    String status = 'pending'
    Integer attempt = 1
    Integer maxAttempts = 1
    String inputSummary
    String outputSummary
    String error
    Instant startedAt
    Instant completedAt
    Map<String, Object> metadata = [:]
}

@CompileStatic
class AgentEvidenceRecord {
    String id
    String runId
    String stepId
    String sourceType
    String sourceName
    String sourceId
    String title
    String summary
    String content
    Map<String, Object> structuredContent = [:]
    Instant createdAt
    Map<String, Object> metadata = [:]
}

@CompileStatic
class AgentGuidanceRecord {
    String id
    String runId
    String stepId
    String sourceType
    String sourceName
    String sourceId
    String content
    Boolean followForFinal = false
    Instant createdAt
    Map<String, Object> metadata = [:]
}

package org.maurodata.service.chat.agent

import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.micronaut.context.annotation.Value
import io.micronaut.http.HttpHeaders
import io.micronaut.http.HttpRequest
import jakarta.inject.Singleton
import org.maurodata.plugin.chat.api.chat.ChatEventDto
import org.maurodata.plugin.chat.api.chat.McpServerDto
import org.maurodata.plugin.chat.api.chat.MessageDto
import org.maurodata.plugin.chat.api.chat.SendMessageRequest
import org.maurodata.plugin.chat.api.chat.SessionDto
import org.maurodata.plugin.chat.api.chat.ToolInvokeRequest
import org.maurodata.plugin.chat.api.chat.ToolInvokeResponse
import org.maurodata.plugin.chat.api.chat.ToolSummaryDto
import org.maurodata.service.chat.ChatInMemoryStore
import org.maurodata.service.chat.ChatMcpService
import org.maurodata.service.chat.ChatPromptAssetDefinition
import org.maurodata.service.chat.ChatPromptComposer
import org.maurodata.service.chat.ChatPromptAssetService
import org.maurodata.service.chat.ChatPromptRenderResult
import org.maurodata.service.chat.SkillToolApplicability
import org.maurodata.service.chat.llm.LlmProvider
import org.maurodata.service.chat.llm.ProviderChunk
import org.maurodata.service.chat.llm.ProviderMessage
import org.maurodata.service.chat.llm.ProviderRegistry
import org.maurodata.service.chat.llm.ProviderRequest
import org.reactivestreams.Publisher
import reactor.core.publisher.Flux
import reactor.core.publisher.FluxSink

import java.time.Instant
import java.util.Locale
import java.util.regex.Pattern

@Slf4j
@CompileStatic
@Singleton
class AgentSupervisorService {

    private static final int CONTEXT_SIFT_PAGE_SIZE = 2
    private static final int MAX_CONTEXT_SIFT_PAGES = 3

    private final ChatInMemoryStore store
    private final ProviderRegistry providerRegistry
    private final ChatMcpService chatMcpService
    private final ChatPromptAssetService promptAssetService
    private final AgentPromptService agentPromptService
    private final AgentStateTransitionService transitionService = new AgentStateTransitionService()
    private final ThreadLocal<OperationTraceContext> operationTrace = new ThreadLocal<OperationTraceContext>()
    private final JsonSlurper slurper = new JsonSlurper()
    private final int maxSteps
    private final int maxToolCalls
    private final int maxReplans
    private final int maxStepRetries
    private final double contextResolverTemperature

    AgentSupervisorService(
        ChatInMemoryStore store,
        ProviderRegistry providerRegistry,
        ChatMcpService chatMcpService,
        ChatPromptAssetService promptAssetService,
        @Value('${chat.agent.max-steps:8}') Integer maxSteps,
        @Value('${chat.agent.max-tool-calls:16}') Integer maxToolCalls,
        @Value('${chat.agent.max-replans:3}') Integer maxReplans,
        @Value('${chat.agent.max-step-retries:2}') Integer maxStepRetries,
        @Value('${chat.agent.context-resolver.temperature:0.3}') Double contextResolverTemperature
    ) {
        this.store = store
        this.providerRegistry = providerRegistry
        this.chatMcpService = chatMcpService
        this.promptAssetService = promptAssetService
        this.agentPromptService = new AgentPromptService(new ChatPromptComposer(promptAssetService))
        this.maxSteps = Math.max(maxSteps ?: 8, 1)
        this.maxToolCalls = Math.max(maxToolCalls ?: 16, 1)
        this.maxReplans = Math.max(maxReplans == null ? 3 : maxReplans, 0)
        this.maxStepRetries = Math.max(maxStepRetries == null ? 2 : maxStepRetries, 0)
        this.contextResolverTemperature = contextResolverTemperature == null ? 0.3d : contextResolverTemperature.doubleValue()
    }

    AgentSupervisorService(
        ChatInMemoryStore store,
        ProviderRegistry providerRegistry,
        ChatMcpService chatMcpService,
        ChatPromptAssetService promptAssetService,
        Integer maxSteps,
        Integer maxToolCalls,
        Integer maxReplans
    ) {
        this(store, providerRegistry, chatMcpService, promptAssetService, maxSteps, maxToolCalls, maxReplans, 2, 0.3d)
    }

    AgentSupervisorService(
        ChatInMemoryStore store,
        ProviderRegistry providerRegistry,
        ChatMcpService chatMcpService,
        Integer maxSteps,
        Integer maxToolCalls
    ) {
        this(store, providerRegistry, chatMcpService, null, maxSteps, maxToolCalls, 3, 2, 0.3d)
    }

    AgentSupervisorService(
        ChatInMemoryStore store,
        ProviderRegistry providerRegistry,
        ChatMcpService chatMcpService,
        Integer maxSteps,
        Integer maxToolCalls,
        Integer maxReplans
    ) {
        this(store, providerRegistry, chatMcpService, null, maxSteps, maxToolCalls, maxReplans, 2, 0.3d)
    }

    Publisher<ChatEventDto> streamAgentRun(
        SessionDto session,
        SendMessageRequest request,
        String assistantMessageId,
        List<MessageDto> timeline,
        HttpRequest<?> httpRequest
    ) {
        Flux.create {sink ->
            Thread worker = new Thread({
                AgentRunRecord run = null
                try {
                    LlmProvider provider = providerRegistry.byModel(session.model)
                    List<Map<String, Object>> tools = providerTools()
                    List<String> toolNames = toolNames(tools)
                    run = createRun(session, request, assistantMessageId, provider.id())
                    emit(timeline, sink as FluxSink<ChatEventDto>, session.id, agentEvent('agent_run_created', assistantMessageId, 'system', '', false, [
                        runId : run.id,
                        status: run.status,
                        goal  : run.goal
                    ] as Map<String, Object>))
                    transitionRun(run, AgentStateTransitionService.RUN_IN_PROGRESS, 'run_started', null, timeline, sink as FluxSink<ChatEventDto>, session.id, assistantMessageId)
                    emit(timeline, sink as FluxSink<ChatEventDto>, session.id, agentEvent('agent_run_started', assistantMessageId, 'system', '', false, [
                        runId: run.id,
                        status: run.status
                    ] as Map<String, Object>))
                    emitProgress(timeline, sink as FluxSink<ChatEventDto>, session.id, assistantMessageId, 'run_started', 'Starting the agent run.', [
                        runId: run.id
                    ] as Map<String, Object>)

                    List<AgentEvidenceRecord> evidence = new ArrayList<AgentEvidenceRecord>()
                    List<AgentGuidanceRecord> guidance = new ArrayList<AgentGuidanceRecord>()
                    AgentSessionContinuityContext sessionContinuity = buildSessionContinuity(run, timeline)
                    emitSessionContinuityDebug(timeline, sink as FluxSink<ChatEventDto>, session.id, assistantMessageId, run, sessionContinuity)
                    AgentContextRecord context = resolveContextOperation(run, null, null, provider, tools, evidence, guidance, null, sessionContinuity, 1, timeline, sink as FluxSink<ChatEventDto>, session.id, assistantMessageId)
                    seedPriorContextSelections(context, evidence, guidance)
                    store.agentContexts[context.id] = context
                    emitContextDebug(timeline, sink as FluxSink<ChatEventDto>, session.id, assistantMessageId, context)
                    emitContextResolved(timeline, sink as FluxSink<ChatEventDto>, session.id, assistantMessageId, context, null)
                    emitProgress(timeline, sink as FluxSink<ChatEventDto>, session.id, assistantMessageId, 'context_resolved', 'Resolved planning context.', [
                        runId: run.id,
                        contextId: context.id
                    ] as Map<String, Object>)

                    AgentPlanRecord plan = createPlanOperation(run, null, null, provider, tools, toolNames, evidence, null, context, timeline, sink as FluxSink<ChatEventDto>, session.id, assistantMessageId)
                    store.agentPlans[plan.id] = plan
                    run.currentPlanId = plan.id
                    emit(timeline, sink as FluxSink<ChatEventDto>, session.id, agentEvent('agent_plan_created', assistantMessageId, 'system', plan.goalRestatement ?: '', false, [
                        runId          : run.id,
                        planId         : plan.id,
                        fitness        : plan.fitness,
                        successCriteria: plan.successCriteria,
                        successCriteriaMarkdown: criteriaMarkdown(plan.successCriteria),
                        steps          : plan.steps.collect {AgentStepRecord step -> stepMetadata(step)}
                    ] as Map<String, Object>))
                    emitProgress(timeline, sink as FluxSink<ChatEventDto>, session.id, assistantMessageId, 'plan_created', "Created a plan with ${plan.steps.size()} step${plan.steps.size() == 1 ? '' : 's'}.", [
                        runId: run.id,
                        planId: plan.id
                    ] as Map<String, Object>)

                    int toolCalls = 0
                    boolean finalWritten = false
                    boolean stoppedIncomplete = false
                    String incompleteReason = null
                    int replans = 0
                    boolean executePlan = true
                    while (executePlan && run.status == 'in_progress' && !finalWritten) {
                        executePlan = false
                        List<AgentStepRecord> executableSteps = plan.steps.findAll {AgentStepRecord step ->
                            step.kind != 'final_answer' && isPendingStep(step)
                        }
                        for (AgentStepRecord step : executableSteps.take(maxSteps)) {
                            StepGuardDecision guardDecision = evaluateStepGuard(plan, step, evidence)
                            if (!guardDecision.execute) {
                                skipStep(run, plan, step, 'step_guard_false', guardDecision.reason, timeline, sink as FluxSink<ChatEventDto>, session.id, assistantMessageId, [
                                    guard: step.guard,
                                    guardReason: step.guardReason,
                                    optional: step.optional
                                ] as Map<String, Object>)
                                continue
                            }
                            step.attemptCount = (step.attemptCount ?: 0) + 1
                            step.startedAt = ChatInMemoryStore.now()
                            store.agentSteps[step.id] = step
                            transitionStep(step, AgentStateTransitionService.STEP_IN_PROGRESS, 'step_started', null, timeline, sink as FluxSink<ChatEventDto>, session.id, assistantMessageId)
                            emit(timeline, sink as FluxSink<ChatEventDto>, session.id, agentEvent('agent_step_started', assistantMessageId, 'system', step.title ?: '', false, [
                                runId       : run.id,
                                planId      : plan.id,
                                stepId      : step.id,
                                status      : step.status,
                                objective   : step.objective,
                                kind        : step.kind,
                                allowedTools: step.allowedTools
                            ] as Map<String, Object>))
                            emitProgress(timeline, sink as FluxSink<ChatEventDto>, session.id, assistantMessageId, 'step_started', progressStepStarted(step), [
                                runId: run.id,
                                planId: plan.id,
                                stepId: step.id
                            ] as Map<String, Object>)

                            StepExecution execution = executeStep(run, plan, context, step, evidence, guidance, provider, tools, timeline, sink as FluxSink<ChatEventDto>, session.id, assistantMessageId, httpRequest)
                            evidence.addAll(execution.evidence)
                            guidance.addAll(execution.guidance)
                            toolCalls += execution.toolCalls
                            if (toolCalls > maxToolCalls) {
                                throw new IllegalStateException("Agent stopped after reaching max tool calls (${maxToolCalls})")
                            }
                            StepAssessment stepAssessment = evaluateStepOperation(run, plan, context, step, evidence, execution.evidence, guidance, provider, timeline, sink as FluxSink<ChatEventDto>, session.id, assistantMessageId)
                            step.resultSummary = stepAssessment.summary ?: execution.summary
                            step.completedAt = ChatInMemoryStore.now()
                            step.evidenceRefs = evidence.collect {AgentEvidenceRecord item -> item.id}
                            transitionStep(step, stepAssessment.completed ? AgentStateTransitionService.STEP_COMPLETED : AgentStateTransitionService.STEP_FAILED, stepAssessment.completed ? 'step_completed' : 'step_failed', stepAssessment.reason, timeline, sink as FluxSink<ChatEventDto>, session.id, assistantMessageId, [
                                decision: stepAssessment.decision
                            ] as Map<String, Object>)
                            emit(timeline, sink as FluxSink<ChatEventDto>, session.id, agentEvent(
                                stepAssessment.completed ? 'agent_step_completed' : 'agent_step_failed',
                                assistantMessageId,
                                'system',
                                step.resultSummary ?: '',
                                false,
                                [
                                    runId   : run.id,
                                    planId  : plan.id,
                                    stepId  : step.id,
                                    status  : step.status,
                                    decision: stepAssessment.decision,
                                    reason  : stepAssessment.reason
                                ] as Map<String, Object>
                            ))
                            emitProgress(timeline, sink as FluxSink<ChatEventDto>, session.id, assistantMessageId, stepAssessment.completed ? 'step_completed' : 'step_failed', progressStepAssessed(stepAssessment), [
                                runId: run.id,
                                planId: plan.id,
                                stepId: step.id,
                                decision: stepAssessment.decision
                            ] as Map<String, Object>)

                            if (stepAssessment.completed) {
                                PlanAssessment planAssessment = evaluatePlanOperation(run, plan, context, step, evidence, guidance, provider, timeline, sink as FluxSink<ChatEventDto>, session.id, assistantMessageId)
                                planAssessment = normalizePlanAssessmentWithTransitionService(planAssessment, plan, evidence)
                                applyObsoleteStepSkips(run, plan, planAssessment, timeline, sink as FluxSink<ChatEventDto>, session.id, assistantMessageId)
                                emit(timeline, sink as FluxSink<ChatEventDto>, session.id, agentEvent('agent_plan_evaluated', assistantMessageId, 'system', planAssessment.summary ?: '', false, [
                                    runId   : run.id,
                                    planId  : plan.id,
                                    stepId  : step.id,
                                    decision: planAssessment.decision,
                                    reason  : planAssessment.reason
                                ] as Map<String, Object>))
                                emitProgress(timeline, sink as FluxSink<ChatEventDto>, session.id, assistantMessageId, "plan_${planAssessment.decision ?: 'evaluated'}".toString(), progressPlanAssessed(planAssessment), [
                                    runId: run.id,
                                    planId: plan.id,
                                    stepId: step.id,
                                    decision: planAssessment.decision
                                ] as Map<String, Object>)

                                if (planAssessment.decision == 'final') {
                                    transitionPlan(plan, AgentStateTransitionService.PLAN_COMPLETE, 'plan_final', planAssessment.reason, timeline, sink as FluxSink<ChatEventDto>, session.id, assistantMessageId, [
                                        stepId: step.id,
                                        decision: planAssessment.decision
                                    ] as Map<String, Object>)
                                    writeFinalOperation(run, plan, context, evidence, guidance, provider, timeline, sink as FluxSink<ChatEventDto>, session.id, assistantMessageId)
                                    finalWritten = true
                                    break
                                }
                                if (planAssessment.decision == 'continue') {
                                    continue
                                }
                                if (planAssessment.decision == 'replan') {
                                    String replanReason = planAssessment.reason ?: planAssessment.summary ?: 'The current plan needs another concrete step.'
                                    if (replans < maxReplans) {
                                        replans++
                                        emit(timeline, sink as FluxSink<ChatEventDto>, session.id, agentEvent('agent_replan_started', assistantMessageId, 'system', replanReason, false, [
                                            runId : run.id,
                                            planId: plan.id,
                                            stepId: step.id,
                                            reason: replanReason,
                                            replan: replans
                                        ] as Map<String, Object>))
                                        emitProgress(timeline, sink as FluxSink<ChatEventDto>, session.id, assistantMessageId, 'replan_started', "Changing approach: ${shortText(replanReason)}", [
                                            runId: run.id,
                                            planId: plan.id,
                                            stepId: step.id,
                                            replan: replans
                                        ] as Map<String, Object>)
                                        transitionPlan(plan, AgentStateTransitionService.PLAN_SUPERSEDED, 'replan_started', replanReason, timeline, sink as FluxSink<ChatEventDto>, session.id, assistantMessageId, [
                                            replan: replans
                                        ] as Map<String, Object>)
                                        AgentContextRecord revisedContext = resolveContextOperation(run, plan, step, provider, tools, evidence, guidance, replanReason, sessionContinuity, replans + 1, timeline, sink as FluxSink<ChatEventDto>, session.id, assistantMessageId)
                                        seedPriorContextSelections(revisedContext, evidence, guidance)
                                        store.agentContexts[revisedContext.id] = revisedContext
                                        emitContextDebug(timeline, sink as FluxSink<ChatEventDto>, session.id, assistantMessageId, revisedContext)
                                        emitContextResolved(timeline, sink as FluxSink<ChatEventDto>, session.id, assistantMessageId, revisedContext, replans)
                                        emitProgress(timeline, sink as FluxSink<ChatEventDto>, session.id, assistantMessageId, 'context_resolved', 'Resolved updated planning context.', [
                                            runId: run.id,
                                            planId: plan.id,
                                            contextId: revisedContext.id,
                                            replan: replans
                                        ] as Map<String, Object>)
                                        AgentPlanRecord revisedPlan = createPlanOperation(run, plan, step, provider, tools, toolNames, evidence, replanReason, revisedContext, timeline, sink as FluxSink<ChatEventDto>, session.id, assistantMessageId)
                                        revisedPlan.version = (plan.version ?: 1) + 1
                                        store.agentPlans[revisedPlan.id] = revisedPlan
                                        run.currentPlanId = revisedPlan.id
                                        plan = revisedPlan
                                        context = revisedContext
                                        emit(timeline, sink as FluxSink<ChatEventDto>, session.id, agentEvent('agent_replan_completed', assistantMessageId, 'system', plan.goalRestatement ?: '', false, [
                                            runId          : run.id,
                                            planId         : plan.id,
                                            fitness        : plan.fitness,
                                            successCriteria: plan.successCriteria,
                                            successCriteriaMarkdown: criteriaMarkdown(plan.successCriteria),
                                            steps          : plan.steps.collect {AgentStepRecord revisedStep -> stepMetadata(revisedStep)},
                                            replan         : replans
                                        ] as Map<String, Object>))
                                        emitProgress(timeline, sink as FluxSink<ChatEventDto>, session.id, assistantMessageId, 'replan_completed', "Updated the plan with ${plan.steps.size()} remaining step${plan.steps.size() == 1 ? '' : 's'}.", [
                                            runId: run.id,
                                            planId: plan.id,
                                            replan: replans
                                        ] as Map<String, Object>)
                                        executePlan = true
                                    } else {
                                        stoppedIncomplete = true
                                        incompleteReason = replanReason
                                    }
                                    break
                                }
                                if (planAssessment.decision == 'ask_user') {
                                    transitionRun(run, AgentStateTransitionService.RUN_REQUIRES_ACTION, 'plan_requires_action', planAssessment.reason ?: planAssessment.question, timeline, sink as FluxSink<ChatEventDto>, session.id, assistantMessageId, [
                                        planId: plan.id,
                                        stepId: step.id,
                                        waiting: 'user_input'
                                    ] as Map<String, Object>)
                                    emit(timeline, sink as FluxSink<ChatEventDto>, session.id, agentEvent('agent_requires_action', assistantMessageId, 'assistant', planAssessment.question ?: planAssessment.reason ?: 'More information is required.', false, [
                                        runId  : run.id,
                                        planId : plan.id,
                                        stepId : step.id,
                                        status : run.status,
                                        waiting: 'user_input'
                                    ] as Map<String, Object>))
                                    emitProgress(timeline, sink as FluxSink<ChatEventDto>, session.id, assistantMessageId, 'requires_action', shortText(planAssessment.question ?: planAssessment.reason ?: 'More information is required.'), [
                                        runId: run.id,
                                        planId: plan.id,
                                        stepId: step.id
                                    ] as Map<String, Object>)
                                    break
                                }

                                stoppedIncomplete = true
                                incompleteReason = planAssessment.reason ?: planAssessment.summary ?: 'The agent could not complete the current plan with the evidence available.'
                                break
                            }

                            if (stepAssessment.decision == 'retry' && nonRetryableToolFailure(execution.evidence)) {
                                String retryReason = stepAssessment.reason ?: stepAssessment.summary ?: 'The current step failed with a non-retryable tool error.'
                                emit(timeline, sink as FluxSink<ChatEventDto>, session.id, agentEvent('agent_step_retry_suppressed', assistantMessageId, 'system', retryReason, false, [
                                    runId: run.id,
                                    planId: plan.id,
                                    stepId: step.id,
                                    reason: retryReason,
                                    retrySuppressed: true
                                ] as Map<String, Object>))
                                emitProgress(timeline, sink as FluxSink<ChatEventDto>, session.id, assistantMessageId, 'step_retry_suppressed', "Retry skipped: ${shortText(retryReason)}", [
                                    runId: run.id,
                                    planId: plan.id,
                                    stepId: step.id
                                ] as Map<String, Object>)
                                if (hasPendingOptionalRecoveryStep(plan, step)) {
                                    continue
                                }
                            } else if (stepAssessment.decision == 'retry' && (step.attemptCount ?: 0) <= maxStepRetries) {
                                step.completedAt = null
                                transitionStep(step, AgentStateTransitionService.STEP_PENDING, 'step_retrying', stepAssessment.reason, timeline, sink as FluxSink<ChatEventDto>, session.id, assistantMessageId, [
                                    decision: stepAssessment.decision,
                                    attempt: step.attemptCount,
                                    maxStepRetries: maxStepRetries
                                ] as Map<String, Object>)
                                String retryReason = stepAssessment.reason ?: stepAssessment.summary ?: 'Retrying the current step.'
                                emit(timeline, sink as FluxSink<ChatEventDto>, session.id, agentEvent('agent_step_retrying', assistantMessageId, 'system', retryReason, false, [
                                    runId: run.id,
                                    planId: plan.id,
                                    stepId: step.id,
                                    attempt: step.attemptCount,
                                    maxStepRetries: maxStepRetries,
                                    reason: retryReason
                                ] as Map<String, Object>))
                                emitProgress(timeline, sink as FluxSink<ChatEventDto>, session.id, assistantMessageId, 'step_retrying', "Retrying: ${shortText(retryReason)}", [
                                    runId: run.id,
                                    planId: plan.id,
                                    stepId: step.id,
                                    attempt: step.attemptCount,
                                    maxStepRetries: maxStepRetries
                                ] as Map<String, Object>)
                                executePlan = true
                                break
                            }

                            if (stepAssessment.decision in ['continue', 'retry', 'replan']) {
                                String replanReason = stepAssessment.reason ?: stepAssessment.summary ?: 'The current plan needs another concrete step.'
                                if (replans < maxReplans) {
                                    replans++
                                    emit(timeline, sink as FluxSink<ChatEventDto>, session.id, agentEvent('agent_replan_started', assistantMessageId, 'system', replanReason, false, [
                                        runId : run.id,
                                        planId: plan.id,
                                        stepId: step.id,
                                        reason: replanReason,
                                        replan: replans
                                    ] as Map<String, Object>))
                                    emitProgress(timeline, sink as FluxSink<ChatEventDto>, session.id, assistantMessageId, 'replan_started', "Changing approach: ${shortText(replanReason)}", [
                                        runId: run.id,
                                        planId: plan.id,
                                        stepId: step.id,
                                        replan: replans
                                    ] as Map<String, Object>)
                                    transitionPlan(plan, AgentStateTransitionService.PLAN_SUPERSEDED, 'replan_started', replanReason, timeline, sink as FluxSink<ChatEventDto>, session.id, assistantMessageId, [
                                        replan: replans
                                    ] as Map<String, Object>)
                                    AgentContextRecord revisedContext = resolveContextOperation(run, plan, step, provider, tools, evidence, guidance, replanReason, sessionContinuity, replans + 1, timeline, sink as FluxSink<ChatEventDto>, session.id, assistantMessageId)
                                    seedPriorContextSelections(revisedContext, evidence, guidance)
                                    store.agentContexts[revisedContext.id] = revisedContext
                                    emitContextDebug(timeline, sink as FluxSink<ChatEventDto>, session.id, assistantMessageId, revisedContext)
                                    emitContextResolved(timeline, sink as FluxSink<ChatEventDto>, session.id, assistantMessageId, revisedContext, replans)
                                    emitProgress(timeline, sink as FluxSink<ChatEventDto>, session.id, assistantMessageId, 'context_resolved', 'Resolved updated planning context.', [
                                        runId: run.id,
                                        planId: plan.id,
                                        contextId: revisedContext.id,
                                        replan: replans
                                    ] as Map<String, Object>)
                                    AgentPlanRecord revisedPlan = createPlanOperation(run, plan, step, provider, tools, toolNames, evidence, replanReason, revisedContext, timeline, sink as FluxSink<ChatEventDto>, session.id, assistantMessageId)
                                    revisedPlan.version = (plan.version ?: 1) + 1
                                    store.agentPlans[revisedPlan.id] = revisedPlan
                                    run.currentPlanId = revisedPlan.id
                                    plan = revisedPlan
                                    context = revisedContext
                                    emit(timeline, sink as FluxSink<ChatEventDto>, session.id, agentEvent('agent_replan_completed', assistantMessageId, 'system', plan.goalRestatement ?: '', false, [
                                        runId          : run.id,
                                        planId         : plan.id,
                                        fitness        : plan.fitness,
                                        successCriteria: plan.successCriteria,
                                        successCriteriaMarkdown: criteriaMarkdown(plan.successCriteria),
                                        steps          : plan.steps.collect {AgentStepRecord revisedStep -> stepMetadata(revisedStep)},
                                        replan         : replans
                                    ] as Map<String, Object>))
                                    emitProgress(timeline, sink as FluxSink<ChatEventDto>, session.id, assistantMessageId, 'replan_completed', "Updated the plan with ${plan.steps.size()} remaining step${plan.steps.size() == 1 ? '' : 's'}.", [
                                        runId: run.id,
                                        planId: plan.id,
                                        replan: replans
                                    ] as Map<String, Object>)
                                    executePlan = true
                                } else {
                                    stoppedIncomplete = true
                                    incompleteReason = replanReason
                                }
                                break
                            }
                            if (stepAssessment.decision == 'fail') {
                                stoppedIncomplete = true
                                incompleteReason = stepAssessment.reason ?: stepAssessment.summary ?: 'The agent could not complete the current step.'
                                break
                            }
                            if (stepAssessment.decision == 'ask_user') {
                                transitionRun(run, AgentStateTransitionService.RUN_REQUIRES_ACTION, 'step_requires_action', stepAssessment.reason ?: stepAssessment.question, timeline, sink as FluxSink<ChatEventDto>, session.id, assistantMessageId, [
                                    planId: plan.id,
                                    stepId: step.id,
                                    waiting: 'user_input'
                                ] as Map<String, Object>)
                                emit(timeline, sink as FluxSink<ChatEventDto>, session.id, agentEvent('agent_requires_action', assistantMessageId, 'assistant', stepAssessment.question ?: stepAssessment.reason ?: 'More information is required.', false, [
                                    runId  : run.id,
                                    planId : plan.id,
                                    stepId : step.id,
                                    status : run.status,
                                    waiting: 'user_input'
                                ] as Map<String, Object>))
                                emitProgress(timeline, sink as FluxSink<ChatEventDto>, session.id, assistantMessageId, 'requires_action', shortText(stepAssessment.question ?: stepAssessment.reason ?: 'More information is required.'), [
                                    runId: run.id,
                                    planId: plan.id,
                                    stepId: step.id
                                ] as Map<String, Object>)
                                break
                            }
                            stoppedIncomplete = true
                            incompleteReason = stepAssessment.reason ?: stepAssessment.summary ?: 'The agent could not complete the current step.'
                            break
                        }

                        boolean allExecutableStepsTerminal = plan.steps.findAll {AgentStepRecord step ->
                            step.kind != 'final_answer'
                        }.every {AgentStepRecord step -> isTerminalStep(step)}
                        if (run.status != 'requires_action' && !finalWritten && !stoppedIncomplete && !executePlan && allExecutableStepsTerminal) {
                            transitionPlan(plan, AgentStateTransitionService.PLAN_COMPLETE, 'plan_all_steps_completed', null, timeline, sink as FluxSink<ChatEventDto>, session.id, assistantMessageId)
                            writeFinalOperation(run, plan, context, evidence, guidance, provider, timeline, sink as FluxSink<ChatEventDto>, session.id, assistantMessageId)
                            finalWritten = true
                        }
                    }
                    if (stoppedIncomplete) {
                        transitionRun(run, AgentStateTransitionService.RUN_FAILED, 'run_failed', incompleteReason, timeline, sink as FluxSink<ChatEventDto>, session.id, assistantMessageId, [
                            planId: plan.id
                        ] as Map<String, Object>)
                        emit(timeline, sink as FluxSink<ChatEventDto>, session.id, agentEvent('agent_run_failed', assistantMessageId, 'assistant', incompleteReason, false, [
                            runId : run.id,
                            planId: plan.id,
                            status: run.status,
                            reason: incompleteReason
                        ] as Map<String, Object>))
                        emitProgress(timeline, sink as FluxSink<ChatEventDto>, session.id, assistantMessageId, 'run_failed', "Stopped: ${shortText(incompleteReason)}", [
                            runId: run.id,
                            planId: plan.id
                        ] as Map<String, Object>)
                    }
                    if (run.status != 'requires_action' && run.status != 'failed') {
                        transitionRun(run, AgentStateTransitionService.RUN_COMPLETED, 'run_completed', null, timeline, sink as FluxSink<ChatEventDto>, session.id, assistantMessageId, [
                            planId: plan.id
                        ] as Map<String, Object>)
                        emit(timeline, sink as FluxSink<ChatEventDto>, session.id, agentEvent('agent_run_completed', assistantMessageId, 'system', '', false, [
                            runId: run.id,
                            planId: plan.id,
                            status: run.status
                        ] as Map<String, Object>))
                        emitProgress(timeline, sink as FluxSink<ChatEventDto>, session.id, assistantMessageId, 'run_completed', 'Agent run completed.', [
                            runId: run.id,
                            planId: plan.id
                        ] as Map<String, Object>)
                    }
                    sink.complete()
                } catch (Throwable t) {
                    log.error('Agent run failed sessionId={} messageId={}', session?.id, assistantMessageId, t)
                    if (run != null) {
                        transitionRun(run, AgentStateTransitionService.RUN_FAILED, 'run_error', t.message ?: t.class.simpleName, timeline, sink as FluxSink<ChatEventDto>, session.id, assistantMessageId)
                    }
                    Map<String, Object> errorMetadata = [
                        runId: run?.id,
                        status: run?.status,
                        error: t.message ?: 'Agent run failed'
                    ] as Map<String, Object>
                    if (t instanceof StrictJsonParseException) {
                        StrictJsonParseException jsonError = (StrictJsonParseException) t
                        errorMetadata.putAll([
                            roleName: jsonError.roleName,
                            parseError: jsonError.parseError,
                            rawOutputSnippet: jsonError.rawOutputSnippet,
                            repairParseError: jsonError.repairParseError,
                            repairOutputSnippet: jsonError.repairOutputSnippet
                        ] as Map<String, Object>)
                    }
                    emit(timeline, sink as FluxSink<ChatEventDto>, session.id, agentEvent('agent_run_failed', assistantMessageId, 'assistant', t.message ?: 'Agent run failed', false, errorMetadata))
                    emitProgress(timeline, sink as FluxSink<ChatEventDto>, session.id, assistantMessageId, 'run_failed', "Stopped: ${shortText(t.message ?: 'Agent run failed')}", [
                        runId: run?.id
                    ] as Map<String, Object>)
                    sink.next(new ChatEventDto(
                        type: 'error',
                        messageId: assistantMessageId,
                        role: 'assistant',
                        content: "Agent error: ${t.message ?: 'Unknown error'}".toString(),
                        done: false,
                        metadata: [runId: run?.id] as Map<String, Object>
                    ))
                    sink.complete()
                }
            } as Runnable, "agent-supervisor-${assistantMessageId}".toString())
            worker.daemon = true
            worker.start()
        }
    }

    private AgentRunRecord createRun(SessionDto session, SendMessageRequest request, String assistantMessageId, String providerId) {
        Instant now = ChatInMemoryStore.now()
        AgentRunRecord run = new AgentRunRecord(
            id: UUID.randomUUID().toString(),
            sessionId: session.id,
            messageId: assistantMessageId,
            goal: request.content ?: '',
            status: 'queued',
            model: session.model,
            createdAt: now,
            updatedAt: now,
            metadata: [
                provider: providerId,
                mode    : 'agent'
            ] as Map<String, Object>
        )
        store.agentRuns[run.id] = run
        run
    }

    private AgentPlanRecord createPlan(
        AgentRunRecord run,
        LlmProvider provider,
        List<Map<String, Object>> tools,
        List<String> toolNames,
        List<AgentEvidenceRecord> evidence,
        String replanReason,
        AgentContextRecord context
    ) {
        ProviderRequest request = new ProviderRequest(
            sessionId: run.sessionId,
            messageId: UUID.randomUUID().toString(),
            model: run.model,
            tools: [],
            options: [purpose: 'agent_planner', think: false, temperature: 0.3, num_predict: 2048, format: 'json'] as Map<String, Object>,
            messages: [
                new ProviderMessage(role: 'system', content: agentPromptService.plannerSystemPrompt(toolNames)),
                new ProviderMessage(role: 'user', content: agentPromptService.plannerUserPrompt(run, tools, evidence, replanReason, context))
            ]
        )
        Map<String, Object> json = collectStrictJson(provider, request, 'planner')
        AgentPlanRecord plan = planFromJson(run, json, toolNames)
        if (plan.steps.isEmpty()) {
            throw new IllegalStateException('Planner returned no steps')
        }
        if (context != null) {
            plan.metadata.put('contextId', context.id)
            plan.metadata.put('contextVersion', context.version)
        }
        plan
    }

    private AgentPlanRecord createPlanOperation(
        AgentRunRecord run,
        AgentPlanRecord currentPlan,
        AgentStepRecord currentStep,
        LlmProvider provider,
        List<Map<String, Object>> tools,
        List<String> toolNames,
        List<AgentEvidenceRecord> evidence,
        String replanReason,
        AgentContextRecord context,
        List<MessageDto> timeline,
        FluxSink<ChatEventDto> sink,
        String sessionId,
        String messageId
    ) {
        AgentOperationRecord operation = startOperation(run, currentPlan, currentStep, 'planner', replanReason ?: 'Create agent plan.', 1, maxReplans + 1, timeline, sink, sessionId, messageId, [
            replan: replanReason != null,
            contextId: context?.id
        ] as Map<String, Object>)
        operationTrace.set(new OperationTraceContext(run: run, plan: currentPlan, step: currentStep, parentOperation: operation, timeline: timeline, sink: sink, sessionId: sessionId, messageId: messageId))
        try {
            while (true) {
                try {
                    AgentPlanRecord plan = createPlan(run, provider, tools, toolNames, evidence, replanReason, context)
                    completeOperation(operation, "Created plan ${plan.id} with ${plan.steps.size()} step${plan.steps.size() == 1 ? '' : 's'}.".toString(), timeline, sink, sessionId, messageId)
                    return plan
                } catch (Throwable failure) {
                    failOperation(operation, failure, timeline, sink, sessionId, messageId)
                    if (retryOperationIfAvailable(operation, failure.message ?: failure.class.simpleName, timeline, sink, sessionId, messageId)) {
                        continue
                    }
                    exhaustOperation(operation, failure.message ?: failure.class.simpleName, timeline, sink, sessionId, messageId)
                    throw failure
                }
            }
        } finally {
            operationTrace.remove()
        }
    }

    private AgentContextRecord resolveContext(
        AgentRunRecord run,
        LlmProvider provider,
        List<Map<String, Object>> tools,
        List<AgentEvidenceRecord> evidence,
        List<AgentGuidanceRecord> guidance,
        String replanReason,
        AgentSessionContinuityContext sessionContinuity,
        Integer version
    ) {
        List<ChatPromptAssetDefinition> personas = personaAssets()
        List<ChatPromptAssetDefinition> matchingSkills = lookupSkillAssetsForGoal(run.goal)
        ChatPromptRenderResult systemPrompt = agentPromptService.contextResolverSystemPromptRender(toolNames(tools))
        ChatPromptRenderResult userPrompt = agentPromptService.contextResolverUserPromptRender(run, tools, evidence, guidance, replanReason, personas, matchingSkills, renderSessionContinuity(sessionContinuity))
        ProviderRequest request = new ProviderRequest(
            sessionId: run.sessionId,
            messageId: UUID.randomUUID().toString(),
            model: run.model,
            tools: [],
            options: [purpose: 'agent_context_resolver', think: false, temperature: contextResolverTemperature, num_predict: 768, format: 'json'] as Map<String, Object>,
            messages: [
                new ProviderMessage(role: 'system', content: systemPrompt.text),
                new ProviderMessage(role: 'user', content: userPrompt.text)
            ]
        )
        contextFromJson(run, collectStrictJson(provider, request, 'context_resolver'), version ?: 1, systemPrompt, userPrompt, personas, matchingSkills, sessionContinuity)
    }

    private AgentContextRecord resolveContextOperation(
        AgentRunRecord run,
        AgentPlanRecord currentPlan,
        AgentStepRecord currentStep,
        LlmProvider provider,
        List<Map<String, Object>> tools,
        List<AgentEvidenceRecord> evidence,
        List<AgentGuidanceRecord> guidance,
        String replanReason,
        AgentSessionContinuityContext sessionContinuity,
        Integer version,
        List<MessageDto> timeline,
        FluxSink<ChatEventDto> sink,
        String sessionId,
        String messageId
    ) {
        AgentOperationRecord operation = startOperation(run, currentPlan, currentStep, 'context_resolver', replanReason ?: 'Resolve planning context.', version ?: 1, maxReplans + 1, timeline, sink, sessionId, messageId, [
            contextVersion: version ?: 1,
            replan: replanReason != null
        ] as Map<String, Object>)
        operationTrace.set(new OperationTraceContext(run: run, plan: currentPlan, step: currentStep, parentOperation: operation, timeline: timeline, sink: sink, sessionId: sessionId, messageId: messageId))
        try {
            AgentSessionContinuityContext activeContinuity = sessionContinuity
            int siftPage = 0
            while (true) {
                try {
                    AgentContextRecord context = resolveContext(run, provider, tools, evidence, guidance, replanReason, activeContinuity, (version ?: 1) + siftPage)
                    if (contextRequestsSift(context, activeContinuity) && siftPage < MAX_CONTEXT_SIFT_PAGES) {
                        siftPage++
                        activeContinuity = siftSessionContinuity(run, currentPlan, currentStep, timeline, sink, sessionId, messageId, siftPage, context)
                        continue
                    }
                    completeOperation(operation, "Resolved context ${context.id}.".toString(), timeline, sink, sessionId, messageId)
                    return context
                } catch (Throwable failure) {
                    failOperation(operation, failure, timeline, sink, sessionId, messageId)
                    if (retryOperationIfAvailable(operation, failure.message ?: failure.class.simpleName, timeline, sink, sessionId, messageId)) {
                        continue
                    }
                    exhaustOperation(operation, failure.message ?: failure.class.simpleName, timeline, sink, sessionId, messageId)
                    throw failure
                }
            }
        } finally {
            operationTrace.remove()
        }
    }

    private AgentSessionContinuityContext siftSessionContinuity(
        AgentRunRecord run,
        AgentPlanRecord currentPlan,
        AgentStepRecord currentStep,
        List<MessageDto> timeline,
        FluxSink<ChatEventDto> sink,
        String sessionId,
        String messageId,
        int page,
        AgentContextRecord requestingContext
    ) {
        AgentOperationRecord siftOperation = startChildOperation(operationTrace.get(), 'context_sifter', "Sift session context page ${page}.".toString(), page, MAX_CONTEXT_SIFT_PAGES, [
            contextId: requestingContext?.id,
            contextRequests: requestingContext?.contextRequests ?: ([] as List<Map<String, Object>>),
            page: page,
            direction: 'newest_to_oldest'
        ] as Map<String, Object>)
        try {
            AgentSessionContinuityContext sifted = buildSessionContinuity(run, timeline, page)
            String rendered = renderSessionContinuity(sifted)
            completeChildOperation(siftOperation, "Loaded session context page ${page} (${rendered.length()} chars).".toString())
            emit(timeline, sink, sessionId, agentEvent('agent_context_sifted', messageId, 'system', "Loaded session context page ${page}.", false, [
                runId: run.id,
                planId: currentPlan?.id,
                stepId: currentStep?.id,
                page: page,
                pageSize: sifted.metadata?.get('pageSize'),
                hasOlderPages: sifted.metadata?.get('hasOlderPages'),
                contentChars: rendered.length(),
                contextRequests: requestingContext?.contextRequests ?: ([] as List<Map<String, Object>>),
                visibility: 'debug'
            ] as Map<String, Object>))
            sifted
        } catch (Throwable failure) {
            failChildOperation(siftOperation, failure)
            exhaustChildOperation(siftOperation, failure.message ?: failure.class.simpleName)
            throw failure
        }
    }

    private static boolean contextRequestsSift(AgentContextRecord context, AgentSessionContinuityContext continuity) {
        if (context == null || !context.contextRequests) {
            return false
        }
        if (continuity != null && Boolean.FALSE == continuity.metadata?.get('hasOlderPages')) {
            return false
        }
        Set<String> siftingTypes = [
            'session_memory_page',
            'session_resource_lookup',
            'session_context_page',
            'session_memory_lookup'
        ] as Set<String>
        context.contextRequests.any {Map<String, Object> request ->
            siftingTypes.contains(asString(request.get('type')))
        }
    }

    private StepExecution executeStep(
        AgentRunRecord run,
        AgentPlanRecord plan,
        AgentContextRecord context,
        AgentStepRecord step,
        List<AgentEvidenceRecord> priorEvidence,
        List<AgentGuidanceRecord> priorGuidance,
        LlmProvider provider,
        List<Map<String, Object>> allTools,
        List<MessageDto> timeline,
        FluxSink<ChatEventDto> sink,
        String sessionId,
        String assistantMessageId,
        HttpRequest<?> httpRequest
    ) {
        AgentOperationRecord executorOperation = startOperation(run, plan, step, 'executor', "Execute step: ${step.title ?: step.id}".toString(), step.attemptCount ?: 1, maxStepRetries + 1, timeline, sink, sessionId, assistantMessageId)
        try {
        List<Map<String, Object>> stepTools = filterTools(allTools, step.allowedTools)
        ProviderRequest request = new ProviderRequest(
            sessionId: run.sessionId,
            messageId: assistantMessageId,
            model: run.model,
            tools: stepTools,
            options: [purpose: 'agent_executor', think: false, temperature: 0.1, _mauroDisableToolLoop: true, _mauroForwardHeaders: forwardedHeaders(httpRequest)] as Map<String, Object>,
            messages: [
                new ProviderMessage(role: 'system', content: agentPromptService.executorSystemPrompt(step)),
                new ProviderMessage(role: 'user', content: agentPromptService.executorUserPrompt(run, plan, context, step, priorEvidence, priorGuidance))
            ]
        )

        StringBuilder text = new StringBuilder(1024)
        List<Map<String, Object>> calls = new ArrayList<Map<String, Object>>()
        List<AgentEvidenceRecord> evidence = new ArrayList<AgentEvidenceRecord>()
        List<AgentGuidanceRecord> guidance = new ArrayList<AgentGuidanceRecord>()
        for (ProviderChunk chunk : Flux.from(provider.streamChat(request)).collectList().block() ?: []) {
            if (chunk.type == 'token') {
                text.append(chunk.content ?: '')
            } else if (chunk.type == 'tool_call') {
                Map<String, Object> meta = new LinkedHashMap<String, Object>(chunk.metadata ?: [:])
                meta.put('runId', run.id)
                meta.put('planId', plan.id)
                meta.put('stepId', step.id)
                calls.add(meta)
                emit(timeline, sink, sessionId, new ChatEventDto(type: 'tool_call', messageId: assistantMessageId, role: 'assistant', content: '', done: false, metadata: meta))
            } else if (chunk.type == 'tool_result') {
                Map<String, Object> meta = new LinkedHashMap<String, Object>(chunk.metadata ?: [:])
                meta.put('runId', run.id)
                meta.put('planId', plan.id)
                meta.put('stepId', step.id)
                String toolName = toolNameFromResultMeta(meta)
                String resolvedModelText = resolveModelTextFromResultMeta(toolName, meta)
                AgentEvidenceRecord observed = evidenceFromToolResult(run, step, meta)
                evidence.add(observed)
                store.agentEvidence[observed.id] = observed
                meta.put('evidenceId', observed.id)
                boolean observedPertinentToFinal = Boolean.TRUE == observed.metadata?.get('pertinentToFinal')
                AgentGuidanceRecord observedGuidance = guidanceFromToolResult(run, step, toolName, asString(meta.get('callId')), Boolean.TRUE == meta.get('ok'), resolvedModelText, observedPertinentToFinal)
                if (observedGuidance != null) {
                    linkGuidanceToEvidence(observed, observedGuidance)
                    guidance.add(observedGuidance)
                    store.agentGuidance[observedGuidance.id] = observedGuidance
                    meta.put('guidanceId', observedGuidance.id)
                    emit(timeline, sink, sessionId, agentEvent('agent_guidance_added', assistantMessageId, 'system', observedGuidance.content ?: '', false, [
                        runId: run.id,
                        planId: plan.id,
                        stepId: step.id,
                        guidanceId: observedGuidance.id,
                        sourceName: observedGuidance.sourceName,
                        followForFinal: observedGuidance.followForFinal
                    ] as Map<String, Object>))
                }
                emit(timeline, sink, sessionId, new ChatEventDto(type: 'tool_result', messageId: assistantMessageId, role: 'assistant', content: '', done: false, metadata: meta))
                emit(timeline, sink, sessionId, agentEvent('agent_evidence_added', assistantMessageId, 'system', observed.summary ?: '', false, [
                    runId: run.id,
                    planId: plan.id,
                    stepId: step.id,
                    evidenceId: observed.id,
                    sourceName: observed.sourceName
                ] as Map<String, Object>))
            } else if (chunk.type == 'error') {
                emit(timeline, sink, sessionId, new ChatEventDto(type: 'error', messageId: assistantMessageId, role: 'assistant', content: chunk.content ?: 'Provider error', done: false, metadata: [
                    runId: run.id,
                    planId: plan.id,
                    stepId: step.id
                ] as Map<String, Object>))
            }
        }

        String executorOutput = text.toString().trim()
        if (!executorOutput.isEmpty() && evidence.isEmpty() && calls.isEmpty()) {
            AgentEvidenceRecord observed = evidenceFromExecutorOutput(run, step, executorOutput)
            evidence.add(observed)
            store.agentEvidence[observed.id] = observed
            emit(timeline, sink, sessionId, agentEvent('agent_evidence_added', assistantMessageId, 'system', observed.summary ?: '', false, [
                runId: run.id,
                planId: plan.id,
                stepId: step.id,
                evidenceId: observed.id,
                sourceName: observed.sourceName
            ] as Map<String, Object>))
        }

        for (Map<String, Object> call : calls) {
            String callId = asString(call.get('callId')) ?: UUID.randomUUID().toString()
            if (evidence.any {AgentEvidenceRecord item -> item.metadata?.get('callId') == callId}) {
                continue
            }
            String toolName = asString(call.get('name'))
            Map<String, Object> arguments = completeToolArgumentsFromContext(context, toolName, getMap(call.get('arguments')))
            AgentOperationRecord toolOperation = startOperation(run, plan, step, 'tool_call', "Call tool ${toolName ?: 'unknown'}".toString(), 1, 1, timeline, sink, sessionId, assistantMessageId, [
                toolName: toolName,
                callId: callId,
                arguments: arguments
            ] as Map<String, Object>)
            AgentActionRecord action = new AgentActionRecord(
                id: UUID.randomUUID().toString(),
                runId: run.id,
                stepId: step.id,
                kind: 'tool',
                status: 'in_progress',
                toolName: toolName,
                callId: callId,
                arguments: arguments,
                startedAt: ChatInMemoryStore.now()
            )
            store.agentActions[action.id] = action
            ToolInvokeResponse response
            try {
                response = invokeTool(toolName, arguments, httpRequest)
            } catch (Throwable toolFailure) {
                response = new ToolInvokeResponse(
                    success: false,
                    result: [
                        tool: toolName,
                        arguments: arguments,
                        blocked: true
                    ] as Map<String, Object>,
                    error: toolFailure.message ?: toolFailure.class.simpleName,
                    modelText: "Tool ${toolName} was not executed: ${toolFailure.message ?: toolFailure.class.simpleName}".toString()
                )
            }
            action.status = Boolean.TRUE == response.success ? 'completed' : 'failed'
            action.error = response.error
            action.completedAt = ChatInMemoryStore.now()
            if (Boolean.TRUE == response.success) {
                completeOperation(toolOperation, "Tool ${toolName} completed.".toString(), timeline, sink, sessionId, assistantMessageId)
            } else {
                toolOperation.error = response.error ?: "Tool ${toolName} failed.".toString()
                toolOperation.completedAt = ChatInMemoryStore.now()
                transitionOperation(toolOperation, AgentStateTransitionService.OP_FAILED, 'operation_failed', toolOperation.error, timeline, sink, sessionId, assistantMessageId)
                exhaustOperation(toolOperation, toolOperation.error, timeline, sink, sessionId, assistantMessageId)
            }
            Map<String, Object> resultMeta = [
                callId: callId,
                ok: response.success,
                arguments: arguments,
                output: response.result,
                error: response.error
            ] as Map<String, Object>
            String resolvedModelText = resolveModelText(toolName, response)
            AgentEvidenceRecord observed = evidenceFromToolResult(run, step, resultMeta)
            action.resultRef = observed.id
            evidence.add(observed)
            store.agentEvidence[observed.id] = observed
            boolean observedPertinentToFinal = Boolean.TRUE == observed.metadata?.get('pertinentToFinal')
            AgentGuidanceRecord observedGuidance = guidanceFromToolResult(run, step, toolName, callId, Boolean.TRUE == response.success, resolvedModelText, observedPertinentToFinal)
            if (observedGuidance != null) {
                linkGuidanceToEvidence(observed, observedGuidance)
                guidance.add(observedGuidance)
                store.agentGuidance[observedGuidance.id] = observedGuidance
                resultMeta.put('guidanceId', observedGuidance.id)
                emit(timeline, sink, sessionId, agentEvent('agent_guidance_added', assistantMessageId, 'system', observedGuidance.content ?: '', false, [
                    runId: run.id,
                    planId: plan.id,
                    stepId: step.id,
                    guidanceId: observedGuidance.id,
                    sourceName: observedGuidance.sourceName,
                    followForFinal: observedGuidance.followForFinal
                ] as Map<String, Object>))
            }
            resultMeta.put('runId', run.id)
            resultMeta.put('planId', plan.id)
            resultMeta.put('stepId', step.id)
            resultMeta.put('evidenceId', observed.id)
            emit(timeline, sink, sessionId, new ChatEventDto(type: 'tool_result', messageId: assistantMessageId, role: 'assistant', content: '', done: false, metadata: resultMeta))
            emit(timeline, sink, sessionId, agentEvent('agent_evidence_added', assistantMessageId, 'system', observed.summary ?: '', false, [
                runId: run.id,
                planId: plan.id,
                stepId: step.id,
                evidenceId: observed.id,
                sourceName: observed.sourceName
            ] as Map<String, Object>))
        }

        StepExecution result = new StepExecution(summary: executorOutput, evidence: evidence, guidance: guidance, toolCalls: evidence.size())
        completeOperation(executorOperation, "Executor produced ${evidence.size()} evidence item${evidence.size() == 1 ? '' : 's'}.".toString(), timeline, sink, sessionId, assistantMessageId)
        result
        } catch (Throwable failure) {
            failOperation(executorOperation, failure, timeline, sink, sessionId, assistantMessageId)
            exhaustOperation(executorOperation, failure.message ?: failure.class.simpleName, timeline, sink, sessionId, assistantMessageId)
            throw failure
        }
    }

    private StepAssessment evaluateStep(
        AgentRunRecord run,
        AgentPlanRecord plan,
        AgentContextRecord context,
        AgentStepRecord step,
        List<AgentEvidenceRecord> evidence,
        List<AgentEvidenceRecord> currentStepEvidence,
        List<AgentGuidanceRecord> guidance,
        LlmProvider provider
    ) {
        ProviderRequest request = new ProviderRequest(
            sessionId: run.sessionId,
            messageId: UUID.randomUUID().toString(),
            model: run.model,
            tools: [],
            options: [purpose: 'agent_step_evaluator', think: false, temperature: 0.0, num_predict: 1024, format: 'json'] as Map<String, Object>,
            messages: [
                new ProviderMessage(role: 'system', content: agentPromptService.stepEvaluatorSystemPrompt()),
                new ProviderMessage(role: 'user', content: agentPromptService.stepEvaluatorUserPrompt(run, plan, context, step, evidence, currentStepEvidence, guidance))
            ]
        )
        Map<String, Object> json = collectStrictJson(provider, request, 'step_evaluator')
        String decision = asString(json.get('decision')) ?: 'continue'
        StepDecisionTransition transition = transitionService.normalizeStepDecision(decision, Boolean.TRUE == json.get('stepComplete'), step.attemptCount ?: 0, maxStepRetries)
        new StepAssessment(
            completed: Boolean.TRUE == transition.stepComplete,
            decision: transition.decision,
            summary: asString(json.get('summary')),
            reason: asString(json.get('reason')),
            question: asString(json.get('question'))
        )
    }

    private StepAssessment evaluateStepOperation(
        AgentRunRecord run,
        AgentPlanRecord plan,
        AgentContextRecord context,
        AgentStepRecord step,
        List<AgentEvidenceRecord> evidence,
        List<AgentEvidenceRecord> currentStepEvidence,
        List<AgentGuidanceRecord> guidance,
        LlmProvider provider,
        List<MessageDto> timeline,
        FluxSink<ChatEventDto> sink,
        String sessionId,
        String messageId
    ) {
        AgentOperationRecord operation = startOperation(run, plan, step, 'step_evaluator', "Evaluate step: ${step.title ?: step.id}".toString(), step.attemptCount ?: 1, maxStepRetries + 1, timeline, sink, sessionId, messageId)
        operationTrace.set(new OperationTraceContext(run: run, plan: plan, step: step, parentOperation: operation, timeline: timeline, sink: sink, sessionId: sessionId, messageId: messageId))
        try {
            while (true) {
                try {
                    StepAssessment assessment = evaluateStep(run, plan, context, step, evidence, currentStepEvidence, guidance, provider)
                    completeOperation(operation, "${assessment.decision}: ${assessment.summary ?: assessment.reason ?: ''}".toString(), timeline, sink, sessionId, messageId)
                    return assessment
                } catch (Throwable failure) {
                    failOperation(operation, failure, timeline, sink, sessionId, messageId)
                    if (retryOperationIfAvailable(operation, failure.message ?: failure.class.simpleName, timeline, sink, sessionId, messageId)) {
                        continue
                    }
                    exhaustOperation(operation, failure.message ?: failure.class.simpleName, timeline, sink, sessionId, messageId)
                    throw failure
                }
            }
        } finally {
            operationTrace.remove()
        }
    }

    private PlanAssessment evaluatePlan(
        AgentRunRecord run,
        AgentPlanRecord plan,
        AgentContextRecord context,
        AgentStepRecord completedStep,
        List<AgentEvidenceRecord> evidence,
        List<AgentGuidanceRecord> guidance,
        LlmProvider provider
    ) {
        ProviderRequest request = new ProviderRequest(
            sessionId: run.sessionId,
            messageId: UUID.randomUUID().toString(),
            model: run.model,
            tools: [],
            options: [purpose: 'agent_plan_evaluator', think: false, temperature: 0.0, num_predict: 1024, format: 'json'] as Map<String, Object>,
            messages: [
                new ProviderMessage(role: 'system', content: agentPromptService.planEvaluatorSystemPrompt()),
                new ProviderMessage(role: 'user', content: agentPromptService.planEvaluatorUserPrompt(run, plan, context, completedStep, evidence, guidance))
            ]
        )
        Map<String, Object> json = collectStrictJson(provider, request, 'plan_evaluator')
        new PlanAssessment(
            decision: asString(json.get('decision')) ?: 'continue',
            summary: asString(json.get('summary')),
            reason: asString(json.get('reason')),
            question: asString(json.get('question')),
            missing: asStringList(json.get('missing')),
            obsoleteStepIds: asStringList(json.get('obsoleteStepIds'))
        )
    }

    private PlanAssessment evaluatePlanOperation(
        AgentRunRecord run,
        AgentPlanRecord plan,
        AgentContextRecord context,
        AgentStepRecord completedStep,
        List<AgentEvidenceRecord> evidence,
        List<AgentGuidanceRecord> guidance,
        LlmProvider provider,
        List<MessageDto> timeline,
        FluxSink<ChatEventDto> sink,
        String sessionId,
        String messageId
    ) {
        AgentOperationRecord operation = startOperation(run, plan, completedStep, 'plan_evaluator', "Evaluate plan after step: ${completedStep.title ?: completedStep.id}".toString(), 1, 1, timeline, sink, sessionId, messageId)
        operationTrace.set(new OperationTraceContext(run: run, plan: plan, step: completedStep, parentOperation: operation, timeline: timeline, sink: sink, sessionId: sessionId, messageId: messageId))
        try {
            while (true) {
                try {
                    PlanAssessment assessment = evaluatePlan(run, plan, context, completedStep, evidence, guidance, provider)
                    completeOperation(operation, "${assessment.decision}: ${assessment.summary ?: assessment.reason ?: ''}".toString(), timeline, sink, sessionId, messageId)
                    return assessment
                } catch (Throwable failure) {
                    failOperation(operation, failure, timeline, sink, sessionId, messageId)
                    if (retryOperationIfAvailable(operation, failure.message ?: failure.class.simpleName, timeline, sink, sessionId, messageId)) {
                        continue
                    }
                    exhaustOperation(operation, failure.message ?: failure.class.simpleName, timeline, sink, sessionId, messageId)
                    throw failure
                }
            }
        } finally {
            operationTrace.remove()
        }
    }

    private void writeFinal(
        AgentRunRecord run,
        AgentPlanRecord plan,
        AgentContextRecord context,
        List<AgentEvidenceRecord> evidence,
        List<AgentGuidanceRecord> guidance,
        LlmProvider provider,
        List<MessageDto> timeline,
        FluxSink<ChatEventDto> sink,
        String sessionId,
        String assistantMessageId
    ) {
        emit(timeline, sink, sessionId, agentEvent('agent_final_started', assistantMessageId, 'system', '', false, [
            runId: run.id,
            planId: plan.id
        ] as Map<String, Object>))
        emitProgress(timeline, sink, sessionId, assistantMessageId, 'final_started', 'Preparing the final answer.', [
            runId: run.id,
            planId: plan.id
        ] as Map<String, Object>)
        List<AgentEvidenceRecord> renderedEvidence = AgentPromptService.finalAnswerEvidence(evidence, run.id)
        if (renderedEvidence.isEmpty()) {
            renderedEvidence = evidence ?: ([] as List<AgentEvidenceRecord>)
        }
        List<AgentGuidanceRecord> renderedGuidance = AgentPromptService.finalAnswerGuidance(guidance, renderedEvidence)
        ChatPromptRenderResult finalSystemPrompt = agentPromptService.finalWriterSystemPromptRender()
        ChatPromptRenderResult finalUserPrompt = agentPromptService.finalWriterUserPromptRender(run, plan, context, evidence, guidance)
        String finalContext = finalUserPrompt.text
        emit(timeline, sink, sessionId, agentEvent('agent_final_context', assistantMessageId, 'system', finalContext, false, [
            runId: run.id,
            planId: plan.id,
            evidenceCount: evidence.size(),
            renderedEvidenceIds: renderedEvidence.collect {AgentEvidenceRecord item -> item.id},
            renderedGuidanceIds: renderedGuidance.collect {AgentGuidanceRecord item -> item.id},
            guidanceLinks: guidanceLinks(renderedEvidence, renderedGuidance),
            systemPrompt: finalSystemPrompt.toMetadata(),
            userPrompt: finalUserPrompt.toMetadata(),
            visibility: 'debug',
            source: 'agent_final'
        ] as Map<String, Object>))
        ProviderRequest request = new ProviderRequest(
            sessionId: run.sessionId,
            messageId: assistantMessageId,
            model: run.model,
            tools: [],
            options: [purpose: 'agent_final', think: false, temperature: 0.2, num_predict: 4096] as Map<String, Object>,
            messages: [
                new ProviderMessage(role: 'system', content: finalSystemPrompt.text),
                new ProviderMessage(role: 'user', content: finalContext)
            ]
        )
        for (ProviderChunk chunk : Flux.from(provider.streamChat(request)).collectList().block() ?: []) {
            if (chunk.type == 'token') {
                emit(timeline, sink, sessionId, new ChatEventDto(type: 'token', messageId: assistantMessageId, role: 'assistant', content: chunk.content ?: '', done: false, metadata: [
                    runId: run.id,
                    planId: plan.id,
                    source: 'agent_final'
                ] as Map<String, Object>))
            } else if (chunk.type == 'error') {
                emit(timeline, sink, sessionId, new ChatEventDto(type: 'error', messageId: assistantMessageId, role: 'assistant', content: chunk.content ?: 'Provider error', done: false, metadata: [
                    runId: run.id,
                    planId: plan.id,
                    source: 'agent_final'
                ] as Map<String, Object>))
            }
        }
    }

    private void writeFinalOperation(
        AgentRunRecord run,
        AgentPlanRecord plan,
        AgentContextRecord context,
        List<AgentEvidenceRecord> evidence,
        List<AgentGuidanceRecord> guidance,
        LlmProvider provider,
        List<MessageDto> timeline,
        FluxSink<ChatEventDto> sink,
        String sessionId,
        String assistantMessageId
    ) {
        AgentOperationRecord operation = startOperation(run, plan, null, 'final_writer', 'Prepare final answer.', 1, 1, timeline, sink, sessionId, assistantMessageId)
        try {
            writeFinal(run, plan, context, evidence, guidance, provider, timeline, sink, sessionId, assistantMessageId)
            completeOperation(operation, 'Final answer written.', timeline, sink, sessionId, assistantMessageId)
        } catch (Throwable failure) {
            failOperation(operation, failure, timeline, sink, sessionId, assistantMessageId)
            exhaustOperation(operation, failure.message ?: failure.class.simpleName, timeline, sink, sessionId, assistantMessageId)
            throw failure
        }
    }

    private ToolInvokeResponse invokeTool(String toolName, Map<String, Object> arguments, HttpRequest<?> httpRequest) {
        Map<String, Object> safeArgs = new LinkedHashMap<String, Object>(arguments ?: ([:] as Map<String, Object>))
        Map<String, List<String>> headers = forwardedHeaders(httpRequest)
        chatMcpService.invokeTool(toolName, new ToolInvokeRequest(arguments: safeArgs, forwardHeaders: headers))
    }

    private static Map<String, Object> completeToolArgumentsFromContext(AgentContextRecord context, String toolName, Map<String, Object> arguments) {
        Map<String, Object> completed = new LinkedHashMap<String, Object>(arguments ?: [:] as Map<String, Object>)
        if (context == null || toolName == null || toolName.trim().isEmpty()) {
            return completed
        }
        if (!(context.followUpInterpretation in ['paging_request', 'continuation'])) {
            return completed
        }
        Map<String, Object> nextArguments = nextPagingArgumentsForTool(context, toolName)
        if (!nextArguments) {
            return completed
        }
        Object requestedOffset = completed.get('offset')
        Object nextOffset = nextArguments.get('offset')
        if (requestedOffset != null && requestedOffset != nextOffset) {
            return completed
        }
        for (Map.Entry<String, Object> entry : nextArguments.entrySet()) {
            Object existing = completed.get(entry.key)
            if (existing == null || existing.toString().trim().isEmpty()) {
                completed.put(entry.key, entry.value)
            }
        }
        if (nextOffset != null) {
            completed.put('offset', nextOffset)
        }
        completed
    }

    private static Map<String, Object> nextPagingArgumentsForTool(AgentContextRecord context, String toolName) {
        Map<String, Object> continuity = getMap(context.metadata?.get('sessionContinuityStructured'))
        List<Map<String, Object>> paging = asMapList(continuity.get('pagingState'))
        for (Map<String, Object> page : paging) {
            if (asString(page.get('toolName')) != toolName) {
                continue
            }
            Object hasMore = page.get('hasMore')
            Object nextOffset = page.get('nextOffset')
            if (!(hasMore == true || nextOffset != null)) {
                continue
            }
            Map<String, Object> nextArguments = getMap(page.get('nextArguments'))
            if (nextArguments) {
                return nextArguments
            }
            Map<String, Object> baseArguments = getMap(page.get('arguments'))
            if (!baseArguments) {
                continue
            }
            Map<String, Object> out = new LinkedHashMap<String, Object>(baseArguments)
            if (nextOffset != null) {
                out.put('offset', nextOffset)
            }
            Object max = page.get('max')
            if (max != null && !out.containsKey('max')) {
                out.put('max', max)
            }
            return out
        }
        [:] as Map<String, Object>
    }

    private String resolveModelText(String toolName, ToolInvokeResponse response) {
        String existing = response?.modelText
        if (existing != null && !existing.trim().isEmpty()) {
            return existing
        }
        if (Boolean.TRUE != response?.success) {
            return existing
        }
        Map<String, Object> responseResult = response?.result ?: [:] as Map<String, Object>
        Map<String, Object> output = getMap(responseResult.get('output'))
        if (output.isEmpty()) {
            return existing
        }
        chatMcpService.renderModelText(toolName, output)
    }

    private String resolveModelTextFromResultMeta(String toolName, Map<String, Object> resultMeta) {
        if (toolName == null || toolName.trim().isEmpty()) {
            return null
        }
        if (Boolean.TRUE != resultMeta?.get('ok')) {
            return null
        }
        Map<String, Object> responseResult = getMap(resultMeta.get('output'))
        Map<String, Object> output = getMap(responseResult.get('output'))
        if (output.isEmpty()) {
            return null
        }
        chatMcpService.renderModelText(toolName, output)
    }

    private static String toolNameFromResultMeta(Map<String, Object> resultMeta) {
        asString(getMap(resultMeta?.get('output')).get('tool')) ?: asString(resultMeta?.get('name')) ?: asString(resultMeta?.get('tool')) ?: 'tool'
    }

    private AgentPlanRecord planFromJson(AgentRunRecord run, Map<String, Object> json, List<String> toolNames) {
        Instant now = ChatInMemoryStore.now()
        AgentPlanRecord plan = new AgentPlanRecord(
            id: UUID.randomUUID().toString(),
            runId: run.id,
            version: 1,
            status: 'active',
            fitness: asString(json.get('fitness')) ?: 'draft',
            goalRestatement: asString(json.get('goalRestatement')) ?: run.goal,
            successCriteria: asStringList(json.get('successCriteria')),
            assumptions: asStringList(json.get('assumptions')),
            risks: asStringList(json.get('risks')),
            createdAt: now,
            metadata: [raw: json] as Map<String, Object>
        )
        Object stepsObj = json.get('steps')
        if (stepsObj instanceof Collection) {
            int ordinal = 1
            for (Object stepObj : (Collection<?>) stepsObj) {
                if (!(stepObj instanceof Map)) {
                    continue
                }
                @SuppressWarnings('unchecked')
                Map<String, Object> stepMap = (Map<String, Object>) stepObj
                List<String> rawAllowedTools = asStringList(stepMap.get('allowedTools'))
                Set<String> validToolNames = new LinkedHashSet<String>(toolNames ?: [])
                List<String> allowedTools = rawAllowedTools.findAll {String toolName ->
                    validToolNames.contains(toolName)
                } as List<String>
                List<String> invalidAllowedTools = rawAllowedTools.findAll {String toolName ->
                    !validToolNames.contains(toolName)
                } as List<String>
                Boolean optional = asBoolean(stepMap.get('optional'), false)
                String rawGuard = asString(stepMap.get('guard'))
                String guard = normalizeStepGuard(rawGuard)
                boolean guardNormalizedForRequiredStep = false
                if (!optional && guard != 'always') {
                    guardNormalizedForRequiredStep = true
                    guard = 'always'
                }
                AgentStepRecord step = new AgentStepRecord(
                    id: UUID.randomUUID().toString(),
                    runId: run.id,
                    planId: plan.id,
                    ordinal: ordinal++,
                    title: asString(stepMap.get('title')) ?: "Step ${ordinal}".toString(),
                    objective: asString(stepMap.get('objective')) ?: '',
                    kind: asString(stepMap.get('kind')) ?: 'tool',
                    status: 'pending',
                    allowedTools: allowedTools,
                    guard: guard,
                    guardReason: asString(stepMap.get('guardReason')),
                    optional: optional,
                    expectedOutput: asString(stepMap.get('expectedOutput')),
                    successCriteria: asStringList(stepMap.get('successCriteria')),
                    metadata: [
                        raw: stepMap,
                        invalidAllowedTools: invalidAllowedTools,
                        rawGuard: rawGuard,
                        guardNormalizedForRequiredStep: guardNormalizedForRequiredStep
                    ] as Map<String, Object>
                )
                plan.steps.add(step)
                store.agentSteps[step.id] = step
            }
        }
        plan
    }

    private static AgentContextRecord contextFromJson(
        AgentRunRecord run,
        Map<String, Object> json,
        Integer version,
        ChatPromptRenderResult systemPrompt,
        ChatPromptRenderResult userPrompt,
        List<ChatPromptAssetDefinition> personas,
        List<ChatPromptAssetDefinition> matchingSkills,
        AgentSessionContinuityContext sessionContinuity
    ) {
        new AgentContextRecord(
            id: UUID.randomUUID().toString(),
            runId: run.id,
            version: version ?: 1,
            status: 'resolved',
            goalRestatement: asString(json.get('goalRestatement')) ?: run.goal,
            followUpInterpretation: asString(json.get('followUpInterpretation')),
            goalFrame: getMap(json.get('goalFrame')),
            domainContext: asStringList(json.get('domainContext')),
            relevantTools: asMapList(json.get('relevantTools')),
            recommendedSkills: asMapList(json.get('recommendedSkills')),
            relevantResources: asMapList(json.get('relevantResources')),
            instructions: asMapList(json.get('instructions')),
            resolvedReferences: asMapList(json.get('resolvedReferences')),
            resolvedResources: asMapList(json.get('resolvedResources')),
            priorEvidenceToReuse: asMapList(json.get('priorEvidenceToReuse')),
            priorGuidanceToFollow: asMapList(json.get('priorGuidanceToFollow')),
            contextRequests: asMapList(json.get('contextRequests')),
            planningHints: asStringList(json.get('planningHints')),
            constraints: asStringList(json.get('constraints')),
            createdAt: ChatInMemoryStore.now(),
            metadata: [
                raw: json,
                sessionContinuity: renderSessionContinuity(sessionContinuity),
                sessionContinuityStructured: sessionContinuityToMap(sessionContinuity),
                systemPrompt: systemPrompt.text,
                userPrompt: userPrompt.text,
                systemPromptMetadata: systemPrompt.toMetadata(),
                userPromptMetadata: userPrompt.toMetadata(),
                personaGuidance: AgentPromptService.renderPersonaSkills(personas),
                skillLookupGuidance: AgentPromptService.renderSkillLookup(matchingSkills, true),
                personaSkills: skillSummaries(personas),
                skillLookupResults: skillSummaries(matchingSkills)
            ] as Map<String, Object>
        )
    }

    private Map<String, Object> collectStrictJson(LlmProvider provider, ProviderRequest request, String roleName) {
        String text = collectStrictJsonText(provider, request, roleName)
        try {
            return parseStrictJsonObject(text, roleName)
        } catch (Exception firstFailure) {
            return repairStrictJson(provider, request, roleName, text, firstFailure)
        }
    }

    private Map<String, Object> repairStrictJson(LlmProvider provider, ProviderRequest originalRequest, String roleName, String malformedText, Exception firstFailure) {
        String error = firstFailure.message ?: firstFailure.class.simpleName
        OperationTraceContext trace = operationTrace.get()
        AgentOperationRecord repairOperation = startChildOperation(trace, 'json_repair', "Repair strict JSON for ${roleName}.".toString(), 1, 2, [
            roleName: roleName,
            parseError: error
        ] as Map<String, Object>)
        ProviderRequest repairRequest = new ProviderRequest(
            sessionId: originalRequest.sessionId,
            messageId: UUID.randomUUID().toString(),
            model: originalRequest.model,
            tools: [],
            options: [
                purpose: "agent_${roleName}_json_repair".toString(),
                think: false,
                temperature: 0.0,
                num_predict: Math.max(asInteger(originalRequest.options?.get('num_predict'), 2048), 4096),
                format: 'json'
            ] as Map<String, Object>,
            messages: [
                new ProviderMessage(role: 'system', content: agentPromptService.strictJsonRepairSystemPrompt(roleName)),
                new ProviderMessage(role: 'user', content: agentPromptService.strictJsonRepairUserPrompt(roleName, malformedText, error))
            ]
        )
        String repairedText = collectStrictJsonText(provider, repairRequest, "${roleName} JSON repair".toString())
        try {
            Map<String, Object> repaired = parseStrictJsonObject(repairedText, roleName)
            completeChildOperation(repairOperation, "JSON repair succeeded for ${roleName}.".toString())
            return repaired
        } catch (Exception repairFailure) {
            String repairedError = repairFailure.message ?: repairFailure.class.simpleName
            failChildOperation(repairOperation, repairFailure)
            try {
                OperationTraceContext traceForRetry = operationTrace.get()
                if (traceForRetry != null) {
                    retryOperationIfAvailable(repairOperation, repairedError, traceForRetry.timeline, traceForRetry.sink, traceForRetry.sessionId, traceForRetry.messageId)
                }
                Map<String, Object> cleaned = retryStrictJsonClean(provider, originalRequest, roleName)
                completeChildOperation(repairOperation, "JSON clean retry recovered ${roleName}.".toString())
                return cleaned
            } catch (Exception retryFailure) {
                exhaustChildOperation(repairOperation, retryFailure.message ?: retryFailure.class.simpleName)
                throw new StrictJsonParseException(
                    "${roleName} returned malformed JSON after repair attempt: ${repairedError}".toString(),
                    roleName,
                    error,
                    snippet(malformedText),
                    repairedError,
                    snippet(repairedText)
                )
            }
        }
    }

    private Map<String, Object> retryStrictJsonClean(LlmProvider provider, ProviderRequest originalRequest, String roleName) {
        AgentOperationRecord retryOperation = startChildOperation(operationTrace.get(), 'json_clean_retry', "Clean strict JSON retry for ${roleName}.".toString(), 2, 2, [
            roleName: roleName
        ] as Map<String, Object>)
        Map<String, Object> options = new LinkedHashMap<String, Object>(originalRequest.options ?: [:] as Map<String, Object>)
        options.put('purpose', "agent_${roleName}_json_retry".toString())
        options.put('think', false)
        options.put('temperature', 0.2d)
        options.put('num_predict', Math.max(asInteger(options.get('num_predict'), 2048), 4096))
        options.put('format', 'json')
        options.put('seed', Math.abs(UUID.randomUUID().hashCode()))

        List<ProviderMessage> retryMessages = new ArrayList<ProviderMessage>()
        retryMessages.add(new ProviderMessage(role: 'system', content: agentPromptService.strictJsonCleanRetrySystemPrompt(roleName)))
        retryMessages.addAll((originalRequest.messages ?: []) as List<ProviderMessage>)

        ProviderRequest retryRequest = new ProviderRequest(
            sessionId: originalRequest.sessionId,
            messageId: UUID.randomUUID().toString(),
            model: originalRequest.model,
            tools: [],
            options: options,
            messages: retryMessages
        )
        try {
            Map<String, Object> retried = parseStrictJsonObject(collectStrictJsonText(provider, retryRequest, "${roleName} clean JSON retry".toString()), roleName)
            completeChildOperation(retryOperation, "Clean JSON retry succeeded for ${roleName}.".toString())
            return retried
        } catch (Exception retryFailure) {
            failChildOperation(retryOperation, retryFailure)
            exhaustChildOperation(retryOperation, retryFailure.message ?: retryFailure.class.simpleName)
            throw retryFailure
        }
    }

    private Map<String, Object> parseStrictJsonObject(String text, String roleName) {
        String cleaned = stripCodeFence(text ?: '')
        Object parsed
        try {
            parsed = slurper.parseText(cleaned)
        } catch (Exception firstFailure) {
            String normalized = normalizeJsonEscapes(cleaned)
            if (normalized == cleaned) {
                throw firstFailure
            }
            parsed = slurper.parseText(normalized)
        }
        if (!(parsed instanceof Map)) {
            throw new IllegalStateException("${roleName} returned JSON ${parsed?.getClass()?.simpleName ?: 'null'}, expected object".toString())
        }
        @SuppressWarnings('unchecked')
        Map<String, Object> typed = (Map<String, Object>) parsed
        typed
    }

    private static String collectStrictJsonText(LlmProvider provider, ProviderRequest request, String roleName) {
        StringBuilder out = new StringBuilder(2048)
        for (ProviderChunk chunk : Flux.from(provider.streamChat(request)).collectList().block() ?: []) {
            if (chunk.type == 'token' && chunk.content != null) {
                out.append(chunk.content)
            } else if (chunk.type == 'error') {
                throw new IllegalStateException(providerErrorMessage(roleName, chunk))
            }
        }
        stripCodeFence(out.toString().trim())
    }

    private static String providerErrorMessage(String roleName, ProviderChunk chunk) {
        String message = "${roleName} provider error: ${chunk?.content ?: 'unknown'}".toString()
        Map<String, Object> metadata = chunk?.metadata ?: Collections.<String, Object>emptyMap()
        Object partialOutput = metadata.get('partialOutput')
        Object partialOutputChars = metadata.get('partialOutputChars')
        if (partialOutput != null && String.valueOf(partialOutput).trim()) {
            message += "\nPartial provider output (${partialOutputChars ?: String.valueOf(partialOutput).length()} chars captured):\n${partialOutput}".toString()
        }
        message
    }

    private PlanAssessment normalizePlanAssessmentWithTransitionService(PlanAssessment assessment, AgentPlanRecord plan, List<AgentEvidenceRecord> evidence) {
        PlanAssessment source = assessment ?: new PlanAssessment(decision: 'continue')
        PlanDecisionTransition transition = transitionService.normalizePlanDecision(
            source.decision,
            plan,
            evidence,
            source.missing,
            source.summary,
            source.reason
        )
        new PlanAssessment(
            decision: transition.decision,
            summary: transition.summary,
            reason: transition.reason,
            question: source.question,
            missing: transition.missing,
            obsoleteStepIds: source.obsoleteStepIds
        )
    }

    private static String criteriaMarkdown(List<String> items) {
        if (!items) {
            return ''
        }
        StringBuilder builder = new StringBuilder('## Success Criteria\n')
        for (String item : items) {
            builder.append('- ').append(item ?: '').append('\n')
        }
        builder.toString().trim()
    }

    private static AgentEvidenceRecord evidenceFromToolResult(AgentRunRecord run, AgentStepRecord step, Map<String, Object> meta) {
        String tool = asString(getMap(meta.get('output')).get('tool')) ?: asString(meta.get('name')) ?: asString(meta.get('tool')) ?: 'tool'
        Object output = meta.get('output')
        String outputText = summarizeObject(output)
        boolean ok = Boolean.TRUE == meta.get('ok')
        String evidenceRole = ok ? 'tool_result' : 'tool_error'
        AgentEvidenceRecord evidence = new AgentEvidenceRecord(
            id: UUID.randomUUID().toString(),
            runId: run.id,
            stepId: step.id,
            sourceType: 'tool_result',
            sourceName: tool,
            sourceId: asString(meta.get('callId')),
            title: "Tool result ${tool}".toString(),
            summary: toolEvidenceSummary(tool, meta),
            content: outputText,
            structuredContent: new LinkedHashMap<String, Object>(meta ?: [:]),
            createdAt: ChatInMemoryStore.now(),
            metadata: [
                callId: meta.get('callId'),
                ok    : meta.get('ok'),
                evidenceRole: evidenceRole,
                pertinentToFinal: ok
            ] as Map<String, Object>
        )
        evidence
    }

    private static AgentGuidanceRecord guidanceFromToolResult(AgentRunRecord run, AgentStepRecord step, String toolName, String callId, boolean success, String modelText, boolean finalEvidence) {
        String guidanceText = modelText?.trim()
        if (guidanceText == null || guidanceText.trim().isEmpty()) {
            return null
        }
        new AgentGuidanceRecord(
            id: UUID.randomUUID().toString(),
            runId: run.id,
            stepId: step.id,
            sourceType: 'tool_guidance',
            sourceName: toolName,
            sourceId: callId,
            content: guidanceText.trim(),
            followForFinal: success && finalEvidence,
            createdAt: ChatInMemoryStore.now(),
            metadata: [
                callId: callId,
                toolName: toolName,
                ok: success,
                evidenceRole: 'guidance'
            ] as Map<String, Object>
        )
    }

    private static void linkGuidanceToEvidence(AgentEvidenceRecord evidence, AgentGuidanceRecord guidance) {
        if (evidence == null || guidance == null) {
            return
        }
        guidance.metadata.put('evidenceId', evidence.id)
        evidence.metadata.put('guidanceId', guidance.id)
        evidence.metadata.put('guidance', [
            id            : guidance.id,
            sourceName    : guidance.sourceName,
            sourceId      : guidance.sourceId,
            followForFinal: Boolean.TRUE == guidance.followForFinal,
            contentChars  : guidance.content == null ? 0 : guidance.content.length()
        ] as Map<String, Object>)
    }

    private static List<Map<String, Object>> guidanceLinks(List<AgentEvidenceRecord> evidence, List<AgentGuidanceRecord> guidance) {
        Map<String, AgentGuidanceRecord> guidanceById = new LinkedHashMap<String, AgentGuidanceRecord>()
        for (AgentGuidanceRecord item : guidance ?: ([] as List<AgentGuidanceRecord>)) {
            guidanceById.put(item.id, item)
        }
        List<Map<String, Object>> links = new ArrayList<Map<String, Object>>()
        for (AgentEvidenceRecord item : evidence ?: ([] as List<AgentEvidenceRecord>)) {
            String guidanceId = asString(item.metadata?.get('guidanceId'))
            AgentGuidanceRecord linked = guidanceId == null ? null : guidanceById.get(guidanceId)
            links.add([
                evidenceId    : item.id,
                evidenceSource: item.sourceName,
                guidanceId    : guidanceId,
                guidanceSource: linked?.sourceName,
                followForFinal: linked == null ? null : Boolean.TRUE == linked.followForFinal
            ] as Map<String, Object>)
        }
        links
    }

    private static String toolEvidenceSummary(String tool, Map<String, Object> meta) {
        Boolean ok = Boolean.TRUE == meta.get('ok')
        String error = asString(meta.get('error'))
        if (!ok && error != null && !error.trim().isEmpty()) {
            return "Tool ${tool} failed: ${error}".toString()
        }
        ok ? "Tool ${tool} completed.".toString() : "Tool ${tool} returned a result.".toString()
    }

    private static AgentEvidenceRecord evidenceFromExecutorOutput(AgentRunRecord run, AgentStepRecord step, String text) {
        new AgentEvidenceRecord(
            id: UUID.randomUUID().toString(),
            runId: run.id,
            stepId: step.id,
            sourceType: 'executor_output',
            sourceName: 'agent_executor',
            sourceId: step.id,
            title: "Executor output ${step.title ?: step.id}".toString(),
            summary: text,
            content: text,
            structuredContent: [
                stepId: step.id,
                stepTitle: step.title
            ] as Map<String, Object>,
            createdAt: ChatInMemoryStore.now(),
            metadata: [
                stepId: step.id,
                evidenceRole: 'final_candidate',
                pertinentToFinal: true
            ] as Map<String, Object>
        )
    }


    private static String summarizeObject(Object value) {
        if (value == null) {
            return ''
        }
        String json = JsonOutput.toJson(value)
        json.length() > 3000 ? json.substring(0, 3000) : json
    }

    private List<Map<String, Object>> providerTools() {
        chatMcpService.listServers()
            .collectMany {McpServerDto server -> server.tools ?: ([] as List<ToolSummaryDto>)}
            .collect {ToolSummaryDto tool -> [
                type: 'function',
                function: [
                    name: tool.name,
                    description: tool.description ?: '',
                    parameters: tool.inputSchema ?: [type: 'object']
                ] as Map<String, Object>,
                routing: tool.routing ?: [:]
            ] as Map<String, Object>}
    }

    private List<ChatPromptAssetDefinition> personaAssets() {
        promptAssetService == null ? [] : sortPromptAssets(promptAssetService.listAssetsByType('PERSONA') ?: [])
    }

    private List<ChatPromptAssetDefinition> lookupSkillAssetsForGoal(String goal) {
        if (promptAssetService == null) {
            return []
        }
        List<ChatPromptAssetDefinition> allSkills
        List<ChatPromptAssetDefinition> matches
        allSkills = (promptAssetService.listAssetsByType('SKILL') ?: []) as List<ChatPromptAssetDefinition>
        matches = (promptAssetService.searchAssets(goal ?: '') ?: [])
            .findAll {ChatPromptAssetDefinition asset -> isSkillAsset(asset)}
        List<ChatPromptAssetDefinition> requiredMatches = allSkills.findAll {ChatPromptAssetDefinition asset ->
            requiredApplicabilityMatches(asset, goal ?: '')
        } as List<ChatPromptAssetDefinition>
        if (matches.isEmpty()) {
            matches = allSkills
        }
        List<ChatPromptAssetDefinition> combined = []
        for (ChatPromptAssetDefinition asset : sortPromptAssets(requiredMatches + matches)) {
            if (!combined.any {ChatPromptAssetDefinition existing -> existing.id == asset.id}) {
                combined.add(asset)
            }
        }
        combined.take(6) as List<ChatPromptAssetDefinition>
    }

    private static List<Map<String, Object>> filterTools(List<Map<String, Object>> tools, List<String> allowedNames) {
        if (!allowedNames) {
            return []
        }
        Set<String> allowed = new LinkedHashSet<String>(allowedNames)
        tools.findAll {Map<String, Object> tool ->
            allowed.contains(asString(getMap(tool.get('function')).get('name')))
        } as List<Map<String, Object>>
    }

    private static List<String> toolNames(List<Map<String, Object>> tools) {
        List<String> names = []
        for (Map<String, Object> tool : tools ?: []) {
            String name = asString(getMap(tool.get('function')).get('name'))
            if (name != null && !name.trim().isEmpty()) {
                names.add(name)
            }
        }
        names.sort()
    }

    private static ChatEventDto agentEvent(String type, String messageId, String role, String content, boolean done, Map<String, Object> metadata) {
        new ChatEventDto(
            type: type,
            messageId: messageId,
            role: role,
            content: content ?: '',
            done: Boolean.valueOf(done),
            metadata: metadata ?: [:]
        )
    }

    private AgentOperationRecord startOperation(
        AgentRunRecord run,
        AgentPlanRecord plan,
        AgentStepRecord step,
        String role,
        String inputSummary,
        Integer attempt,
        Integer maxAttempts,
        List<MessageDto> timeline,
        FluxSink<ChatEventDto> sink,
        String sessionId,
        String messageId,
        Map<String, Object> metadata = [:]
    ) {
        AgentOperationRecord operation = new AgentOperationRecord(
            id: UUID.randomUUID().toString(),
            runId: run.id,
            planId: plan?.id,
            stepId: step?.id,
            parentOperationId: asString(metadata?.get('parentOperationId')),
            role: role,
            status: AgentStateTransitionService.OP_PENDING,
            attempt: attempt ?: 1,
            maxAttempts: maxAttempts ?: 1,
            inputSummary: inputSummary,
            startedAt: ChatInMemoryStore.now(),
            metadata: metadata ? new LinkedHashMap<String, Object>(metadata) : [:] as Map<String, Object>
        )
        store.agentOperations[operation.id] = operation
        transitionOperation(operation, AgentStateTransitionService.OP_IN_PROGRESS, 'operation_started', null, timeline, sink, sessionId, messageId)
        operation
    }

    private void transitionRun(
        AgentRunRecord run,
        String requestedStatus,
        String trigger,
        String reason,
        List<MessageDto> timeline,
        FluxSink<ChatEventDto> sink,
        String sessionId,
        String messageId,
        Map<String, Object> extraMetadata = [:]
    ) {
        String oldStatus = run.status
        TransitionAudit audit = transitionService.auditRunTransition(oldStatus, requestedStatus, trigger, reason)
        run.status = audit.normalizedStatus
        run.updatedAt = ChatInMemoryStore.now()
        store.agentRuns[run.id] = run
        Map<String, Object> metadata = transitionMetadata(run.id, null, null, oldStatus, requestedStatus, audit, trigger, reason, extraMetadata)
        emit(timeline, sink, sessionId, agentEvent('agent_status_changed', messageId, 'system', '', false, metadata))
    }

    private void transitionStep(
        AgentStepRecord step,
        String requestedStatus,
        String trigger,
        String reason,
        List<MessageDto> timeline,
        FluxSink<ChatEventDto> sink,
        String sessionId,
        String messageId,
        Map<String, Object> extraMetadata = [:]
    ) {
        String oldStatus = step.status
        TransitionAudit audit = transitionService.auditStepTransition(oldStatus, requestedStatus, trigger, reason)
        step.status = audit.normalizedStatus
        store.agentSteps[step.id] = step
        Map<String, Object> metadata = transitionMetadata(step.runId, step.planId, step.id, oldStatus, requestedStatus, audit, trigger, reason, extraMetadata)
        emit(timeline, sink, sessionId, agentEvent('agent_step_status_changed', messageId, 'system', '', false, metadata))
    }

    private void transitionPlan(
        AgentPlanRecord plan,
        String requestedStatus,
        String trigger,
        String reason,
        List<MessageDto> timeline,
        FluxSink<ChatEventDto> sink,
        String sessionId,
        String messageId,
        Map<String, Object> extraMetadata = [:]
    ) {
        String oldStatus = plan.status
        TransitionAudit audit = transitionService.auditPlanTransition(oldStatus, requestedStatus, trigger, reason)
        plan.status = audit.normalizedStatus
        store.agentPlans[plan.id] = plan
        Map<String, Object> metadata = transitionMetadata(plan.runId, plan.id, null, oldStatus, requestedStatus, audit, trigger, reason, extraMetadata)
        emit(timeline, sink, sessionId, agentEvent('agent_plan_status_changed', messageId, 'system', '', false, metadata))
    }

    private void skipStep(
        AgentRunRecord run,
        AgentPlanRecord plan,
        AgentStepRecord step,
        String trigger,
        String reason,
        List<MessageDto> timeline,
        FluxSink<ChatEventDto> sink,
        String sessionId,
        String messageId,
        Map<String, Object> extraMetadata = [:]
    ) {
        step.completedAt = ChatInMemoryStore.now()
        transitionStep(step, AgentStateTransitionService.STEP_SKIPPED, trigger, reason, timeline, sink, sessionId, messageId, extraMetadata)
        emit(timeline, sink, sessionId, agentEvent('agent_step_skipped', messageId, 'system', reason ?: 'Step skipped.', false, [
            runId: run.id,
            planId: plan.id,
            stepId: step.id,
            status: step.status,
            trigger: trigger,
            reason: reason,
            guard: step.guard,
            optional: step.optional
        ] as Map<String, Object>))
        emitProgress(timeline, sink, sessionId, messageId, 'step_skipped', "Skipped: ${shortText(step.title ?: reason ?: 'step')}".toString(), [
            runId: run.id,
            planId: plan.id,
            stepId: step.id,
            trigger: trigger,
            guard: step.guard,
            optional: step.optional
        ] as Map<String, Object>)
    }

    private void applyObsoleteStepSkips(
        AgentRunRecord run,
        AgentPlanRecord plan,
        PlanAssessment assessment,
        List<MessageDto> timeline,
        FluxSink<ChatEventDto> sink,
        String sessionId,
        String messageId
    ) {
        Set<String> obsoleteIds = new LinkedHashSet<String>(assessment?.obsoleteStepIds ?: [])
        if (!obsoleteIds) {
            return
        }
        for (AgentStepRecord step : plan.steps ?: ([] as List<AgentStepRecord>)) {
            if (obsoleteIds.contains(step.id) && isSkippableStep(step)) {
                skipStep(run, plan, step, 'step_obsolete', assessment.reason ?: assessment.summary ?: 'Step is no longer needed.', timeline, sink, sessionId, messageId, [
                    obsoleteStepIds: assessment.obsoleteStepIds
                ] as Map<String, Object>)
            }
        }
    }

    private static StepGuardDecision evaluateStepGuard(AgentPlanRecord plan, AgentStepRecord step, List<AgentEvidenceRecord> evidence) {
        String guard = normalizeStepGuard(step?.guard)
        if (guard == 'always') {
            return new StepGuardDecision(execute: true, reason: 'Guard always allows execution.')
        }
        if (guard == 'if_no_final_evidence') {
            boolean execute = AgentPromptService.finalAnswerEvidence(evidence).isEmpty()
            return new StepGuardDecision(
                execute: execute,
                reason: execute ? 'No final-answer evidence is available yet.' : 'Final-answer evidence is already available.'
            )
        }
        if (guard == 'if_no_successful_tool_evidence') {
            boolean execute = !(evidence ?: []).any {AgentEvidenceRecord item ->
                Boolean.TRUE == item.metadata?.get('ok') || item.metadata?.get('evidenceRole') == 'tool_result'
            }
            return new StepGuardDecision(
                execute: execute,
                reason: execute ? 'No successful tool evidence is available yet.' : 'Successful tool evidence is already available.'
            )
        }
        if (guard == 'if_previous_step_failed') {
            AgentStepRecord previous = previousStep(plan, step)
            boolean execute = previous != null && previous.status == AgentStateTransitionService.STEP_FAILED
            return new StepGuardDecision(
                execute: execute,
                reason: execute ? 'The previous step failed.' : 'The previous step did not fail.'
            )
        }
        return new StepGuardDecision(execute: true, reason: "Unsupported guard '${guard}' is treated as executable.".toString())
    }

    private static AgentStepRecord previousStep(AgentPlanRecord plan, AgentStepRecord step) {
        if (plan == null || step == null || step.ordinal == null) {
            return null
        }
        (plan.steps ?: ([] as List<AgentStepRecord>)).find {AgentStepRecord candidate ->
            candidate.ordinal != null && candidate.ordinal == step.ordinal - 1
        } as AgentStepRecord
    }

    private static boolean isPendingStep(AgentStepRecord step) {
        String status = step?.status ?: AgentStateTransitionService.STEP_PENDING
        status == AgentStateTransitionService.STEP_PENDING
    }

    private static boolean isSkippableStep(AgentStepRecord step) {
        String status = step?.status ?: AgentStateTransitionService.STEP_PENDING
        status in [AgentStateTransitionService.STEP_PENDING, AgentStateTransitionService.STEP_FAILED]
    }

    private static boolean isTerminalStep(AgentStepRecord step) {
        String status = step?.status ?: AgentStateTransitionService.STEP_PENDING
        status in [AgentStateTransitionService.STEP_COMPLETED, AgentStateTransitionService.STEP_SKIPPED, AgentStateTransitionService.STEP_CANCELLED]
    }

    private static boolean hasPendingOptionalRecoveryStep(AgentPlanRecord plan, AgentStepRecord failedStep) {
        if (plan == null || failedStep == null) {
            return false
        }
        (plan.steps ?: ([] as List<AgentStepRecord>)).any {AgentStepRecord candidate ->
            candidate.ordinal != null &&
                failedStep.ordinal != null &&
                candidate.ordinal > failedStep.ordinal &&
                Boolean.TRUE == candidate.optional &&
                isPendingStep(candidate)
        }
    }

    private static boolean nonRetryableToolFailure(List<AgentEvidenceRecord> currentStepEvidence) {
        (currentStepEvidence ?: ([] as List<AgentEvidenceRecord>)).any {AgentEvidenceRecord item ->
            nonRetryableToolFailure(item)
        }
    }

    private static boolean nonRetryableToolFailure(AgentEvidenceRecord evidence) {
        if (evidence == null || evidence.sourceType != 'tool_result') {
            return false
        }
        Integer statusCode = asInteger(findNestedValue(evidence.structuredContent, 'statusCode'))
        if (statusCode != null) {
            return statusCode >= 400 && statusCode < 500 && !(statusCode in [408, 409, 425, 429])
        }
        if (Boolean.FALSE == evidence.metadata?.get('ok')) {
            String error = asString(evidence.structuredContent?.get('error')) ?: asString(evidence.metadata?.get('error'))
            return deterministicToolError(error)
        }
        false
    }

    private static boolean deterministicToolError(String error) {
        if (error == null || error.trim().isEmpty()) {
            return false
        }
        String normalized = error.toLowerCase(Locale.ROOT)
        normalized.contains('unknown resource uri') ||
            normalized.contains('bad request') ||
            normalized.contains('uuid string too large') ||
            normalized.contains('failed to convert argument') ||
            normalized.contains('invalid argument')
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

    private static Map<String, Object> transitionMetadata(
        String runId,
        String planId,
        String stepId,
        String oldStatus,
        String requestedStatus,
        TransitionAudit audit,
        String trigger,
        String reason,
        Map<String, Object> extraMetadata
    ) {
        Map<String, Object> metadata = extraMetadata ? new LinkedHashMap<String, Object>(extraMetadata) : [:] as Map<String, Object>
        metadata.put('runId', runId)
        if (planId != null) {
            metadata.put('planId', planId)
        }
        if (stepId != null) {
            metadata.put('stepId', stepId)
        }
        metadata.put('oldStatus', oldStatus)
        metadata.put('requestedStatus', requestedStatus)
        metadata.put('status', audit.normalizedStatus)
        metadata.put('validTransition', Boolean.TRUE == audit.valid)
        metadata.put('legalNextStatuses', audit.legalNextStatuses)
        metadata.put('trigger', trigger)
        metadata.put('reason', reason)
        metadata.put('visibility', 'user')
        metadata
    }

    private void completeOperation(
        AgentOperationRecord operation,
        String outputSummary,
        List<MessageDto> timeline,
        FluxSink<ChatEventDto> sink,
        String sessionId,
        String messageId
    ) {
        operation.outputSummary = outputSummary
        operation.completedAt = ChatInMemoryStore.now()
        transitionOperation(operation, AgentStateTransitionService.OP_COMPLETED, 'operation_completed', null, timeline, sink, sessionId, messageId)
    }

    private void failOperation(
        AgentOperationRecord operation,
        Throwable failure,
        List<MessageDto> timeline,
        FluxSink<ChatEventDto> sink,
        String sessionId,
        String messageId
    ) {
        operation.error = failure?.message ?: failure?.class?.simpleName ?: 'Operation failed'
        operation.completedAt = ChatInMemoryStore.now()
        transitionOperation(operation, AgentStateTransitionService.OP_FAILED, 'operation_failed', operation.error, timeline, sink, sessionId, messageId)
    }

    private boolean retryOperationIfAvailable(
        AgentOperationRecord operation,
        String reason,
        List<MessageDto> timeline,
        FluxSink<ChatEventDto> sink,
        String sessionId,
        String messageId
    ) {
        if (providerContextLimitFailure(reason)) {
            return false
        }
        if (operation == null || (operation.attempt ?: 1) >= (operation.maxAttempts ?: 1)) {
            return false
        }
        transitionOperation(operation, AgentStateTransitionService.OP_RETRYING, 'operation_retrying', reason, timeline, sink, sessionId, messageId)
        operation.attempt = (operation.attempt ?: 1) + 1
        operation.completedAt = null
        operation.error = null
        operation.outputSummary = null
        transitionOperation(operation, AgentStateTransitionService.OP_IN_PROGRESS, 'operation_retry_started', reason, timeline, sink, sessionId, messageId)
        true
    }

    private static boolean providerContextLimitFailure(String reason) {
        String normalized = reason == null ? '' : reason.toLowerCase(Locale.ROOT)
        normalized.contains('context/output limit') ||
            normalized.contains('context limit') ||
            normalized.contains('output limit') ||
            normalized.contains('prompt_eval_count')
    }

    private void exhaustOperation(
        AgentOperationRecord operation,
        String reason,
        List<MessageDto> timeline,
        FluxSink<ChatEventDto> sink,
        String sessionId,
        String messageId
    ) {
        if (operation == null || operation.status == AgentStateTransitionService.OP_EXHAUSTED) {
            return
        }
        transitionOperation(operation, AgentStateTransitionService.OP_EXHAUSTED, 'operation_exhausted', reason, timeline, sink, sessionId, messageId)
    }

    private void transitionOperation(
        AgentOperationRecord operation,
        String requestedStatus,
        String trigger,
        String reason,
        List<MessageDto> timeline,
        FluxSink<ChatEventDto> sink,
        String sessionId,
        String messageId
    ) {
        String oldStatus = operation.status
        TransitionAudit audit = transitionService.auditOperationTransition(oldStatus, requestedStatus, trigger, reason)
        operation.status = audit.normalizedStatus
        store.agentOperations[operation.id] = operation
        emit(timeline, sink, sessionId, agentEvent("agent_operation_${operation.status}".toString(), messageId, 'system', operation.outputSummary ?: operation.error ?: operation.inputSummary ?: '', false, [
            runId: operation.runId,
            planId: operation.planId,
            stepId: operation.stepId,
            operationId: operation.id,
            roleName: operation.role,
            oldStatus: oldStatus,
            requestedStatus: requestedStatus,
            status: operation.status,
            validTransition: Boolean.TRUE == audit.valid,
            legalNextStatuses: audit.legalNextStatuses,
            trigger: trigger,
            reason: reason,
            attempt: operation.attempt,
            maxAttempts: operation.maxAttempts,
            visibility: 'debug'
        ] as Map<String, Object>))
    }

    private AgentOperationRecord startChildOperation(
        OperationTraceContext trace,
        String role,
        String inputSummary,
        Integer attempt,
        Integer maxAttempts,
        Map<String, Object> metadata = [:]
    ) {
        if (trace == null) {
            return null
        }
        Map<String, Object> childMetadata = metadata ? new LinkedHashMap<String, Object>(metadata) : [:] as Map<String, Object>
        childMetadata.put('parentOperationId', trace.parentOperation?.id)
        startOperation(trace.run, trace.plan, trace.step, role, inputSummary, attempt, maxAttempts, trace.timeline, trace.sink, trace.sessionId, trace.messageId, childMetadata)
    }

    private void completeChildOperation(AgentOperationRecord operation, String outputSummary) {
        if (operation == null) {
            return
        }
        OperationTraceContext trace = operationTrace.get()
        if (trace == null) {
            return
        }
        completeOperation(operation, outputSummary, trace.timeline, trace.sink, trace.sessionId, trace.messageId)
    }

    private void failChildOperation(AgentOperationRecord operation, Throwable failure) {
        if (operation == null) {
            return
        }
        OperationTraceContext trace = operationTrace.get()
        if (trace == null) {
            return
        }
        failOperation(operation, failure, trace.timeline, trace.sink, trace.sessionId, trace.messageId)
    }

    private void exhaustChildOperation(AgentOperationRecord operation, String reason) {
        if (operation == null) {
            return
        }
        OperationTraceContext trace = operationTrace.get()
        if (trace == null) {
            return
        }
        transitionOperation(operation, AgentStateTransitionService.OP_EXHAUSTED, 'operation_exhausted', reason, trace.timeline, trace.sink, trace.sessionId, trace.messageId)
    }

    private static void emitContextResolved(
        List<MessageDto> timeline,
        FluxSink<ChatEventDto> sink,
        String sessionId,
        String messageId,
        AgentContextRecord context,
        Integer replan
    ) {
        Map<String, Object> metadata = [
            runId: context.runId,
            contextId: context.id,
            version: context.version,
            followUpInterpretation: context.followUpInterpretation,
            goalFrame: context.goalFrame,
            domainContext: context.domainContext,
            relevantTools: context.relevantTools,
            recommendedSkills: context.recommendedSkills,
            relevantResources: context.relevantResources,
            instructions: context.instructions,
            resolvedReferences: context.resolvedReferences,
            resolvedResources: context.resolvedResources,
            priorEvidenceToReuse: context.priorEvidenceToReuse,
            priorGuidanceToFollow: context.priorGuidanceToFollow,
            contextRequests: context.contextRequests,
            planningHints: context.planningHints,
            constraints: context.constraints,
            personaSkills: context.metadata?.get('personaSkills') ?: [],
            skillLookupResults: context.metadata?.get('skillLookupResults') ?: []
        ] as Map<String, Object>
        if (replan != null) {
            metadata.put('replan', replan)
        }
        emit(timeline, sink, sessionId, agentEvent('agent_context_resolved', messageId, 'system', context.goalRestatement ?: '', false, metadata))
    }

    private static void emitContextDebug(
        List<MessageDto> timeline,
        FluxSink<ChatEventDto> sink,
        String sessionId,
        String messageId,
        AgentContextRecord context
    ) {
        Map<String, Object> raw = getMap(context.metadata?.get('raw'))
        String outputJson = raw ? JsonOutput.prettyPrint(JsonOutput.toJson(raw)) : ''
        String content = """System prompt:
${asString(context.metadata?.get('systemPrompt')) ?: ''}

User prompt:
${asString(context.metadata?.get('userPrompt')) ?: ''}

Strict JSON output:
${outputJson}""".toString()
        emit(timeline, sink, sessionId, agentEvent('agent_context_context', messageId, 'system', content, false, [
            runId: context.runId,
            contextId: context.id,
            version: context.version,
            systemPrompt: context.metadata?.get('systemPromptMetadata') ?: [:],
            userPrompt: context.metadata?.get('userPromptMetadata') ?: [:],
            visibility: 'debug',
            source: 'agent_context_resolver'
        ] as Map<String, Object>))
    }

    private static void emitProgress(
        List<MessageDto> timeline,
        FluxSink<ChatEventDto> sink,
        String sessionId,
        String messageId,
        String phase,
        String content,
        Map<String, Object> metadata
    ) {
        Map<String, Object> progressMetadata = metadata ? new LinkedHashMap<String, Object>(metadata) : [:] as Map<String, Object>
        progressMetadata.put('phase', phase)
        progressMetadata.put('visibility', 'user')
        emit(timeline, sink, sessionId, agentEvent('agent_progress', messageId, 'system', content ?: '', false, progressMetadata))
    }

    private static String progressStepStarted(AgentStepRecord step) {
        String title = step?.title ?: 'the next step'
        "Starting: ${shortText(title)}".toString()
    }

    private static String progressStepAssessed(StepAssessment assessment) {
        String text = assessment?.summary ?: assessment?.reason
        if (assessment?.completed) {
            return text ? "Completed: ${shortText(text)}".toString() : 'Step completed.'
        }
        text ? "That step did not complete: ${shortText(text)}".toString() : 'That step did not complete.'
    }

    private static String progressPlanAssessed(PlanAssessment assessment) {
        String decision = assessment?.decision ?: 'continue'
        if (decision == 'final') {
            return 'I have enough evidence to prepare the final answer.'
        }
        if (decision == 'continue') {
            return assessment?.summary ? shortText(assessment.summary) : 'Continuing with the next planned step.'
        }
        if (decision == 'replan') {
            return assessment?.reason ? "A plan change is needed: ${shortText(assessment.reason)}".toString() : 'A plan change is needed.'
        }
        if (decision == 'ask_user') {
            return shortText(assessment?.question ?: assessment?.reason ?: 'More information is required.')
        }
        if (decision == 'fail') {
            return assessment?.reason ? "The plan cannot continue: ${shortText(assessment.reason)}".toString() : 'The plan cannot continue.'
        }
        assessment?.summary ? shortText(assessment.summary) : 'Plan state updated.'
    }

    private static void emit(List<MessageDto> timeline, FluxSink<ChatEventDto> sink, String sessionId, ChatEventDto event) {
        appendChatEvent(timeline, sessionId, event)
        sink.next(event)
    }

    private static void appendChatEvent(List<MessageDto> timeline, String sessionId, ChatEventDto event) {
        if (timeline == null || event == null || event.type == null || event.type.trim().isEmpty()) {
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

    private static Map<String, Object> stepMetadata(AgentStepRecord step) {
        [
            id: step.id,
            ordinal: step.ordinal,
            title: step.title,
            objective: step.objective,
                    kind: step.kind,
                    status: step.status,
                    allowedTools: step.allowedTools,
                    guard: step.guard,
                    guardReason: step.guardReason,
                    optional: step.optional
        ] as Map<String, Object>
    }

    private static String stripCodeFence(String text) {
        String cleaned = text ?: ''
        if (cleaned.startsWith('```')) {
            cleaned = cleaned.replaceFirst(/(?s)^```[a-zA-Z0-9_-]*\s*/, '')
            cleaned = cleaned.replaceFirst(/(?s)\s*```\s*$/, '')
        }
        cleaned.trim()
    }

    private static String normalizeJsonEscapes(String text) {
        (text ?: '').replaceAll(/\\(?!["\\\/bfnrtu])/, '')
    }

    private static String snippet(String text) {
        String value = text ?: ''
        value.length() > 1000 ? value.substring(0, 1000) : value
    }

    private static String shortText(String text) {
        shortText(text, 180)
    }

    private static String shortText(String text, int maxLength) {
        String value = (text ?: '').replaceAll(/\s+/, ' ').trim()
        int limit = Math.max(maxLength, 4)
        if (value.length() <= limit) {
            return value
        }
        "${value.substring(0, limit - 3)}...".toString()
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

    private AgentSessionContinuityContext buildSessionContinuity(AgentRunRecord currentRun, List<MessageDto> timeline) {
        buildSessionContinuity(currentRun, timeline, 0)
    }

    private AgentSessionContinuityContext buildSessionContinuity(AgentRunRecord currentRun, List<MessageDto> timeline, int page) {
        AgentSessionContinuityContext continuity = new AgentSessionContinuityContext(
            sessionId: currentRun?.sessionId,
            currentRunId: currentRun?.id,
            createdAt: ChatInMemoryStore.now()
        )
        continuity.metadata.put('page', page)
        continuity.metadata.put('pageSize', CONTEXT_SIFT_PAGE_SIZE)
        if (currentRun == null || currentRun.sessionId == null) {
            return continuity
        }
        List<AgentRunRecord> priorRuns = store.agentRuns.values().findAll {AgentRunRecord run ->
            run.sessionId == currentRun.sessionId && run.id != currentRun.id
        } as List<AgentRunRecord>
        if (!priorRuns) {
            return continuity
        }
        priorRuns.sort {AgentRunRecord left, AgentRunRecord right ->
            (right.createdAt ?: Instant.EPOCH) <=> (left.createdAt ?: Instant.EPOCH)
        }
        int offset = Math.max(page, 0) * CONTEXT_SIFT_PAGE_SIZE
        List<AgentRunRecord> recentRuns = priorRuns.drop(offset).take(CONTEXT_SIFT_PAGE_SIZE)
        continuity.metadata.put('hasOlderPages', priorRuns.size() > offset + CONTEXT_SIFT_PAGE_SIZE)
        for (AgentRunRecord run : recentRuns) {
            AgentPlanRecord plan = run.currentPlanId == null ? null : store.agentPlans.get(run.currentPlanId)
            continuity.priorRuns.add([
                runId: run.id,
                status: run.status,
                goal: run.goal,
                planId: plan?.id,
                planGoal: plan?.goalRestatement,
                successCriteria: plan?.successCriteria ?: ([] as List<String>),
                finalAnswerSummary: priorFinalAnswerSummary(timeline, run)
            ] as Map<String, Object>)
        }
        Set<String> recentRunIds = recentRuns.collect {AgentRunRecord run -> run.id }.toSet()
        List<AgentEvidenceRecord> priorEvidence = store.agentEvidence.values().findAll {AgentEvidenceRecord item ->
            recentRunIds.contains(item.runId)
        } as List<AgentEvidenceRecord>
        priorEvidence.sort {AgentEvidenceRecord left, AgentEvidenceRecord right ->
            (right.createdAt ?: Instant.EPOCH) <=> (left.createdAt ?: Instant.EPOCH)
        }
        List<AgentGuidanceRecord> priorGuidance = store.agentGuidance.values().findAll {AgentGuidanceRecord item ->
            recentRunIds.contains(item.runId)
        } as List<AgentGuidanceRecord>
        priorGuidance.sort {AgentGuidanceRecord left, AgentGuidanceRecord right ->
            (right.createdAt ?: Instant.EPOCH) <=> (left.createdAt ?: Instant.EPOCH)
        }
        Map<String, AgentGuidanceRecord> guidanceByEvidenceId = new LinkedHashMap<String, AgentGuidanceRecord>()
        for (AgentGuidanceRecord item : priorGuidance.take(6)) {
            String evidenceId = asString(item.metadata?.get('evidenceId'))
            if (evidenceId != null && !evidenceId.trim().isEmpty()) {
                guidanceByEvidenceId.put(evidenceId, item)
            }
            continuity.linkedGuidance.add([
                guidanceId: item.id,
                evidenceId: evidenceId,
                runId: item.runId,
                sourceName: item.sourceName ?: item.sourceType,
                followForFinal: Boolean.TRUE == item.followForFinal,
                contentChars: item.content == null ? 0 : item.content.length()
            ] as Map<String, Object>)
        }
        for (AgentEvidenceRecord item : priorEvidence.take(6)) {
            Map<String, Object> evidenceSummary = continuityEvidenceSummary(item)
            continuity.reusableEvidence.add(evidenceSummary)
            Map<String, Object> pageState = pagingStateFromEvidence(item)
            if (pageState) {
                continuity.pagingState.add(pageState)
            }
        }
        continuity.openLimitations.addAll(openLimitationsFromContinuity(continuity))
        continuity
    }

    private static String renderSessionContinuity(AgentSessionContinuityContext continuity) {
        if (continuity == null || !continuity.hasContent()) {
            return ''
        }
        StringBuilder builder = new StringBuilder(3072)
        builder.append('Session continuity page ')
            .append(continuity.metadata?.get('page') ?: 0)
            .append(' (newest-to-oldest sift, pageSize=')
            .append(continuity.metadata?.get('pageSize') ?: CONTEXT_SIFT_PAGE_SIZE)
            .append(', hasOlderPages=')
            .append(continuity.metadata?.get('hasOlderPages') ?: false)
            .append('):\n')
        builder.append('Recent prior agent runs in this page:\n')
        for (Map<String, Object> run : continuity.priorRuns) {
            builder.append('- runId=').append(asString(run.get('runId')) ?: '')
                .append(' status=').append(asString(run.get('status')) ?: '')
                .append(' goal=').append(shortText(asString(run.get('goal')) ?: '', 180))
                .append('\n')
            if (run.get('planGoal') != null) {
                builder.append('  planGoal=').append(shortText(asString(run.get('planGoal')) ?: '', 180)).append('\n')
            }
            Object criteria = run.get('successCriteria')
            if (criteria instanceof Collection && !((Collection<?>) criteria).isEmpty()) {
                builder.append('  successCriteria=').append(shortText(((Collection<?>) criteria).join(' | '), 260)).append('\n')
            }
            if (run.get('finalAnswerSummary') != null) {
                builder.append('  finalAnswerSummary=').append(shortText(asString(run.get('finalAnswerSummary')) ?: '', 180)).append('\n')
            }
        }
        if (continuity.reusableEvidence) {
            builder.append('\nReusable prior evidence:\n')
            for (Map<String, Object> item : continuity.reusableEvidence) {
                builder.append('- evidenceId=').append(asString(item.get('evidenceId')) ?: '')
                    .append(' runId=').append(asString(item.get('runId')) ?: '')
                    .append(' source=').append(asString(item.get('sourceName')) ?: '')
                    .append(' title=').append(shortText(asString(item.get('title')) ?: '', 120))
                    .append('\n')
                if (item.get('summary') != null) {
                    builder.append('  summary=').append(shortText(asString(item.get('summary')) ?: '', 160)).append('\n')
                }
                if (item.get('guidanceId') != null) {
                    builder.append('  guidanceId=').append(asString(item.get('guidanceId'))).append('\n')
                }
                if (item.get('arguments') != null) {
                    builder.append('  arguments=').append(shortText(JsonOutput.toJson(item.get('arguments')), 180)).append('\n')
                }
                if (item.get('nextArguments') != null) {
                    builder.append('  nextArguments=').append(shortText(JsonOutput.toJson(item.get('nextArguments')), 260)).append('\n')
                }
                if (item.get('resourceRefs') != null) {
                    builder.append(renderResourceRefs(asMapList(item.get('resourceRefs')), 12))
                }
            }
        }
        if (continuity.pagingState) {
            builder.append('\nPrior paging state:\n')
            for (Map<String, Object> item : continuity.pagingState) {
                builder.append('- evidenceId=').append(asString(item.get('evidenceId')) ?: '')
                    .append(' tool=').append(asString(item.get('toolName')) ?: '')
                    .append(' hasMore=').append(item.get('hasMore'))
                    .append(' nextOffset=').append(item.get('nextOffset'))
                    .append(' total=').append(item.get('total'))
                    .append('\n')
                if (item.get('nextArguments') != null) {
                    builder.append('  nextArguments=').append(shortText(JsonOutput.toJson(item.get('nextArguments')), 260)).append('\n')
                }
            }
        }
        if (continuity.linkedGuidance) {
            builder.append('\nLinked prior tool guidance:\n')
            for (Map<String, Object> item : continuity.linkedGuidance) {
                builder.append('- guidanceId=').append(asString(item.get('guidanceId')) ?: '')
                    .append(' evidenceId=').append(asString(item.get('evidenceId')) ?: '')
                    .append(' source=').append(asString(item.get('sourceName')) ?: '')
                    .append(' followForFinal=').append(item.get('followForFinal'))
                    .append(' contentChars=').append(item.get('contentChars') ?: 0)
                    .append('\n')
            }
        }
        if (continuity.openLimitations) {
            builder.append('\nOpen limitations/caveats from prior context:\n')
            for (String item : continuity.openLimitations) {
                builder.append('- ').append(item).append('\n')
            }
        }
        shortText(builder.toString().trim(), 2200)
    }

    private static String renderResourceRefs(List<Map<String, Object>> refs, int maxRefs) {
        if (!refs) {
            return ''
        }
        StringBuilder builder = new StringBuilder(1024)
        List<Map<String, Object>> visible = refs.take(maxRefs)
        builder.append('  resourceRefs:\n')
        for (Map<String, Object> ref : visible) {
            builder.append('  - label=').append(shortText(asString(ref.get('label')) ?: '', 90))
                .append(' type=').append(asString(ref.get('domainType')) ?: '')
                .append(' id=').append(asString(ref.get('id')) ?: '')
            String uri = asString(ref.get('uri')) ?: asString(ref.get('href'))
            if (uri != null && !uri.trim().isEmpty()) {
                builder.append(' uri=').append(shortText(uri, 140))
            }
            String evidenceId = asString(ref.get('evidenceId'))
            if (evidenceId != null && !evidenceId.trim().isEmpty()) {
                builder.append(' evidenceId=').append(evidenceId)
            }
            builder.append('\n')
        }
        int omitted = refs.size() - visible.size()
        if (omitted > 0) {
            builder.append('  - ... ').append(omitted).append(' more resource refs omitted from perceiver prompt; select evidenceId to reuse if needed.\n')
        }
        builder.toString()
    }

    private static void emitSessionContinuityDebug(
        List<MessageDto> timeline,
        FluxSink<ChatEventDto> sink,
        String sessionId,
        String messageId,
        AgentRunRecord run,
        AgentSessionContinuityContext sessionContinuity
    ) {
        String rendered = renderSessionContinuity(sessionContinuity)
        emit(timeline, sink, sessionId, agentEvent('agent_context_continuity', messageId, 'system', rendered, false, [
            runId: run?.id,
            visibility: 'debug',
            source: 'agent_session_continuity',
            contentChars: rendered.length(),
            continuity: sessionContinuityToMap(sessionContinuity)
        ] as Map<String, Object>))
    }

    private static Map<String, Object> sessionContinuityToMap(AgentSessionContinuityContext continuity) {
        if (continuity == null) {
            return [:] as Map<String, Object>
        }
        [
            sessionId: continuity.sessionId,
            currentRunId: continuity.currentRunId,
            priorRuns: continuity.priorRuns,
            reusableEvidence: continuity.reusableEvidence,
            linkedGuidance: continuity.linkedGuidance,
            pagingState: continuity.pagingState,
            openLimitations: continuity.openLimitations,
            metadata: continuity.metadata,
            createdAt: continuity.createdAt?.toString()
        ] as Map<String, Object>
    }

    private void seedPriorContextSelections(
        AgentContextRecord context,
        List<AgentEvidenceRecord> evidence,
        List<AgentGuidanceRecord> guidance
    ) {
        if (context == null) {
            return
        }
        Set<String> evidenceIds = new LinkedHashSet<String>()
        for (Map<String, Object> item : context.priorEvidenceToReuse ?: ([] as List<Map<String, Object>>)) {
            String id = asString(item.get('evidenceId'))
            if (id != null && !id.trim().isEmpty()) {
                evidenceIds.add(id)
            }
        }
        for (Map<String, Object> item : context.resolvedReferences ?: ([] as List<Map<String, Object>>)) {
            String id = asString(item.get('evidenceId'))
            if (id != null && !id.trim().isEmpty()) {
                evidenceIds.add(id)
            }
        }
        for (Map<String, Object> item : context.resolvedResources ?: ([] as List<Map<String, Object>>)) {
            String id = asString(item.get('evidenceId'))
            if (id != null && !id.trim().isEmpty()) {
                evidenceIds.add(id)
            }
        }
        Set<String> existingEvidenceIds = (evidence ?: ([] as List<AgentEvidenceRecord>)).collect {AgentEvidenceRecord item -> item.id }.toSet()
        for (String id : evidenceIds) {
            AgentEvidenceRecord selected = store.agentEvidence.get(id)
            if (selected != null && !existingEvidenceIds.contains(selected.id)) {
                evidence.add(selected)
                existingEvidenceIds.add(selected.id)
            }
        }

        Set<String> guidanceIds = new LinkedHashSet<String>()
        for (Map<String, Object> item : context.priorGuidanceToFollow ?: ([] as List<Map<String, Object>>)) {
            String id = asString(item.get('guidanceId'))
            if (id != null && !id.trim().isEmpty()) {
                guidanceIds.add(id)
            }
        }
        for (AgentEvidenceRecord item : evidence ?: ([] as List<AgentEvidenceRecord>)) {
            String guidanceId = asString(item.metadata?.get('guidanceId'))
            if (guidanceId != null && !guidanceId.trim().isEmpty()) {
                guidanceIds.add(guidanceId)
            }
        }
        Set<String> existingGuidanceIds = (guidance ?: ([] as List<AgentGuidanceRecord>)).collect {AgentGuidanceRecord item -> item.id }.toSet()
        for (String id : guidanceIds) {
            AgentGuidanceRecord selected = store.agentGuidance.get(id)
            if (selected != null && !existingGuidanceIds.contains(selected.id)) {
                guidance.add(selected)
                existingGuidanceIds.add(selected.id)
            }
        }
    }

    private static Map<String, Object> continuityEvidenceSummary(AgentEvidenceRecord item) {
        Map<String, Object> arguments = evidenceArguments(item)
        [
            evidenceId: item.id,
            runId: item.runId,
            stepId: item.stepId,
            sourceName: item.sourceName ?: item.sourceType,
            sourceId: item.sourceId,
            title: item.title,
            summary: shortText(item.summary ?: '', 500),
            guidanceId: asString(item.metadata?.get('guidanceId')),
            arguments: arguments,
            nextArguments: nextArguments(arguments, findNestedValue(item.structuredContent?.get('output'), 'nextOffset'), findNestedValue(item.structuredContent?.get('output'), 'max')),
            resourceRefs: resourceRefsFromEvidence(item)
        ] as Map<String, Object>
    }

    private static Map<String, Object> evidenceArguments(AgentEvidenceRecord item) {
        Object args = item.metadata?.get('arguments')
        if (args instanceof Map) {
            return new LinkedHashMap<String, Object>(getMap(args))
        }
        Object output = item.structuredContent?.get('output')
        if (output instanceof Map) {
            Object outputArgs = getMap(output).get('arguments')
            if (outputArgs instanceof Map) {
                return new LinkedHashMap<String, Object>(getMap(outputArgs))
            }
        }
        [:] as Map<String, Object>
    }

    private static List<Map<String, Object>> resourceRefsFromEvidence(AgentEvidenceRecord item) {
        List<Map<String, Object>> refs = []
        Object output = item.structuredContent?.get('output')
        Object items = findNestedValue(output, 'items')
        if (items instanceof Collection) {
            for (Object candidate : ((Collection<?>) items).take(10)) {
                if (candidate instanceof Map) {
                    Map<String, Object> map = getMap(candidate)
                    refs.add([
                        evidenceId: item.id,
                        label: asString(map.get('label')) ?: asString(map.get('name')) ?: asString(map.get('title')),
                        id: asString(map.get('id')),
                        domainType: asString(map.get('domainType')) ?: asString(map.get('type')),
                        uri: asString(map.get('uri')) ?: asString(map.get('resourceUri')) ?: asString(map.get('readUri')) ?: asString(map.get('detailUri')),
                        href: asString(map.get('href')) ?: asString(map.get('path'))
                    ] as Map<String, Object>)
                }
            }
        }
        refs.findAll {Map<String, Object> ref ->
            ref.values().any {Object value -> value != null && !value.toString().trim().isEmpty() }
        } as List<Map<String, Object>>
    }

    private static Map<String, Object> pagingStateFromEvidence(AgentEvidenceRecord item) {
        Object output = item.structuredContent?.get('output')
        Object hasMore = findNestedValue(output, 'hasMore')
        Object nextOffset = findNestedValue(output, 'nextOffset')
        if (hasMore == null && nextOffset == null) {
            return [:] as Map<String, Object>
        }
        Map<String, Object> arguments = evidenceArguments(item)
        Object max = findNestedValue(output, 'max')
        [
            evidenceId: item.id,
            runId: item.runId,
            toolName: item.sourceName,
            arguments: arguments,
            nextArguments: nextArguments(arguments, nextOffset, max),
            hasMore: hasMore,
            nextOffset: nextOffset,
            total: findNestedValue(output, 'total') ?: findNestedValue(output, 'count'),
            max: max,
            offset: findNestedValue(output, 'offset')
        ] as Map<String, Object>
    }

    private static Map<String, Object> nextArguments(Map<String, Object> arguments, Object nextOffset, Object max) {
        Map<String, Object> out = new LinkedHashMap<String, Object>(arguments ?: ([:] as Map<String, Object>))
        if (nextOffset != null) {
            out.put('offset', nextOffset)
        }
        if (max != null && !out.containsKey('max')) {
            out.put('max', max)
        }
        out
    }

    private static Object findNestedValue(Object value, String key) {
        if (value instanceof Map) {
            Map<String, Object> map = getMap(value)
            if (map.containsKey(key)) {
                return map.get(key)
            }
            for (Object nested : map.values()) {
                Object found = findNestedValue(nested, key)
                if (found != null) {
                    return found
                }
            }
        } else if (value instanceof Collection) {
            for (Object nested : (Collection<?>) value) {
                Object found = findNestedValue(nested, key)
                if (found != null) {
                    return found
                }
            }
        }
        null
    }

    private static List<String> openLimitationsFromContinuity(AgentSessionContinuityContext continuity) {
        List<String> limitations = []
        if ((continuity?.pagingState ?: ([] as List<Map<String, Object>>)).any {Map<String, Object> page -> page.get('hasMore') == true || page.get('nextOffset') != null }) {
            limitations.add('A prior search reported more paged results are available.')
        }
        limitations
    }

    private static String priorFinalAnswerSummary(List<MessageDto> timeline, AgentRunRecord run) {
        if (timeline == null || run == null || run.messageId == null) {
            return ''
        }
        String text = timeline.findAll {MessageDto message ->
            message.metadata?.get('messageId') == run.messageId &&
                message.metadata?.get('eventType') == 'token' &&
                message.role == 'assistant' &&
                message.content != null &&
                !message.content.trim().isEmpty()
        }.collect {MessageDto message -> message.content ?: '' }.join('')
        shortText(text, 500)
    }

    private static Map<String, Object> getMap(Object value) {
        if (!(value instanceof Map)) {
            return [:] as Map<String, Object>
        }
        @SuppressWarnings('unchecked')
        Map<String, Object> typed = (Map<String, Object>) value
        typed
    }

    private static List<String> asStringList(Object value) {
        if (value == null) {
            return []
        }
        List<String> out = []
        if (value instanceof Collection) {
            for (Object item : (Collection<?>) value) {
                String text = asString(item)
                if (text != null && !text.trim().isEmpty()) {
                    out.add(text)
                }
            }
        } else {
            String text = asString(value)
            if (text != null && !text.trim().isEmpty()) {
                out.add(text)
            }
        }
        out
    }

    private static List<Map<String, Object>> asMapList(Object value) {
        if (!(value instanceof Collection)) {
            return [] as List<Map<String, Object>>
        }
        List<Map<String, Object>> out = []
        for (Object item : (Collection<?>) value) {
            if (item instanceof Map) {
                out.add(new LinkedHashMap<String, Object>(getMap(item)))
            }
        }
        out
    }

    private static List<Map<String, Object>> skillSummaries(List<ChatPromptAssetDefinition> skills) {
        List<Map<String, Object>> out = []
        for (ChatPromptAssetDefinition skill : sortPromptAssets(skills)) {
            out.add([
                id: skill.id,
                name: skill.name,
                description: skill.description,
                type: skill.type,
                priority: skill.priority,
                keywords: skill.keywords ?: [],
                seeAlso: skill.seeAlso ?: []
            ] as Map<String, Object>)
        }
        out
    }

    private static List<ChatPromptAssetDefinition> sortPromptAssets(List<ChatPromptAssetDefinition> assets) {
        new ArrayList<ChatPromptAssetDefinition>(assets ?: [])
            .sort {ChatPromptAssetDefinition left, ChatPromptAssetDefinition right ->
                Integer leftPriority = left.priority != null ? left.priority : Integer.valueOf(1000)
                Integer rightPriority = right.priority != null ? right.priority : Integer.valueOf(1000)
                int priorityCompare = leftPriority <=> rightPriority
                priorityCompare != 0 ? priorityCompare : (left.id ?: '') <=> (right.id ?: '')
            } as List<ChatPromptAssetDefinition>
    }

    private static boolean isSkillAsset(ChatPromptAssetDefinition asset) {
        asset != null && 'SKILL'.equalsIgnoreCase(asset.type)
    }

    private static boolean requiredApplicabilityMatches(ChatPromptAssetDefinition asset, String userContent) {
        for (SkillToolApplicability applicability : (asset?.toolApplicability ?: ([] as List<SkillToolApplicability>))) {
            if (applicability == null || !isRequiredPrerequisite(applicability.relationship)) {
                continue
            }
            List<String> triggerTerms = applicability.triggerTerms ?: []
            if (triggerTerms.isEmpty()) {
                triggerTerms = asset.keywords ?: []
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

    private static String asString(Object value) {
        value == null ? null : String.valueOf(value)
    }

    private static int asInteger(Object value, int fallback) {
        if (value instanceof Number) {
            return ((Number) value).intValue()
        }
        if (value == null) {
            return fallback
        }
        try {
            return Integer.parseInt(String.valueOf(value))
        } catch (NumberFormatException ignored) {
            return fallback
        }
    }

    private static boolean asBoolean(Object value, boolean fallback) {
        if (value instanceof Boolean) {
            return Boolean.TRUE == value
        }
        if (value == null) {
            return fallback
        }
        String text = String.valueOf(value).trim().toLowerCase()
        if (text in ['true', 'yes', '1']) {
            return true
        }
        if (text in ['false', 'no', '0']) {
            return false
        }
        fallback
    }

    private static String normalizeStepGuard(String guard) {
        String value = (guard ?: 'always').trim()
        value.isEmpty() ? 'always' : value
    }

    @CompileStatic
    private static class StepExecution {
        String summary
        List<AgentEvidenceRecord> evidence = []
        List<AgentGuidanceRecord> guidance = []
        Integer toolCalls = 0
    }

    @CompileStatic
    private static class StepAssessment {
        Boolean completed = false
        String decision = 'continue'
        String summary
        String reason
        String question
    }

    @CompileStatic
    private static class PlanAssessment {
        String decision = 'continue'
        String summary
        String reason
        String question
        List<String> missing = []
        List<String> obsoleteStepIds = []
    }

    @CompileStatic
    private static class StepGuardDecision {
        Boolean execute = true
        String reason
    }

    @CompileStatic
    private static class OperationTraceContext {
        AgentRunRecord run
        AgentPlanRecord plan
        AgentStepRecord step
        AgentOperationRecord parentOperation
        List<MessageDto> timeline
        FluxSink<ChatEventDto> sink
        String sessionId
        String messageId
    }

    @CompileStatic
    private static class StrictJsonParseException extends RuntimeException {
        final String roleName
        final String parseError
        final String rawOutputSnippet
        final String repairParseError
        final String repairOutputSnippet

        StrictJsonParseException(
            String message,
            String roleName,
            String parseError,
            String rawOutputSnippet,
            String repairParseError,
            String repairOutputSnippet
        ) {
            super(message)
            this.roleName = roleName
            this.parseError = parseError
            this.rawOutputSnippet = rawOutputSnippet
            this.repairParseError = repairParseError
            this.repairOutputSnippet = repairOutputSnippet
        }
    }
}

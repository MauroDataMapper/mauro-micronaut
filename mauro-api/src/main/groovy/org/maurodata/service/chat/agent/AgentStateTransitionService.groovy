package org.maurodata.service.chat.agent

import groovy.transform.CompileStatic
import jakarta.inject.Singleton

@CompileStatic
@Singleton
class AgentStateTransitionService {

    static final String RUN_QUEUED = 'queued'
    static final String RUN_IN_PROGRESS = 'in_progress'
    static final String RUN_REQUIRES_ACTION = 'requires_action'
    static final String RUN_CANCELLING = 'cancelling'
    static final String RUN_CANCELLED = 'cancelled'
    static final String RUN_FAILED = 'failed'
    static final String RUN_COMPLETED = 'completed'
    static final String RUN_EXPIRED = 'expired'

    static final String STEP_PENDING = 'pending'
    static final String STEP_IN_PROGRESS = 'in_progress'
    static final String STEP_REQUIRES_ACTION = 'requires_action'
    static final String STEP_COMPLETED = 'completed'
    static final String STEP_FAILED = 'failed'
    static final String STEP_SKIPPED = 'skipped'
    static final String STEP_CANCELLED = 'cancelled'

    static final String PLAN_ACTIVE = 'active'
    static final String PLAN_SUPERSEDED = 'superseded'
    static final String PLAN_COMPLETE = 'complete'
    static final String PLAN_BLOCKED = 'blocked'
    static final String PLAN_FAILED = 'failed'

    static final String OP_PENDING = 'pending'
    static final String OP_IN_PROGRESS = 'in_progress'
    static final String OP_COMPLETED = 'completed'
    static final String OP_FAILED = 'failed'
    static final String OP_RETRYING = 'retrying'
    static final String OP_EXHAUSTED = 'exhausted'
    static final String OP_CANCELLED = 'cancelled'
    static final String OP_SKIPPED = 'skipped'

    private static final Map<String, Set<String>> RUN_TRANSITIONS = [
        (RUN_QUEUED)         : [RUN_IN_PROGRESS, RUN_CANCELLED, RUN_FAILED, RUN_EXPIRED] as Set<String>,
        (RUN_IN_PROGRESS)    : [RUN_REQUIRES_ACTION, RUN_CANCELLING, RUN_CANCELLED, RUN_FAILED, RUN_COMPLETED, RUN_EXPIRED] as Set<String>,
        (RUN_REQUIRES_ACTION): [RUN_IN_PROGRESS, RUN_CANCELLING, RUN_CANCELLED, RUN_FAILED, RUN_EXPIRED] as Set<String>,
        (RUN_CANCELLING)    : [RUN_CANCELLED, RUN_FAILED] as Set<String>,
        (RUN_CANCELLED)     : [] as Set<String>,
        (RUN_FAILED)        : [] as Set<String>,
        (RUN_COMPLETED)     : [] as Set<String>,
        (RUN_EXPIRED)       : [] as Set<String>
    ] as Map<String, Set<String>>

    private static final Map<String, Set<String>> STEP_TRANSITIONS = [
        (STEP_PENDING)        : [STEP_IN_PROGRESS, STEP_SKIPPED, STEP_CANCELLED, STEP_FAILED] as Set<String>,
        (STEP_IN_PROGRESS)    : [STEP_COMPLETED, STEP_FAILED, STEP_REQUIRES_ACTION, STEP_CANCELLED] as Set<String>,
        (STEP_REQUIRES_ACTION): [STEP_IN_PROGRESS, STEP_FAILED, STEP_CANCELLED] as Set<String>,
        (STEP_FAILED)         : [STEP_PENDING, STEP_SKIPPED, STEP_CANCELLED] as Set<String>,
        (STEP_COMPLETED)      : [] as Set<String>,
        (STEP_SKIPPED)        : [] as Set<String>,
        (STEP_CANCELLED)      : [] as Set<String>
    ] as Map<String, Set<String>>

    private static final Map<String, Set<String>> PLAN_TRANSITIONS = [
        (PLAN_ACTIVE)    : [PLAN_SUPERSEDED, PLAN_COMPLETE, PLAN_BLOCKED, PLAN_FAILED] as Set<String>,
        (PLAN_BLOCKED)   : [PLAN_ACTIVE, PLAN_SUPERSEDED, PLAN_FAILED] as Set<String>,
        (PLAN_SUPERSEDED): [] as Set<String>,
        (PLAN_COMPLETE)  : [] as Set<String>,
        (PLAN_FAILED)    : [] as Set<String>
    ] as Map<String, Set<String>>

    private static final Map<String, Set<String>> OPERATION_TRANSITIONS = [
        (OP_PENDING)    : [OP_IN_PROGRESS, OP_SKIPPED, OP_CANCELLED] as Set<String>,
        (OP_IN_PROGRESS): [OP_COMPLETED, OP_FAILED, OP_RETRYING, OP_CANCELLED] as Set<String>,
        (OP_FAILED)     : [OP_RETRYING, OP_EXHAUSTED] as Set<String>,
        (OP_RETRYING)   : [OP_IN_PROGRESS, OP_EXHAUSTED, OP_CANCELLED] as Set<String>,
        (OP_COMPLETED)  : [] as Set<String>,
        (OP_EXHAUSTED)  : [] as Set<String>,
        (OP_CANCELLED)  : [] as Set<String>,
        (OP_SKIPPED)    : [] as Set<String>
    ] as Map<String, Set<String>>

    TransitionAudit auditRunTransition(String currentStatus, String requestedStatus, String trigger = null, String reason = null) {
        audit('run', normalizeStatus(currentStatus, RUN_QUEUED), requestedStatus, RUN_TRANSITIONS, trigger, reason)
    }

    TransitionAudit auditStepTransition(String currentStatus, String requestedStatus, String trigger = null, String reason = null) {
        audit('step', normalizeStatus(currentStatus, STEP_PENDING), requestedStatus, STEP_TRANSITIONS, trigger, reason)
    }

    TransitionAudit auditPlanTransition(String currentStatus, String requestedStatus, String trigger = null, String reason = null) {
        audit('plan', normalizeStatus(currentStatus, PLAN_ACTIVE), requestedStatus, PLAN_TRANSITIONS, trigger, reason)
    }

    TransitionAudit auditOperationTransition(String currentStatus, String requestedStatus, String trigger = null, String reason = null) {
        audit('operation', normalizeStatus(currentStatus, OP_PENDING), requestedStatus, OPERATION_TRANSITIONS, trigger, reason)
    }

    Set<String> legalRunNextStatuses(String currentStatus) {
        legalNext(normalizeStatus(currentStatus, RUN_QUEUED), RUN_TRANSITIONS)
    }

    Set<String> legalStepNextStatuses(String currentStatus) {
        legalNext(normalizeStatus(currentStatus, STEP_PENDING), STEP_TRANSITIONS)
    }

    Set<String> legalPlanNextStatuses(String currentStatus) {
        legalNext(normalizeStatus(currentStatus, PLAN_ACTIVE), PLAN_TRANSITIONS)
    }

    Set<String> legalOperationNextStatuses(String currentStatus) {
        legalNext(normalizeStatus(currentStatus, OP_PENDING), OPERATION_TRANSITIONS)
    }

    StepDecisionTransition normalizeStepDecision(String decision, Boolean stepComplete, Integer attemptCount = 0, Integer maxRetries = 0) {
        String normalizedDecision = normalizeDecision(decision, 'continue')
        boolean completed = stepComplete == null ? normalizedDecision == 'continue' : Boolean.TRUE == stepComplete
        if (normalizedDecision == 'continue') {
            completed = true
        } else if (normalizedDecision in ['retry', 'replan', 'ask_user', 'fail']) {
            completed = false
        }

        String stepStatus
        String runStatus = null
        boolean retryAllowed = false
        if (completed) {
            stepStatus = STEP_COMPLETED
        } else if (normalizedDecision == 'ask_user') {
            stepStatus = STEP_REQUIRES_ACTION
            runStatus = RUN_REQUIRES_ACTION
        } else if (normalizedDecision == 'retry') {
            retryAllowed = (attemptCount ?: 0) < (maxRetries ?: 0)
            stepStatus = retryAllowed ? STEP_PENDING : STEP_FAILED
        } else {
            stepStatus = STEP_FAILED
            if (normalizedDecision == 'fail') {
                runStatus = RUN_FAILED
            }
        }

        new StepDecisionTransition(
            requestedDecision: decision,
            decision: normalizedDecision,
            stepComplete: completed,
            targetStepStatus: stepStatus,
            targetRunStatus: runStatus,
            retryAllowed: retryAllowed,
            reason: stepDecisionReason(normalizedDecision, completed, retryAllowed)
        )
    }

    PlanDecisionTransition normalizePlanDecision(
        String decision,
        AgentPlanRecord plan,
        List<AgentEvidenceRecord> evidence,
        List<String> missing = [],
        String summary = null,
        String reason = null
    ) {
        String normalizedDecision = normalizeDecision(decision, 'continue')
        List<String> normalizedMissing = new ArrayList<String>(missing ?: ([] as List<String>))
        String normalizedSummary = summary
        String normalizedReason = reason

        if (normalizedDecision == 'continue' && !hasRemainingPlannedSteps(plan)) {
            normalizedDecision = 'final'
            normalizedSummary = 'No remaining planned steps are available; preparing the final answer from the evidence gathered.'
            normalizedReason = 'The evaluator requested continuation, but the current plan has no remaining pending steps. The transition service normalized the decision to final.'
        }

        if (normalizedDecision == 'replan' && !hasRemainingPlannedSteps(plan) && finalAnswerEvidencePresent(evidence)) {
            List<String> declaredMissing = unmetDeclaredSuccessCriteria(normalizedMissing, plan)
            if (declaredMissing.isEmpty()) {
                normalizedDecision = 'final'
                normalizedSummary = 'No declared success criteria remain unmet; preparing the final answer from the evidence gathered.'
                normalizedReason = 'The evaluator requested replanning after all planned steps completed, but did not identify an unmet active-plan success criterion.'
                normalizedMissing = []
            } else {
                normalizedMissing = declaredMissing
            }
        }

        String runStatus = null
        String planStatus = null
        if (normalizedDecision == 'final') {
            planStatus = PLAN_COMPLETE
        } else if (normalizedDecision == 'ask_user') {
            runStatus = RUN_REQUIRES_ACTION
            planStatus = PLAN_BLOCKED
        } else if (normalizedDecision == 'replan') {
            planStatus = PLAN_SUPERSEDED
        } else if (normalizedDecision == 'fail') {
            runStatus = RUN_FAILED
            planStatus = PLAN_FAILED
        }

        new PlanDecisionTransition(
            requestedDecision: decision,
            decision: normalizedDecision,
            targetRunStatus: runStatus,
            targetPlanStatus: planStatus,
            summary: normalizedSummary,
            reason: normalizedReason,
            missing: normalizedMissing
        )
    }

    private static TransitionAudit audit(String entityType, String currentStatus, String requestedStatus, Map<String, Set<String>> table, String trigger, String reason) {
        String requested = normalizeStatus(requestedStatus, null)
        Set<String> legalNext = legalNext(currentStatus, table)
        boolean valid = requested != null && legalNext.contains(requested)
        new TransitionAudit(
            entityType: entityType,
            currentStatus: currentStatus,
            requestedStatus: requested,
            valid: valid,
            normalizedStatus: valid ? requested : currentStatus,
            legalNextStatuses: legalNext as List<String>,
            trigger: trigger,
            reason: reason
        )
    }

    private static Set<String> legalNext(String currentStatus, Map<String, Set<String>> table) {
        new LinkedHashSet<String>(table.get(currentStatus) ?: ([] as Set<String>))
    }

    private static String normalizeStatus(String status, String defaultStatus) {
        String normalized = status == null ? null : status.trim()
        normalized == null || normalized.isEmpty() ? defaultStatus : normalized
    }

    private static String normalizeDecision(String decision, String defaultDecision) {
        String normalized = decision == null ? null : decision.trim()
        normalized == null || normalized.isEmpty() ? defaultDecision : normalized
    }

    private static boolean hasRemainingPlannedSteps(AgentPlanRecord plan) {
        (plan?.steps ?: ([] as List<AgentStepRecord>)).any {AgentStepRecord step ->
            step.kind != 'final_answer' && (step.status == null || step.status in [STEP_PENDING, STEP_IN_PROGRESS, STEP_REQUIRES_ACTION])
        }
    }

    private static boolean finalAnswerEvidencePresent(List<AgentEvidenceRecord> evidence) {
        (evidence ?: ([] as List<AgentEvidenceRecord>)).any {AgentEvidenceRecord item ->
            Boolean.TRUE == item.metadata?.get('pertinentToFinal')
        }
    }

    private static List<String> unmetDeclaredSuccessCriteria(List<String> missing, AgentPlanRecord plan) {
        Set<String> declared = ((plan?.successCriteria ?: []) as List<String>).collect {String criterion ->
            (criterion ?: '').trim()
        }.findAll {String criterion ->
            !criterion.isEmpty()
        }.toSet()
        (missing ?: ([] as List<String>)).collect {String item ->
            (item ?: '').trim()
        }.findAll {String item ->
            declared.contains(item)
        } as List<String>
    }

    private static String stepDecisionReason(String decision, boolean completed, boolean retryAllowed) {
        if (completed) {
            return 'The step decision indicates the current step completed.'
        }
        if (decision == 'retry') {
            return retryAllowed ? 'The step can be retried within the configured retry budget.' : 'The retry budget has been exhausted; the step remains failed.'
        }
        if (decision == 'ask_user') {
            return 'The step requires user input before it can continue.'
        }
        if (decision == 'replan') {
            return 'The step cannot complete as written and the plan should be revised.'
        }
        if (decision == 'fail') {
            return 'The step failure should fail the run.'
        }
        'The step did not complete.'
    }
}

@CompileStatic
class TransitionAudit {
    String entityType
    String currentStatus
    String requestedStatus
    Boolean valid = false
    String normalizedStatus
    List<String> legalNextStatuses = []
    String trigger
    String reason
}

@CompileStatic
class StepDecisionTransition {
    String requestedDecision
    String decision
    Boolean stepComplete = false
    String targetStepStatus
    String targetRunStatus
    Boolean retryAllowed = false
    String reason
}

@CompileStatic
class PlanDecisionTransition {
    String requestedDecision
    String decision
    String targetRunStatus
    String targetPlanStatus
    String summary
    String reason
    List<String> missing = []
}

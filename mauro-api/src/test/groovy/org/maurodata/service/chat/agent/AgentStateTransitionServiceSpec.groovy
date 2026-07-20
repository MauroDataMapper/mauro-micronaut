package org.maurodata.service.chat.agent

import spock.lang.Specification

class AgentStateTransitionServiceSpec extends Specification {

    AgentStateTransitionService service = new AgentStateTransitionService()

    void 'audits valid and invalid run transitions'() {
        expect:
        service.auditRunTransition('queued', 'in_progress', 'run_started').valid
        service.auditRunTransition('in_progress', 'completed', 'final_written').valid
        service.auditRunTransition('completed', 'in_progress', 'resume_after_complete').valid == false
        service.auditRunTransition('completed', 'in_progress', 'resume_after_complete').normalizedStatus == 'completed'
    }

    void 'audits valid and invalid step transitions'() {
        expect:
        service.auditStepTransition('pending', 'in_progress', 'step_started').valid
        service.auditStepTransition('in_progress', 'completed', 'step_completed').valid
        service.auditStepTransition('completed', 'pending', 'retry_completed_step').valid == false
        service.legalStepNextStatuses('failed').contains('pending')
    }

    void 'audits valid and invalid plan transitions'() {
        expect:
        service.auditPlanTransition('active', 'superseded', 'replan').valid
        service.auditPlanTransition('active', 'complete', 'final').valid
        service.auditPlanTransition('complete', 'active', 'reopen').valid == false
    }

    void 'audits valid and invalid operation transitions'() {
        expect:
        service.auditOperationTransition('pending', 'in_progress', 'operation_started').valid
        service.auditOperationTransition('in_progress', 'completed', 'operation_completed').valid
        service.auditOperationTransition('in_progress', 'retrying', 'operation_retrying').valid
        service.auditOperationTransition('retrying', 'in_progress', 'operation_restarted').valid
        service.auditOperationTransition('failed', 'exhausted', 'operation_exhausted').valid
        service.auditOperationTransition('completed', 'retrying', 'retry_completed_operation').valid == false
        service.legalOperationNextStatuses('retrying') == ['in_progress', 'exhausted', 'cancelled'] as Set
    }

    void 'normalizes step evaluator decisions'() {
        expect:
        service.normalizeStepDecision('continue', false, 0, 2).with {
            decision == 'continue' && stepComplete && targetStepStatus == 'completed' && targetRunStatus == null
        }
        service.normalizeStepDecision('retry', false, 0, 2).with {
            decision == 'retry' && !stepComplete && retryAllowed && targetStepStatus == 'pending'
        }
        service.normalizeStepDecision('retry', false, 2, 2).with {
            decision == 'retry' && !stepComplete && !retryAllowed && targetStepStatus == 'failed'
        }
        service.normalizeStepDecision('ask_user', false, 0, 2).with {
            decision == 'ask_user' && targetStepStatus == 'requires_action' && targetRunStatus == 'requires_action'
        }
        service.normalizeStepDecision('fail', false, 0, 2).with {
            decision == 'fail' && targetStepStatus == 'failed' && targetRunStatus == 'failed'
        }
    }

    void 'normalizes plan continue to final when no remaining steps exist'() {
        given:
        AgentPlanRecord plan = new AgentPlanRecord(
            status: 'active',
            steps: [
                new AgentStepRecord(kind: 'search', status: 'completed')
            ]
        )

        when:
        PlanDecisionTransition transition = service.normalizePlanDecision('continue', plan, [])

        then:
        transition.decision == 'final'
        transition.targetPlanStatus == 'complete'
        transition.summary.contains('No remaining planned steps')
    }

    void 'normalizes unsupported replan to final when no declared criteria are missing and final evidence exists'() {
        given:
        AgentPlanRecord plan = new AgentPlanRecord(
            status: 'active',
            successCriteria: ['Evidence: Search results retrieved.'],
            steps: [
                new AgentStepRecord(kind: 'search', status: 'completed')
            ]
        )
        List<AgentEvidenceRecord> evidence = [
            new AgentEvidenceRecord(metadata: [pertinentToFinal: true])
        ]

        when:
        PlanDecisionTransition transition = service.normalizePlanDecision(
            'replan',
            plan,
            evidence,
            ['Invented: Fetch every page.']
        )

        then:
        transition.decision == 'final'
        transition.targetPlanStatus == 'complete'
        transition.missing == []
    }

    void 'preserves replan when evaluator names an unmet declared criterion'() {
        given:
        AgentPlanRecord plan = new AgentPlanRecord(
            status: 'active',
            successCriteria: ['Evidence: Search results retrieved.', 'Answer: Present the list.'],
            steps: [
                new AgentStepRecord(kind: 'search', status: 'completed')
            ]
        )
        List<AgentEvidenceRecord> evidence = [
            new AgentEvidenceRecord(metadata: [pertinentToFinal: true])
        ]

        when:
        PlanDecisionTransition transition = service.normalizePlanDecision(
            'replan',
            plan,
            evidence,
            ['Answer: Present the list.', 'Invented: Fetch every page.']
        )

        then:
        transition.decision == 'replan'
        transition.targetPlanStatus == 'superseded'
        transition.missing == ['Answer: Present the list.']
    }
}

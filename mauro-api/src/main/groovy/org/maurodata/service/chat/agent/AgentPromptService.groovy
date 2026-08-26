package org.maurodata.service.chat.agent

import groovy.json.JsonOutput
import groovy.transform.CompileStatic
import jakarta.inject.Singleton
import org.maurodata.service.chat.ChatPromptAssetDefinition
import org.maurodata.service.chat.ChatPromptComposer
import org.maurodata.service.chat.ChatPromptRenderResult

@CompileStatic
@Singleton
class AgentPromptService {

    private final ChatPromptComposer composer

    AgentPromptService(ChatPromptComposer composer) {
        this.composer = composer
    }

    String plannerSystemPrompt(List<String> toolNames, String replanReason = null) {
        composer.render('agent-planner-system', [
            toolNames: (toolNames ?: []).join(', '),
            planningModeInstructions: plannerModeInstructions(replanReason)
        ] as Map<String, Object>)
    }

    String contextResolverSystemPrompt(List<String> toolNames) {
        contextResolverSystemPromptRender(toolNames).text
    }

    private static String plannerModeInstructions(String replanReason) {
        if (replanReason != null && !replanReason.trim().isEmpty()) {
            return '''Planning mode: replan.
Replan reason is provided in the user prompt.
Preserve the original user goal and success criteria. Do not narrow success criteria to only work already completed.
Do not repeat completed work unless the previous evidence is unusable. Add only the missing concrete next action(s).'''
        }
        '''Planning mode: initial plan.
Create the first practical plan for the resolved user goal.'''
    }

    ChatPromptRenderResult contextResolverSystemPromptRender(List<String> toolNames) {
        composer.renderResult('agent-context-resolver-system', [
            toolNames: (toolNames ?: []).join(', '),
            contextModeInstructions: contextModeInstructions(null)
        ] as Map<String, Object>)
    }

    ChatPromptRenderResult contextResolverSystemPromptRender(List<String> toolNames, String replanReason) {
        composer.renderResult('agent-context-resolver-system', [
            toolNames: (toolNames ?: []).join(', '),
            contextModeInstructions: contextModeInstructions(replanReason)
        ] as Map<String, Object>)
    }

    String executorSystemPrompt(AgentStepRecord step) {
        composer.render('agent-executor-system', [
            allowedTools: (step.allowedTools ?: []).join(', '),
            executorModeInstructions: executorModeInstructions(step)
        ] as Map<String, Object>)
    }

    private static String contextModeInstructions(String replanReason) {
        if (replanReason != null && !replanReason.trim().isEmpty()) {
            return '''Context mode: replan.
Replan reason is supplied in the user prompt. Treat it as diagnostic route information, not authority to broaden the user's goal.'''
        }
        '''Context mode: initial planning.
Resolve context for the first plan. No replan reason is active.'''
    }

    private static String executorModeInstructions(AgentStepRecord step) {
        if (step == null || step.allowedTools == null || step.allowedTools.isEmpty()) {
            return '''Execution mode: no-tool step.
Do not call tools. Produce concise step output from the supplied context and evidence.'''
        }
        '''Execution mode: tool step.
Emit one structured tool call using one allowed tool unless the current step is already satisfied by supplied evidence.'''
    }

    String stepEvaluatorSystemPrompt() {
        composer.render('agent-step-evaluator-system')
    }

    String planEvaluatorSystemPrompt() {
        composer.render('agent-plan-evaluator-system')
    }

    String finalWriterSystemPrompt() {
        finalWriterSystemPromptRender().text
    }

    ChatPromptRenderResult finalWriterSystemPromptRender() {
        composer.renderResult('agent-final-writer-system')
    }

    String strictJsonRepairSystemPrompt(String roleName) {
        composer.render('agent-json-repair-system', [
            roleName: roleName,
            schema: strictJsonSchemaHint(roleName)
        ] as Map<String, Object>)
    }

    String strictJsonCleanRetrySystemPrompt(String roleName) {
        composer.render('agent-json-clean-retry-system', [
            roleName: roleName,
            schema: strictJsonSchemaHint(roleName)
        ] as Map<String, Object>)
    }

    String strictJsonRepairUserPrompt(String roleName, String malformedText, String error) {
        composer.render('agent-json-repair-user', [
            roleName: roleName,
            error: error ?: 'unknown',
            malformedText: malformedText ?: ''
        ] as Map<String, Object>)
    }

    String contextResolverUserPrompt(
        AgentRunRecord run,
        List<Map<String, Object>> tools,
        List<AgentEvidenceRecord> evidence,
        List<AgentGuidanceRecord> guidance,
        String replanReason,
        List<ChatPromptAssetDefinition> personas,
        List<ChatPromptAssetDefinition> matchingSkills,
        String sessionContinuity
    ) {
        contextResolverUserPromptRender(run, tools, evidence, guidance, replanReason, personas, matchingSkills, sessionContinuity).text
    }

    ChatPromptRenderResult contextResolverUserPromptRender(
        AgentRunRecord run,
        List<Map<String, Object>> tools,
        List<AgentEvidenceRecord> evidence,
        List<AgentGuidanceRecord> guidance,
        String replanReason,
        List<ChatPromptAssetDefinition> personas,
        List<ChatPromptAssetDefinition> matchingSkills,
        String sessionContinuity
    ) {
        composer.renderResult('agent-context-resolver-user', [
            goal: run.goal ?: '',
            personaGuidance: renderPersonaSummaries(personas),
            skillLookupResult: renderSkillLookup(matchingSkills, false),
            toolAffordances: renderToolSummaries(tools, 120),
            sessionContinuityBlock: optionalBlock('Prior session continuity context', sessionContinuity),
            replanReasonBlock: optionalBlock('Replan reason', replanReason),
            evidenceBlock: evidence ? block('Evidence already gathered', renderEvidence(evidence)) : '',
            guidanceBlock: guidance ? block('Tool guidance already gathered', renderGuidance(guidance)) : ''
        ] as Map<String, Object>)
    }

    String plannerUserPrompt(
        AgentRunRecord run,
        List<Map<String, Object>> tools,
        List<AgentEvidenceRecord> evidence,
        String replanReason,
        AgentContextRecord context
    ) {
        composer.render('agent-planner-user', [
            goal: run.goal ?: '',
            resolvedContext: renderOperationalContextCompact(context),
            toolAffordances: renderToolSummaries(tools),
            replanReasonBlock: optionalBlock('Replan reason', replanReason),
            evidenceBlock: evidence ? block('Evidence already gathered', renderEvidenceCompact(evidence)) : '',
            remainingWorkInstruction: evidence ? '\n\nPlan only the remaining work required to satisfy the original goal. Use exact tool names from the available tool list. Preserve the original comparison goal and do not replace it with a summary of completed work.' : ''
        ] as Map<String, Object>)
    }

    String executorUserPrompt(
        AgentRunRecord run,
        AgentPlanRecord plan,
        AgentContextRecord context,
        AgentStepRecord step,
        List<AgentEvidenceRecord> evidence,
        List<AgentGuidanceRecord> guidance
    ) {
        composer.render('agent-executor-user', [
            goal: run.goal ?: '',
            operationalContext: renderOperationalContextCompact(context),
            planGoal: plan.goalRestatement ?: '',
            stepTitle: step.title ?: '',
            stepObjective: step.objective ?: '',
            expectedOutput: step.expectedOutput ?: '',
            priorEvidence: needsFullEvidenceForExecutor(step) ? renderEvidence(evidence) : renderEvidenceCompact(evidence),
            priorGuidance: renderGuidance(guidance, needsFullEvidenceForExecutor(step) ? 1500 : 600)
        ] as Map<String, Object>)
    }

    String stepEvaluatorUserPrompt(
        AgentRunRecord run,
        AgentPlanRecord plan,
        AgentContextRecord context,
        AgentStepRecord step,
        List<AgentEvidenceRecord> evidence,
        List<AgentEvidenceRecord> currentStepEvidence,
        List<AgentGuidanceRecord> guidance
    ) {
        composer.render('agent-step-evaluator-user', [
            goal: run.goal ?: '',
            planSuccessCriteria: criteriaMarkdown(plan.successCriteria),
            stepTitle: step.title ?: '',
            stepObjective: step.objective ?: '',
            expectedOutput: step.expectedOutput ?: '',
            stepSuccessCriteria: criteriaMarkdown(step.successCriteria),
            operationalContext: renderOperationalContextCompact(context),
            currentStepEvidence: renderCurrentStepEvidenceForEvaluator(currentStepEvidence),
            evidence: renderEvidenceCompact(evidence),
            guidance: renderGuidance(guidance, 600)
        ] as Map<String, Object>)
    }

    String planEvaluatorUserPrompt(
        AgentRunRecord run,
        AgentPlanRecord plan,
        AgentContextRecord context,
        AgentStepRecord completedStep,
        List<AgentEvidenceRecord> evidence,
        List<AgentGuidanceRecord> guidance
    ) {
        List<AgentStepRecord> remainingSteps = (plan.steps ?: [] as List<AgentStepRecord>).findAll {AgentStepRecord step ->
            step.status == null || step.status in ['pending', 'in_progress']
        } as List<AgentStepRecord>
        composer.render('agent-plan-evaluator-user', [
            goal: run.goal ?: '',
            planGoal: plan.goalRestatement ?: '',
            planSuccessCriteria: criteriaMarkdownWithIds(plan.successCriteria),
            completedStepTitle: completedStep.title ?: '',
            completedStepObjective: completedStep.objective ?: '',
            remainingSteps: renderSteps(remainingSteps),
            operationalContext: renderOperationalContextCompact(context),
            evidence: renderEvidenceCompact(evidence),
            guidance: renderGuidance(guidance, 600)
        ] as Map<String, Object>)
    }

    String finalWriterUserPrompt(
        AgentRunRecord run,
        AgentPlanRecord plan,
        AgentContextRecord context,
        List<AgentEvidenceRecord> evidence,
        List<AgentGuidanceRecord> guidance
    ) {
        finalWriterUserPromptRender(run, plan, context, evidence, guidance).text
    }

    ChatPromptRenderResult finalWriterUserPromptRender(
        AgentRunRecord run,
        AgentPlanRecord plan,
        AgentContextRecord context,
        List<AgentEvidenceRecord> evidence,
        List<AgentGuidanceRecord> guidance
    ) {
        List<AgentEvidenceRecord> finalEvidence = finalAnswerEvidence(evidence, run.id)
        composer.renderResult('agent-final-writer-user', [
            goal: run.goal ?: '',
            finalContext: renderFinalContext(context),
            planGoal: plan.goalRestatement ?: '',
            successCriteria: criteriaMarkdown(plan.successCriteria),
            evidence: renderFinalEvidence(finalEvidence, evidence),
            finalGuidance: renderFinalGuidance(guidance, finalEvidence, evidence)
        ] as Map<String, Object>)
    }

    private static String optionalBlock(String title, String content) {
        content == null || content.trim().isEmpty() ? '' : block(title, content)
    }

    private static String block(String title, String content) {
        "\n\n${title}:\n${content ?: ''}".toString()
    }

    static String strictJsonSchemaHint(String roleName) {
        if (roleName == 'context_resolver') {
            return '''{
  "goalRestatement": "string",
  "followUpInterpretation": "new_task|follow_up|continuation|refinement|paging_request|details_request|ambiguous_reference",
  "goalFrame": {
    "userGoal": "string",
    "interpretedGoal": "string",
    "scope": "string",
    "nonGoals": ["string"],
    "acceptableCompletion": {
      "mode": "answer_with_caveats|requires_exhaustive_evidence|requires_clarification",
      "minimumEvidence": ["string"],
      "acceptableCaveats": ["string"],
      "mustNotBlockOn": ["string"]
    }
  },
  "domainContext": ["terse phrase"],
  "relevantTools": [{"name": "exact advertised tool name", "reason": "terse phrase", "readOnly": true|false}],
  "recommendedSkills": [{"id": "string", "reason": "terse phrase", "usage": "terse phrase"}],
  "relevantResources": [{"name": "string", "reason": "terse phrase", "uri": null|"string"}],
  "instructions": [{"type": "string", "target": "planner|executor|step_evaluator|plan_evaluator|final_writer|all", "instruction": "terse phrase"}],
  "resolvedReferences": [{"phrase": "string", "evidenceId": null|"string", "resourceRef": null|"string", "reason": "terse phrase"}],
  "resolvedResources": [{"label": "string", "id": null|"string", "domainType": null|"string", "uri": null|"string", "evidenceId": null|"string", "reason": "terse phrase"}],
  "priorEvidenceToReuse": [{"evidenceId": "string", "reason": "terse phrase"}],
  "priorGuidanceToFollow": [{"guidanceId": "string", "reason": "terse phrase"}],
  "contextRequests": [{"type": "session_memory_page|session_resource_lookup", "query": "string", "reason": "terse phrase"}],
  "planningHints": ["terse phrase"],
  "constraints": ["terse phrase"]
}'''
        }
        if (roleName == 'step_evaluator') {
            return '''{
  "stepComplete": true|false,
  "decision": "continue"|"retry"|"ask_user"|"replan"|"fail",
  "summary": "terse outcome",
  "reason": "terse evidence-based reason",
  "question": null|"string"
}'''
        }
        if (roleName == 'plan_evaluator') {
            return '''{
  "decision": "continue"|"final"|"ask_user"|"replan"|"fail",
  "summary": "terse outcome",
  "reason": "terse evidence-based reason",
  "question": null|"string",
  "missing": ["exact unmet criterion"],
  "obsoleteStepIds": ["string"],
  "replanJustification": {
    "category": "missing_required_step|remaining_step_obsolete|remaining_step_not_executable|tool_or_capability_unavailable|new_evidence_changes_route|previous_step_failed|no_structural_category_applies",
    "evidenceIds": ["string"],
    "affectedStepIds": ["string"],
    "unmetSuccessCriteriaIds": ["criterion-1"],
    "proposedChange": null|"terse phrase"
  }
}'''
        }
        '''{
  "goalRestatement": "string",
  "fitness": "string",
  "successCriteria": ["Short, testable criterion. Use an area label such as Evidence:, Comparison:, Answer:, or Limitation:"],
  "assumptions": ["string"],
  "risks": ["string"],
  "steps": [
    {
      "title": "string",
      "objective": "string",
      "kind": "string",
      "allowedTools": ["exact advertised tool name"],
      "guard": "always|if_no_final_evidence|if_no_successful_tool_evidence|if_previous_step_failed",
      "guardReason": "terse phrase",
      "optional": true|false,
      "expectedOutput": "terse phrase",
      "successCriteria": ["Short, testable criterion for this step. Use an area label such as Tool:, Evidence:, Analysis:, or Output:"]
    }
  ]
}'''
    }

    private static String renderSteps(List<AgentStepRecord> steps) {
        if (!steps) {
            return 'No remaining planned steps.'
        }
        StringBuilder builder = new StringBuilder(1024)
        for (AgentStepRecord step : steps) {
            builder.append('- ')
                .append(step.id ?: '')
                .append(' | ')
                .append(step.status ?: 'pending')
                .append(' | ')
                .append('guard=')
                .append(step.guard ?: 'always')
                .append(' | ')
                .append(step.title ?: '')
                .append(': ')
                .append(step.objective ?: '')
                .append('\n')
        }
        builder.toString().trim()
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

    private static String criteriaMarkdownWithIds(List<String> items) {
        if (!items) {
            return ''
        }
        StringBuilder builder = new StringBuilder('## Success Criteria\n')
        int index = 1
        for (String item : items) {
            builder.append('- criterion-')
                .append(index++)
                .append(': ')
                .append(item ?: '')
                .append('\n')
        }
        builder.toString().trim()
    }

    private static String renderEvidence(List<AgentEvidenceRecord> evidence) {
        if (!evidence) {
            return 'No evidence yet.'
        }
        StringBuilder builder = new StringBuilder(2048)
        int index = 1
        for (AgentEvidenceRecord item : evidence) {
            builder.append(index++)
                .append('. ')
                .append(item.title ?: item.sourceName ?: item.id)
                .append('\n')
            if (item.summary != null && !item.summary.trim().isEmpty()) {
                builder.append(item.summary.trim()).append('\n')
            }
            if (item.content != null && !item.content.trim().isEmpty()) {
                builder.append(renderEvidencePayloadView(item, evidenceContentLimit(item, 1500), 'Evidence payload'))
            }
            builder.append('\n')
        }
        builder.toString().trim()
    }

    private static String renderEvidenceCompact(List<AgentEvidenceRecord> evidence) {
        if (!evidence) {
            return 'No evidence yet.'
        }
        StringBuilder builder = new StringBuilder(1024)
        int index = 1
        for (AgentEvidenceRecord item : evidence) {
            Map<String, Object> keys = compactEvidenceKeys(item)
            builder.append('- E')
                .append(index++)
                .append(' id=')
                .append(item.id ?: '')
                .append(' source=')
                .append(item.sourceName ?: item.sourceType ?: '')
                .append(' role=')
                .append(asString(item.metadata?.get('evidenceRole')) ?: '')
                .append(' ok=')
                .append(item.metadata?.get('ok'))
                .append('\n')
            if (item.title != null && !item.title.trim().isEmpty()) {
                builder.append('  title: ').append(limitText(item.title.trim(), 160)).append('\n')
            }
            if (item.summary != null && !item.summary.trim().isEmpty()) {
                builder.append('  summary: ').append(limitText(item.summary.trim(), 240)).append('\n')
            }
            builder.append('  view: compact projection; not a complete payload; omitted fields are not evidence of absence')
                .append('; contentChars=')
                .append(item.content == null ? 0 : item.content.length())
                .append('\n')
            if (!keys.isEmpty()) {
                builder.append('  keys: ').append(JsonOutput.toJson(keys)).append('\n')
            }
        }
        builder.toString().trim()
    }

    private static String renderCurrentStepEvidenceForEvaluator(List<AgentEvidenceRecord> evidence) {
        if (!evidence) {
            return 'No evidence yet.'
        }
        StringBuilder builder = new StringBuilder(4096)
        int index = 1
        for (AgentEvidenceRecord item : evidence) {
            builder.append('- E')
                .append(index++)
                .append(' id=')
                .append(item.id ?: '')
                .append(' source=')
                .append(item.sourceName ?: item.sourceType ?: '')
                .append(' role=')
                .append(asString(item.metadata?.get('evidenceRole')) ?: '')
                .append(' ok=')
                .append(item.metadata?.get('ok'))
                .append('\n')
            if (item.title != null && !item.title.trim().isEmpty()) {
                builder.append('  title: ').append(limitText(item.title.trim(), 160)).append('\n')
            }
            if (item.summary != null && !item.summary.trim().isEmpty()) {
                builder.append('  summary: ').append(limitText(item.summary.trim(), 240)).append('\n')
            }
            if (item.content != null && !item.content.trim().isEmpty()) {
                builder.append(renderEvidencePayloadView(item, evidenceContentLimit(item, 6000), '  payload'))
            } else {
                Map<String, Object> keys = compactEvidenceKeys(item)
                if (!keys.isEmpty()) {
                    builder.append('  keys: ').append(JsonOutput.toJson(keys)).append('\n')
                }
            }
        }
        builder.toString().trim()
    }

    private static String renderFinalEvidence(List<AgentEvidenceRecord> finalEvidence, List<AgentEvidenceRecord> allEvidence) {
        if (!finalEvidence) {
            return """## Final Evidence
No evidence has been marked as directly pertinent to the final answer.

## Available Intermediate Evidence
${renderEvidence(allEvidence)}""".toString()
        }
        StringBuilder builder = new StringBuilder(2048)
        int index = 1
        for (AgentEvidenceRecord item : finalEvidence) {
            builder.append('## Evidence ')
                .append(index++)
                .append(': ')
                .append(item.title ?: item.sourceName ?: item.id)
                .append('\n')
            if (item.summary != null && !item.summary.trim().isEmpty()) {
                builder.append(item.summary.trim()).append('\n')
            }
            if (item.content != null && !item.content.trim().isEmpty()) {
                builder.append(renderEvidencePayloadView(item, evidenceContentLimit(item, 2500), 'Evidence payload'))
            }
            builder.append('\n')
        }
        int omitted = (allEvidence ?: []).size() - finalEvidence.size()
        if (omitted > 0) {
            builder.append('## Omitted Intermediate Evidence\n')
                .append(omitted)
                .append(' intermediate evidence item')
                .append(omitted == 1 ? ' was' : 's were')
                .append(' omitted from final-answer context because more directly pertinent evidence was available.\n')
        }
        builder.toString().trim()
    }

    private static String renderGuidance(List<AgentGuidanceRecord> guidance, int maxContentChars = 1500) {
        if (!guidance) {
            return 'No tool guidance yet.'
        }
        StringBuilder builder = new StringBuilder(2048)
        int index = 1
        for (AgentGuidanceRecord item : guidance) {
            builder.append(index++)
                .append('. ')
                .append(item.sourceName ?: item.sourceType ?: item.id)
                .append(item.followForFinal ? ' | final guidance' : ' | process guidance')
                .append('\n')
            if (item.content != null && !item.content.trim().isEmpty()) {
                builder.append(item.content.take(maxContentChars)).append('\n')
            }
            builder.append('\n')
        }
        builder.toString().trim()
    }

    private static String renderFinalGuidance(List<AgentGuidanceRecord> guidance, List<AgentEvidenceRecord> finalEvidence, List<AgentEvidenceRecord> allEvidence) {
        List<AgentEvidenceRecord> renderedEvidence = finalEvidence ?: (allEvidence ?: [])
        Set<String> renderedGuidanceIds = guidanceIdsForEvidence(renderedEvidence)
        List<AgentGuidanceRecord> finalGuidance = finalAnswerGuidance(guidance, renderedEvidence)
        if (!finalGuidance) {
            return """No final-answer tool guidance.

## Final Guidance Diagnostics
Guidance records available: ${(guidance ?: []).size()}
Final evidence records available: ${(finalEvidence ?: []).size()}
Rendered evidence records available: ${(renderedEvidence ?: []).size()}
Rendered evidence guidance IDs: ${renderedGuidanceIds}
Guidance records:
${renderGuidanceDiagnostics(guidance)}""".toString()
        }
        """The following guidance came from tools that produced evidence in the final-answer context. Apply it when writing the answer.
${renderGuidance(finalGuidance, 8000)}""".toString()
    }

    private static boolean needsFullEvidenceForExecutor(AgentStepRecord step) {
        if (step == null) {
            return false
        }
        step.allowedTools == null || step.allowedTools.isEmpty()
    }

    private static int evidenceContentLimit(AgentEvidenceRecord item, int defaultLimit) {
        item?.sourceName == 'mauro_get' ? Math.max(defaultLimit, 12000) : defaultLimit
    }

    private static String renderEvidencePayloadView(AgentEvidenceRecord item, int maxChars, String label) {
        String text = item?.content?.trim()
        if (text == null || text.isEmpty()) {
            return ''
        }
        int totalChars = text.length()
        int shownChars = Math.min(totalChars, maxChars)
        boolean complete = totalChars <= maxChars
        StringBuilder builder = new StringBuilder(shownChars + 160)
        builder.append(label)
            .append(' view: chars ')
            .append(shownChars)
            .append(' of ')
            .append(totalChars)
            .append('; complete=')
            .append(complete)
            .append('; omitted fields are not evidence of absence')
            .append('\n')
            .append(text.take(maxChars))
        if (!complete) {
            builder.append('\n...[truncated evidence view]')
        }
        builder.append('\n')
        builder.toString()
    }

    private static Map<String, Object> compactEvidenceKeys(AgentEvidenceRecord item) {
        Map<String, Object> out = new LinkedHashMap<String, Object>()
        if (item == null) {
            return out
        }
        putIfPresent(out, 'callId', item.metadata?.get('callId'))
        putIfPresent(out, 'sourceId', item.sourceId)
        putIfPresent(out, 'ok', item.metadata?.get('ok'))
        collectKeyValues(item.structuredContent, out)
        out
    }

    private static void collectKeyValues(Object value, Map<String, Object> out) {
        if (value instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) value
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                String key = asString(entry.key)
                Object item = entry.value
                if (key == null) {
                    continue
                }
                if (key in ['tool', 'resourceRef', 'readUri', 'uri', 'href', 'id', 'label', 'domainType', 'count', 'total', 'hasMore', 'nextOffset', 'statusCode', 'error', 'blocked', 'duplicate']) {
                    putIfPresent(out, key, compactScalarOrList(item))
                } else if (key == 'arguments') {
                    Map<String, Object> args = compactArguments(getMap(item))
                    if (!args.isEmpty()) {
                        out.put('arguments', args)
                    }
                } else if (key in ['items', 'resources', 'results'] && item instanceof Collection) {
                    List<Map<String, Object>> items = compactItems((Collection<?>) item)
                    if (!items.isEmpty()) {
                        out.put(key, items)
                    }
                } else {
                    collectKeyValues(item, out)
                }
            }
        } else if (value instanceof Collection) {
            for (Object item : (Collection<?>) value) {
                collectKeyValues(item, out)
            }
        }
    }

    private static Object compactScalarOrList(Object value) {
        if (value instanceof Collection) {
            return ((Collection<?>) value)
                .take(5)
                .collect {Object item -> compactScalarOrList(item)}
        }
        if (value instanceof Map) {
            Map<String, Object> compact = new LinkedHashMap<String, Object>()
            collectKeyValues(value, compact)
            return compact
        }
        String text = asString(value)
        text == null ? null : limitText(text, 220)
    }

    private static List<Map<String, Object>> compactItems(Collection<?> items) {
        List<Map<String, Object>> out = new ArrayList<Map<String, Object>>()
        for (Object raw : (items ?: [])) {
            if (out.size() >= 5) {
                break
            }
            Map<String, Object> map = getMap(raw)
            if (map.isEmpty()) {
                continue
            }
            Map<String, Object> compact = new LinkedHashMap<String, Object>()
            for (String key : ['label', 'id', 'domainType', 'resourceRef', 'readUri', 'uri', 'href']) {
                putIfPresent(compact, key, map.get(key))
            }
            if (!compact.isEmpty()) {
                out.add(compact)
            }
        }
        out
    }

    private static Map<String, Object> compactArguments(Map<String, Object> arguments) {
        Map<String, Object> out = new LinkedHashMap<String, Object>()
        for (String key : ['uri', 'resourceRef', 'id', 'searchTerm', 'domainTypes', 'offset', 'max']) {
            putIfPresent(out, key, compactScalarOrList(arguments.get(key)))
        }
        out
    }

    private static void putIfPresent(Map<String, Object> out, String key, Object value) {
        if (value == null) {
            return
        }
        if (value instanceof String && ((String) value).trim().isEmpty()) {
            return
        }
        if (value instanceof Collection && ((Collection<?>) value).isEmpty()) {
            return
        }
        if (value instanceof Map && ((Map<?, ?>) value).isEmpty()) {
            return
        }
        out.put(key, value)
    }

    private static String renderGuidanceDiagnostics(List<AgentGuidanceRecord> guidance) {
        if (!guidance) {
            return '- none'
        }
        StringBuilder builder = new StringBuilder(1024)
        for (AgentGuidanceRecord item : guidance) {
            builder.append('- id=')
                .append(item.id ?: '')
                .append(' stepId=')
                .append(item.stepId ?: '')
                .append(' source=')
                .append(item.sourceName ?: '')
                .append(' followForFinal=')
                .append(Boolean.TRUE == item.followForFinal)
                .append(' contentChars=')
                .append(item.content == null ? 0 : item.content.length())
                .append('\n')
        }
        builder.toString().trim()
    }

    static List<AgentEvidenceRecord> finalAnswerEvidence(List<AgentEvidenceRecord> evidence) {
        List<AgentEvidenceRecord> pertinent = (evidence ?: []).findAll {AgentEvidenceRecord item ->
            Boolean.TRUE == item.metadata?.get('pertinentToFinal')
        } as List<AgentEvidenceRecord>
        pertinent.isEmpty() ? [] : pertinent
    }

    static List<AgentEvidenceRecord> finalAnswerEvidence(List<AgentEvidenceRecord> evidence, String currentRunId) {
        List<AgentEvidenceRecord> pertinent = finalAnswerEvidence(evidence)
        if (currentRunId == null || currentRunId.trim().isEmpty()) {
            return pertinent
        }
        List<AgentEvidenceRecord> currentRunEvidence = pertinent.findAll {AgentEvidenceRecord item ->
            item.runId == currentRunId
        } as List<AgentEvidenceRecord>
        currentRunEvidence.isEmpty() ? pertinent : currentRunEvidence
    }

    static List<AgentGuidanceRecord> finalAnswerGuidance(List<AgentGuidanceRecord> guidance, List<AgentEvidenceRecord> renderedEvidence) {
        Set<String> renderedGuidanceIds = guidanceIdsForEvidence(renderedEvidence)
        (guidance ?: []).findAll {AgentGuidanceRecord item ->
            renderedGuidanceIds.contains(item.id)
        } as List<AgentGuidanceRecord>
    }

    static Set<String> guidanceIdsForEvidence(List<AgentEvidenceRecord> evidence) {
        (evidence ?: []).collect {AgentEvidenceRecord item ->
            asString(item.metadata?.get('guidanceId'))
        }.findAll {String guidanceId ->
            guidanceId != null && !guidanceId.trim().isEmpty()
        }.toSet()
    }

    private static String renderContext(AgentContextRecord context) {
        if (context == null) {
            return 'No resolved context.'
        }
        StringBuilder builder = new StringBuilder(2048)
        builder.append('Goal restatement: ').append(context.goalRestatement ?: '').append('\n')
        if (context.followUpInterpretation != null && !context.followUpInterpretation.trim().isEmpty()) {
            builder.append('Follow-up interpretation: ').append(context.followUpInterpretation).append('\n')
        }
        appendMap(builder, 'Goal frame', context.goalFrame)
        appendStringList(builder, 'Domain context', context.domainContext)
        appendMapList(builder, 'Relevant tools', context.relevantTools)
        appendMapList(builder, 'Recommended skills', context.recommendedSkills)
        appendMapList(builder, 'Relevant resources', context.relevantResources)
        appendMapList(builder, 'Scoped instructions', context.instructions)
        appendMapList(builder, 'Resolved references', context.resolvedReferences)
        appendMapList(builder, 'Resolved resources', context.resolvedResources)
        appendMapList(builder, 'Prior evidence to reuse', context.priorEvidenceToReuse)
        appendMapList(builder, 'Prior guidance to follow', context.priorGuidanceToFollow)
        appendMapList(builder, 'Context requests', context.contextRequests)
        appendStringList(builder, 'Planning hints', context.planningHints)
        appendStringList(builder, 'Constraints', context.constraints)
        String sessionContinuity = asString(context.metadata?.get('sessionContinuity'))
        if (sessionContinuity != null && !sessionContinuity.trim().isEmpty()) {
            builder.append('\nSession continuity available to Perceive:\n')
                .append(sessionContinuity.take(2500))
                .append('\n')
        }
        builder.toString().trim()
    }

    private static String renderFinalContext(AgentContextRecord context) {
        if (context == null) {
            return 'No resolved context.'
        }
        StringBuilder builder = new StringBuilder(2048)
        String personaGuidance = asString(context.metadata?.get('personaGuidance'))
        if (personaGuidance != null && !personaGuidance.trim().isEmpty()) {
            builder.append('Persona guidance:\n')
                .append(personaGuidance.trim())
                .append('\n')
        }
        appendStringList(builder, 'Domain context', context.domainContext)
        appendMap(builder, 'Goal frame', context.goalFrame)
        appendMapList(builder, 'Final writer instructions', finalWriterInstructions(context.instructions))
        appendMapList(builder, 'Resolved references', context.resolvedReferences)
        appendMapList(builder, 'Resolved resources', context.resolvedResources)
        appendStringList(builder, 'Constraints', context.constraints)
        builder.toString().trim()
    }

    private static List<Map<String, Object>> finalWriterInstructions(List<Map<String, Object>> instructions) {
        (instructions ?: []).findAll {Map<String, Object> item ->
            String target = asString(item?.get('target'))?.trim()
            target == 'final_writer' || target == 'all'
        } as List<Map<String, Object>>
    }

    private static String renderOperationalContext(AgentContextRecord context) {
        if (context == null) {
            return 'No resolved context.'
        }
        StringBuilder builder = new StringBuilder(4096)
        String personaGuidance = asString(context.metadata?.get('personaGuidance'))
        if (personaGuidance != null && !personaGuidance.trim().isEmpty()) {
            builder.append('Persona guidance:\n')
                .append(personaGuidance.trim())
                .append('\n\n')
        }
        String skillLookupGuidance = asString(context.metadata?.get('skillLookupGuidance'))
        if (skillLookupGuidance != null && !skillLookupGuidance.trim().isEmpty()) {
            builder.append('Selected skill guidance:\n')
                .append(skillLookupGuidance.trim())
                .append('\n\n')
        }
        builder.append(renderContext(context))
        builder.toString().trim()
    }

    private static String renderOperationalContextCompact(AgentContextRecord context) {
        if (context == null) {
            return 'No resolved context.'
        }
        StringBuilder builder = new StringBuilder(2048)
        builder.append('Goal: ').append(limitText(context.goalRestatement ?: '', 240)).append('\n')
        if (context.followUpInterpretation != null && !context.followUpInterpretation.trim().isEmpty()) {
            builder.append('Follow-up: ').append(context.followUpInterpretation).append('\n')
        }
        String personaGuidance = asString(context.metadata?.get('personaGuidance'))
        if (personaGuidance != null && !personaGuidance.trim().isEmpty()) {
            builder.append('Persona:\n- ').append(limitText(personaGuidance.trim(), 700)).append('\n')
        }
        String skillLookupGuidance = asString(context.metadata?.get('skillLookupGuidance'))
        if (skillLookupGuidance != null && !skillLookupGuidance.trim().isEmpty()) {
            builder.append('Selected skill:\n- ').append(limitText(skillLookupGuidance.trim(), 1200)).append('\n')
        }
        appendCompactMap(builder, 'Goal frame', context.goalFrame)
        appendStringList(builder, 'Domain', context.domainContext)
        appendMapList(builder, 'Resolved resources', context.resolvedResources)
        appendMapList(builder, 'Resolved references', context.resolvedReferences)
        appendMapList(builder, 'Prior evidence ids', context.priorEvidenceToReuse)
        appendMapList(builder, 'Prior guidance ids', context.priorGuidanceToFollow)
        appendMapList(builder, 'Scoped instructions', context.instructions)
        appendStringList(builder, 'Planning hints', context.planningHints)
        appendStringList(builder, 'Constraints', context.constraints)
        String sessionContinuity = asString(context.metadata?.get('sessionContinuity'))
        if (sessionContinuity != null && !sessionContinuity.trim().isEmpty()) {
            builder.append('\nSession continuity available to Perceive:\n')
                .append(sessionContinuity.take(1200))
                .append('\n')
        }
        builder.toString().trim()
    }

    private static void appendCompactMap(StringBuilder builder, String title, Map<String, Object> item) {
        if (!item) {
            return
        }
        builder.append(title).append(':\n')
            .append(JsonOutput.toJson(item))
            .append('\n')
    }

    private static void appendMap(StringBuilder builder, String title, Map<String, Object> item) {
        if (!item) {
            return
        }
        builder.append('\n').append(title).append(':\n')
            .append(JsonOutput.prettyPrint(JsonOutput.toJson(item)))
            .append('\n')
    }

    private static String renderToolSummaries(List<Map<String, Object>> tools) {
        renderToolSummaries(tools, 500)
    }

    private static String renderToolSummaries(List<Map<String, Object>> tools, int maxDescriptionChars) {
        StringBuilder builder = new StringBuilder(2048)
        for (Map<String, Object> tool : tools ?: []) {
            Map<String, Object> fn = getMap(tool.get('function'))
            Map<String, Object> routing = getMap(tool.get('routing'))
            builder.append('- ')
                .append(asString(fn.get('name')) ?: '')
                .append(': ')
                .append(limitText(asString(fn.get('description')) ?: '', maxDescriptionChars))
                .append('\n')
            appendToolRouting(builder, routing)
        }
        builder.toString().trim()
    }

    private static void appendToolRouting(StringBuilder builder, Map<String, Object> routing) {
        if (!routing) {
            return
        }
        appendToolRoutingValue(builder, '  purpose: ', routing.get('purpose'), 220)
        appendToolRoutingValue(builder, '  use when: ', routing.get('useWhen'), 260)
        appendToolRoutingValue(builder, '  avoid when: ', routing.get('avoidWhen'), 320)
    }

    private static void appendToolRoutingValue(StringBuilder builder, String label, Object value, int maxChars) {
        String text = routingText(value)
        if (text == null || text.trim().isEmpty()) {
            return
        }
        builder.append(label)
            .append(limitText(text, maxChars))
            .append('\n')
    }

    private static String routingText(Object value) {
        if (value instanceof Collection) {
            return ((Collection<?>) value)
                .collect {Object item -> asString(item)}
                .findAll {String item -> item != null && !item.trim().isEmpty()}
                .join('; ')
        }
        asString(value)
    }

    static String renderPersonaSummaries(List<ChatPromptAssetDefinition> personas) {
        if (!personas) {
            return 'No persona guidance available.'
        }
        StringBuilder builder = new StringBuilder(1024)
        for (ChatPromptAssetDefinition persona : sortPromptAssets(personas)) {
            builder.append('- ')
                .append(persona.id ?: '')
                .append(' | ')
                .append(persona.name ?: '')
                .append(': ')
                .append(limitText(persona.description ?: '', 180))
                .append('\n')
        }
        builder.toString().trim()
    }

    static String renderPersonaSkills(List<ChatPromptAssetDefinition> personas) {
        if (!personas) {
            return 'No persona guidance available.'
        }
        StringBuilder builder = new StringBuilder(2048)
        for (ChatPromptAssetDefinition persona : sortPromptAssets(personas)) {
            builder.append('## ')
                .append(persona.name ?: persona.id ?: 'Persona')
                .append('\n')
            if (persona.description != null && !persona.description.trim().isEmpty()) {
                builder.append(persona.description.trim()).append('\n')
            }
            if (persona.instruction != null && !persona.instruction.trim().isEmpty()) {
                builder.append(limitText(persona.instruction.trim(), 4000)).append('\n')
            }
            builder.append('\n')
        }
        builder.toString().trim()
    }

    static String renderSkillLookup(List<ChatPromptAssetDefinition> skills, boolean includeInstruction = false) {
        if (!skills) {
            return 'No matching non-persona skills found. If broader skill context is needed, plan an explicit mauro_skill call with {"list":true}.'
        }
        StringBuilder builder = new StringBuilder(4096)
        for (ChatPromptAssetDefinition skill : sortPromptAssets(skills)) {
            builder.append('- ')
                .append(skill.id ?: '')
                .append(' | ')
                .append(skill.name ?: '')
                .append(': ')
                .append(skill.description ?: '')
                .append('\n')
            if (skill.keywords) {
                builder.append('  Keywords: ').append((skill.keywords ?: []).join(', ')).append('\n')
            }
            if (skill.seeAlso) {
                builder.append('  See also: ').append((skill.seeAlso ?: []).join(', ')).append('\n')
            }
            if (includeInstruction && skill.instruction != null && !skill.instruction.trim().isEmpty()) {
                builder.append('  Full guidance:\n')
                    .append(indentText(limitText(skill.instruction.trim(), 3500), '  '))
                    .append('\n')
            }
        }
        builder.toString().trim()
    }

    private static void appendStringList(StringBuilder builder, String title, List<String> items) {
        if (!items) {
            return
        }
        builder.append(title).append(':\n')
        for (String item : items) {
            builder.append('- ').append(item ?: '').append('\n')
        }
    }

    private static void appendMapList(StringBuilder builder, String title, List<Map<String, Object>> items) {
        if (!items) {
            return
        }
        builder.append(title).append(':\n')
        for (Map<String, Object> item : items) {
            builder.append('- ').append(JsonOutput.toJson(item)).append('\n')
        }
    }

    private static Map<String, Object> getMap(Object value) {
        if (value instanceof Map) {
            @SuppressWarnings('unchecked')
            Map<String, Object> typed = (Map<String, Object>) value
            return typed
        }
        [:] as Map<String, Object>
    }

    private static String asString(Object value) {
        value == null ? null : value.toString()
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

    private static String limitText(String text, int maxChars) {
        if (text == null || text.length() <= maxChars) {
            return text
        }
        text.substring(0, maxChars) + '\n...[truncated]'
    }

    private static String indentText(String text, String prefix) {
        (text ?: '').readLines().collect {String line -> prefix + line}.join('\n')
    }
}

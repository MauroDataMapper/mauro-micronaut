package org.maurodata.service.chat.mcp

import groovy.transform.CompileStatic
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.maurodata.service.chat.AffordanceBroker
import org.maurodata.service.chat.AffordanceContext

@CompileStatic
@Singleton
class ResultGuidanceService {

    private final AffordanceBroker affordanceBroker
    private final List<ResultInterpretation> interpretations

    @Inject
    ResultGuidanceService(AffordanceBroker affordanceBroker, List<ResultInterpretation> interpretations) {
        this.affordanceBroker = affordanceBroker
        this.interpretations = sortInterpretations(interpretations)
    }

    ResultGuidanceService(McpHttpResourceRegistry resourceRegistry) {
        this(new AffordanceBroker(resourceRegistry), [] as List<ResultInterpretation>)
    }

    ResultGuidanceService() {
        this(new AffordanceBroker(), defaultInterpretations())
    }

    String applyToolGuidance(String toolName, Map<String, Object> result, String modelText) {
        if (!withGuidance(result)) {
            return modelText
        }
        Map<String, Object> safeResult = result ?: [:] as Map<String, Object>
        String text = modelText
        ResultContext context = buildContext(toolName, safeResult)
        List<ResultInterpretationOutput> interpretationOutputs = interpret(context)
        if (!interpretationOutputs.isEmpty()) {
            text = appendInterpretations(text, interpretationOutputs)
        }
        List<Map<String, Object>> affordances = availableNextAffordances(toolName, safeResult)
        if (!affordances.isEmpty()) {
            safeResult.put('affordances', affordances)
        }
        List<String> actions = affordanceBroker.renderModelActions(affordances)
        if (actions.isEmpty()) {
            return text
        }
        appendSectionBeforeEnd(text, 'Available Next Actions', actions)
    }

    List<ResultInterpretationOutput> interpret(ResultContext context) {
        List<ResultInterpretationOutput> outputs = new ArrayList<ResultInterpretationOutput>()
        for (ResultInterpretation interpretation : interpretations ?: []) {
            if (interpretation != null && interpretation.supports(context)) {
                ResultInterpretationOutput output = interpretation.interpret(context)
                if (output != null) {
                    outputs.add(output)
                }
            }
        }
        outputs
    }

    List<String> availableNextActions(Map<String, Object> result) {
        affordanceBroker.renderModelActions(availableNextAffordances(result))
    }

    List<Map<String, Object>> availableNextAffordances(Map<String, Object> result) {
        availableNextAffordances(null, result)
    }

    List<Map<String, Object>> availableNextAffordances(String toolName, Map<String, Object> result) {
        Map<String, Object> safeResult = result ?: [:] as Map<String, Object>
        affordanceBroker.deriveMaps(new AffordanceContext(
            sourceType: 'tool_result',
            sourceName: toolName,
            result: safeResult,
            artefacts: extractArtefacts(safeResult)
        ))
    }

    private static ResultContext buildContext(String toolName, Map<String, Object> result) {
        new ResultContext(
            sourceName: toolName,
            resourceName: asString(result.get('name')),
            uri: asString(result.get('uri')),
            mimeType: asString(result.get('mimeType')),
            statusCode: result.containsKey('statusCode') ? asInteger(result.get('statusCode'), null) : null,
            result: result,
            artefacts: extractArtefacts(result)
        )
    }

    private static List<Object> extractArtefacts(Map<String, Object> result) {
        List<Object> artefacts = new ArrayList<Object>()
        Object rawArtefacts = result.get('artefacts')
        if (rawArtefacts instanceof Collection) {
            artefacts.addAll((Collection<?>) rawArtefacts)
        }
        String resourceName = asString(result.get('name'))
        if (resourceName == 'DataModel.show') {
            artefacts.add([
                type      : 'DataModelResource',
                domainType: 'DataModel',
                id        : result.get('id'),
                label     : result.get('label')
            ] as Map<String, Object>)
        }
        Object rawItems = result.get('items')
        if (rawItems instanceof Collection) {
            artefacts.addAll((Collection<?>) rawItems)
        }
        artefacts
    }

    private static boolean withGuidance(Map<String, Object> result) {
        Boolean.FALSE != result?.get('withGuidance')
    }

    private static String appendInterpretations(String text, List<ResultInterpretationOutput> outputs) {
        List<String> lines = new ArrayList<String>()
        for (ResultInterpretationOutput output : outputs) {
            if (output == null) {
                continue
            }
            if (output.title != null && !output.title.trim().isEmpty()) {
                lines.add("### ${output.title}".toString())
            }
            for (String statement : output.statements ?: []) {
                lines.add(statement)
            }
            for (String rendered : output.renderedContent ?: []) {
                lines.add(rendered)
            }
            if (!(output.answerInstructions ?: []).isEmpty()) {
                if (!(output.renderedContent ?: []).isEmpty()) {
                    lines.add('')
                }
                lines.add('Answer instructions from this interpretation:')
                for (String instruction : output.answerInstructions) {
                    lines.add('- ' + instruction)
                }
            }
        }
        appendSection(text, 'Additional Interpretations', lines)
    }

    private static String appendSection(String text, String heading, List<String> lines) {
        StringBuilder builder = new StringBuilder(text ?: '')
        if (builder.length() > 0) {
            builder.append('\n\n')
        }
        builder.append('## ')
            .append(heading)
            .append('\n')
        for (String line : lines) {
            if (line != null) {
                builder.append(line)
                    .append('\n')
            }
        }
        builder.toString()
    }

    private static String appendSectionBeforeEnd(String text, String heading, List<String> lines) {
        String section = appendSection('', heading, lines)
        String marker = '\n\n## END\n'
        int markerIndex = (text ?: '').indexOf(marker)
        if (markerIndex < 0) {
            return appendSection(text, heading, lines)
        }
        String before = text.substring(0, markerIndex)
        String after = text.substring(markerIndex)
        before + '\n\n' + section.trim() + after
    }

    private static String asString(Object value) {
        value == null ? null : String.valueOf(value)
    }

    private static Integer asInteger(Object value, Integer fallback) {
        if (value == null) {
            return fallback
        }
        if (value instanceof Number) {
            return ((Number) value).intValue()
        }
        String text = String.valueOf(value)
        text.trim().isEmpty() ? fallback : Integer.valueOf(text)
    }

    private static List<ResultInterpretation> defaultInterpretations() {
        [
            new FailedHttpResourceInterpretation()
        ] as List<ResultInterpretation>
    }

    private static List<ResultInterpretation> sortInterpretations(List<ResultInterpretation> values) {
        new ArrayList<ResultInterpretation>(values ?: [])
            .sort {ResultInterpretation left, ResultInterpretation right ->
                Integer leftPriority = left?.priority() ?: Integer.valueOf(1000)
                Integer rightPriority = right?.priority() ?: Integer.valueOf(1000)
                int priorityCompare = leftPriority <=> rightPriority
                priorityCompare != 0 ? priorityCompare : (left?.id() ?: '') <=> (right?.id() ?: '')
            } as List<ResultInterpretation>
    }
}

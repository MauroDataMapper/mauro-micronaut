package org.maurodata.plugin.chat.mcp

import org.maurodata.service.chat.mcp.*

import groovy.transform.CompileStatic
import jakarta.inject.Singleton

@CompileStatic
@Singleton
class DataModelResourceInterpretation implements ResultInterpretation {

    @Override
    String id() {
        'data-model-resource'
    }

    @Override
    Integer priority() {
        100
    }

    @Override
    List<String> appliesToResourceNames() {
        ['DataModel.show']
    }

    @Override
    List<String> appliesToTypes() {
        ['DataModelResource', 'DataModel']
    }

    @Override
    boolean supports(ResultContext context) {
        context?.successfulHttpStatus() && (
            appliesToResourceNames().contains(context.resourceName) ||
                !context.artefactsOfType('DataModelResource').isEmpty() ||
                !context.artefactsOfType('DataModel').isEmpty()
        )
    }

    @Override
    ResultInterpretationOutput interpret(ResultContext context) {
        Map<String, Object> data = resourceData(context?.result ?: [:] as Map<String, Object>)
        Map<String, Object> distilled = distilDataModel(data)
        new ResultInterpretationOutput(
            id: id(),
            title: 'DataModel Reading Guidance',
            statements: [
                'Interpret the raw returned resource content as one Mauro DataModel resource, not as a search result page.',
                'The earlier search/list page is no longer the answer source once this DataModel resource has been fetched.',
                'This interpretation highlights useful fields in the raw DataModel JSON; it is not a replacement for the raw returned data.'
            ],
            renderedContent: renderSummary(distilled),
            answerInstructions: [
                'Answer the current request by describing this fetched DataModel. Do not repeat the previous search result table unless the user explicitly asks to compare or revisit the search results.',
                'Summarise what the DataModel appears to represent using the label, description, classifiers, path, type, and metadata in the raw JSON.',
                'Mention whether the DataModel appears draft/finalised when finalised is present.',
                'If presenting DataModel details in a Markdown table, escape any pipe characters inside cell values as \\|, including the path value.',
                'If the path or another value contains several pipe characters, prefer putting that value in a bullet or fenced code block outside the table.',
                'Do not invent child classes, fields, or hierarchy unless they are present in the returned JSON.'
            ],
            distilledData: distilled
        )
    }

    private static Map<String, Object> resourceData(Map<String, Object> result) {
        Object data = result.get('data')
        if (data instanceof Map) {
            @SuppressWarnings('unchecked')
            Map<String, Object> map = (Map<String, Object>) data
            return map
        }
        result
    }

    private static Map<String, Object> distilDataModel(Map<String, Object> data) {
        Map<String, Object> distilled = new LinkedHashMap<String, Object>()
        putIfPresent(distilled, 'id', data.get('id'))
        putIfPresent(distilled, 'domainType', data.get('domainType'))
        putIfPresent(distilled, 'label', data.get('label'))
        putIfPresent(distilled, 'description', data.get('description'))
        putIfPresent(distilled, 'classifiers', labels(data.get('classifiers')))
        putIfPresent(distilled, 'path', data.get('path'))
        putIfPresent(distilled, 'metadata', metadataSummary(data.get('metadata')))
        putIfPresent(distilled, 'finalised', data.get('finalised'))
        putIfPresent(distilled, 'type', data.get('type'))
        putIfPresent(distilled, 'modelType', data.get('modelType'))
        putIfPresent(distilled, 'branchName', data.get('branchName'))
        putIfPresent(distilled, 'version', data.get('version'))
        distilled
    }

    private static void putIfPresent(Map<String, Object> target, String key, Object value) {
        if (value != null && !String.valueOf(value).trim().isEmpty()) {
            target.put(key, value)
        }
    }

    private static List<String> renderSummary(Map<String, Object> distilled) {
        if (distilled == null || distilled.isEmpty()) {
            return Collections.<String>emptyList()
        }
        List<String> lines = new ArrayList<String>()
        lines.add('Useful fields to consider when summarising the raw DataModel JSON:')
        for (Map.Entry<String, Object> entry : distilled.entrySet()) {
            lines.add("- ${entry.key}: ${formatValue(entry.value)}".toString())
        }
        lines
    }

    private static List<String> labels(Object value) {
        if (!(value instanceof Collection)) {
            return Collections.<String>emptyList()
        }
        List<String> labels = new ArrayList<String>()
        for (Object itemObj : (Collection<?>) value) {
            if (itemObj instanceof Map) {
                String label = asString(((Map<?, ?>) itemObj).get('label'))
                if (label != null && !label.trim().isEmpty() && !labels.contains(label)) {
                    labels.add(label)
                }
            }
        }
        labels
    }

    private static List<String> metadataSummary(Object value) {
        if (!(value instanceof Collection)) {
            return Collections.<String>emptyList()
        }
        List<String> entries = new ArrayList<String>()
        for (Object itemObj : (Collection<?>) value) {
            if (!(itemObj instanceof Map)) {
                continue
            }
            Map<?, ?> item = (Map<?, ?>) itemObj
            String namespace = asString(item.get('namespace'))
            String key = asString(item.get('key'))
            String metadataValue = asString(item.get('value'))
            if (key == null || key.trim().isEmpty()) {
                continue
            }
            String label = namespace == null || namespace.trim().isEmpty() ? key : "${namespace}.${key}".toString()
            entries.add("${label}=${metadataValue ?: ''}".toString())
        }
        entries
    }

    private static String formatValue(Object value) {
        if (value instanceof Collection) {
            Collection<?> values = (Collection<?>) value
            if (values.isEmpty()) {
                return ''
            }
            return values.collect {Object item -> String.valueOf(item) }.join('; ')
        }
        String.valueOf(value)
            .replace('\n', ' ')
    }

    private static String asString(Object value) {
        value == null ? null : String.valueOf(value)
    }
}

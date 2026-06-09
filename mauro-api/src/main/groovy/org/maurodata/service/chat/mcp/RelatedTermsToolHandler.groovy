package org.maurodata.service.chat.mcp

import groovy.transform.CompileStatic
import jakarta.inject.Singleton
import org.maurodata.service.chat.semantic.RelatedTerm
import org.maurodata.service.chat.semantic.RelatedTermsResult
import org.maurodata.service.chat.semantic.RelatedTermsService
import org.maurodata.service.chat.semantic.TermExpansionPrompt

@CompileStatic
@Singleton
@McpToolDefinition(
    name = 'related_terms',
    description = 'Find or prepare semantically related terms for a word or phrase in a Mauro context.',
    purpose = 'Return stored or generated related terms for a word or phrase so the model can refine or expand a later catalogue search. If the user asks for related terms as part of a search task, this tool is the expansion step, not the final answer.',
    useWhen = [
        'users ask for related terms, synonyms, aliases, alternative search terms, or semantic neighbours',
        'a catalogue search query needs expansion',
        'a user asks what other terms might be used for a concept',
        'users ask for related terms as part of a catalogue search task; after this tool returns, call catalogue_search next unless they explicitly ask only for suggestions and no search'
    ],
    avoidWhen = [
        'the user asks for live catalogue items and the search term is already clear',
        'the user asks for a definition that can be answered by Mauro glossary guidance'
    ],
    examples = [
        'related terms for diabetes',
        'what else might smoking be called?',
        'expand maternity care for catalogue search',
        'suggest related terms for maternity care and then search'
    ],
    inputSchema = '{"type":"object","properties":{"text":{"type":"string","description":"Word or phrase to expand or look up semantically"},"context":{"type":"string","description":"Domain context for term expansion. If the user phrase has an obvious domain, pass that domain context, for example \\"maternity, pregnancy, birth, antenatal, postnatal care in Mauro Data Mapper catalogue search\\" rather than only the generic default."},"locale":{"type":"string","description":"Locale/spelling preference; defaults to British"},"embeddingProvider":{"type":"string","description":"Embedding provider to use or filter by, for example openai or ollama"},"embeddingModel":{"type":"string","description":"Embedding model to use or filter by. Vectors are only comparable within the same model."},"generationModel":{"type":"string","description":"LLM model to use when generated related terms are needed; defaults to the configured chat default model"},"max":{"type":"integer","minimum":1,"maximum":100,"description":"Maximum related terms to return"},"generate":{"type":"boolean","description":"When true, generate related terms with the configured or requested model if none are stored; defaults to true"},"includePrompts":{"type":"boolean","description":"When true, include LLM prompt templates used for term generation; defaults to false"}},"required":["text"]}'
)
class RelatedTermsToolHandler extends AbstractAnnotatedToolHandler {

    private final RelatedTermsService relatedTermsService

    RelatedTermsToolHandler(RelatedTermsService relatedTermsService) {
        super(RelatedTermsToolHandler)
        this.relatedTermsService = relatedTermsService
    }

    @Override
    protected Map<String, Object> doInvoke(Map<String, Object> arguments) {
        String text = asString(arguments.get('text'))
        if (text == null || text.trim().isEmpty()) {
            throw new IllegalArgumentException('related_terms requires text')
        }
        RelatedTermsResult result = relatedTermsService.lookup(
            text,
            asString(arguments.get('context')),
            asString(arguments.get('locale')),
            asString(arguments.get('embeddingProvider')),
            asString(arguments.get('embeddingModel')),
            asString(arguments.get('generationModel')),
            asInteger(arguments.get('max'), 10),
            asBoolean(arguments.get('generate'), true),
            asBoolean(arguments.get('includePrompts'), false)
        )
        toMap(result)
    }

    @Override
    String modelText(Map<String, Object> result) {
        List<?> terms = result.get('terms') instanceof List ? (List<?>) result.get('terms') : []
        List<?> prompts = result.get('prompts') instanceof List ? (List<?>) result.get('prompts') : []
        List<String> returnedTerms = new ArrayList<String>()
        for (Object termObj : terms) {
            if (termObj instanceof Map) {
                Map<?, ?> term = (Map<?, ?>) termObj
                returnedTerms.add(String.valueOf("${term.get('text')} (${term.get('relation') ?: 'related'}, score: ${term.get('score') ?: 0})"))
            }
        }
        List<String> promptSummaries = new ArrayList<String>()
        for (Object promptObj : prompts) {
            if (promptObj instanceof Map) {
                Map<?, ?> prompt = (Map<?, ?>) promptObj
                promptSummaries.add(String.valueOf("${prompt.get('category')}: prompt template available"))
            }
        }
        List<String> searchExpression = buildSearchExpression(result.get('inputText'), terms)

        renderModelTextSections([
            'Tool Call Status'   : ['Tool related_terms succeeded.'],
            'Result Metadata'    : [
                "Input text: ${result.get('inputText')}",
                "Context: ${result.get('context')}",
                "Lookup strategy: ${result.get('lookupStrategy')}",
                "Embedding provider: ${result.get('embeddingProvider') ?: 'not specified'}",
                "Embedding model: ${result.get('embeddingModel') ?: 'not specified'}"
            ],
            'Returned Data'      : returnedTerms ?: ['No stored related terms were found.'],
            'Suggested Search Expression': searchExpression,
            'Expansion Prompts'  : promptSummaries,
            'Answer Instructions': [
                'If the user only asked for suggestions, present the related terms clearly.',
                'If the user asked for terms as part of a search task, or framed this as query expansion for a catalogue search, use these terms to call catalogue_search next rather than only telling the user to search themselves.',
                'When the user explicitly asks to use related terms in a search task, carry the looked-up related terms into catalogue_search; do not replace them with a narrower single-term search.',
                'When calling catalogue_search after this tool, use the Suggested Search Expression as the catalogue_search searchTerm unless there is a clear tool-schema reason not to. Preserve any domainTypes implied by already-injected representation skills.',
                'If you omit any looked-up related terms from the search expression because there are too many or they are irrelevant, briefly state that you used a subset and why.',
                'If no terms are present but prompts are available, say that term-generation prompts are available for the next indexing/expansion step.',
                'Do not claim vector similarity was used unless the lookup strategy or result explicitly says so.'
            ],
            'Completion Guidance': [
                'If the user only asked for related-term suggestions, answer now from this result.',
                'If the user asked for related terms as part of a search task, this result is not the final answer; call catalogue_search next using the Suggested Search Expression.',
                'Do not call related_terms again with identical text and context unless the user asks for a different expansion.'
            ]
        ] as Map<String, Object>)
    }

    private static Map<String, Object> toMap(RelatedTermsResult result) {
        [
            inputText          : result.inputText,
            normalizedInputText: result.normalizedInputText,
            context            : result.context,
            locale             : result.locale,
            lookupStrategy     : result.lookupStrategy,
            embeddingProvider  : result.embeddingProvider,
            embeddingModel     : result.embeddingModel,
            terms              : result.terms.collect {RelatedTerm term -> termToMap(term)},
            prompts            : result.prompts.collect {TermExpansionPrompt prompt -> [category: prompt.category, prompt: prompt.prompt] as Map<String, Object>}
        ] as Map<String, Object>
    }

    private static Map<String, Object> termToMap(RelatedTerm term) {
        [
            text          : term.text,
            normalizedText: term.normalizedText,
            relation      : term.relation,
            source        : term.source,
            sourceModel   : term.sourceModel,
            score         : term.score,
            metadata      : term.metadata ?: [:]
        ] as Map<String, Object>
    }

    private static List<String> buildSearchExpression(Object inputText, List<?> terms) {
        List<String> values = new ArrayList<String>()
        String input = asString(inputText)
        if (input != null && !input.trim().isEmpty()) {
            values.add(quoteIfNeeded(input.trim()))
        }
        for (Object termObj : terms) {
            if (!(termObj instanceof Map)) {
                continue
            }
            Object textObj = ((Map<?, ?>) termObj).get('text')
            String text = asString(textObj)
            if (text != null && !text.trim().isEmpty()) {
                String value = quoteIfNeeded(text.trim())
                if (!values.contains(value)) {
                    values.add(value)
                }
            }
        }
        if (values.isEmpty()) {
            return []
        }
        ['Use this as a candidate catalogue_search searchTerm: ' + values.take(8).join(' OR ')]
    }

    private static String quoteIfNeeded(String text) {
        text.contains(' ') ? '"' + text.replace('"', '\\"') + '"' : text
    }

    private static String asString(Object value) {
        value == null ? null : String.valueOf(value)
    }

    private static int asInteger(Object value, int fallback) {
        if (value == null) {
            return fallback
        }
        if (value instanceof Number) {
            return ((Number) value).intValue()
        }
        String text = String.valueOf(value)
        text.trim().isEmpty() ? fallback : Integer.valueOf(text)
    }

    private static boolean asBoolean(Object value, boolean fallback) {
        value == null ? fallback : Boolean.valueOf(String.valueOf(value))
    }
}

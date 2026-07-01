package org.maurodata.plugin.chat.semantic

import org.maurodata.service.chat.semantic.*
import org.maurodata.service.search.*
import org.maurodata.service.semantic.*

import groovy.transform.CompileStatic
import io.micronaut.context.annotation.Value
import jakarta.inject.Singleton

@CompileStatic
@Singleton
class RelatedTermsApiService implements RelatedTermsService {

    private final SemanticTermStore semanticTermStore
    private final TermExpansionPromptFactory promptFactory
    private final TermExpansionOutputParser outputParser
    private final TextGenerationService textGenerationService
    private final String defaultContext
    private final String defaultEmbeddingProvider
    private final String defaultEmbeddingModel
    private final String defaultGenerationModel
    private final List<String> defaultGenerationCategories
    private final int maxGenerationCategories

    RelatedTermsApiService(
        SemanticTermStore semanticTermStore,
        TermExpansionPromptFactory promptFactory,
        TermExpansionOutputParser outputParser,
        TextGenerationService textGenerationService,
        @Value('${chat.semantic.default-context:Mauro Data Mapper metadata catalogue search}') String defaultContext,
        @Value('${chat.semantic.default-embedding-provider:}') String defaultEmbeddingProvider,
        @Value('${chat.semantic.default-embedding-model:}') String defaultEmbeddingModel,
        @Value('${chat.providers.default-model:llama3.2:1b}') String defaultGenerationModel,
        @Value('${chat.semantic.related-terms.default-categories:synonym,neighbours,searchKeywords}') String defaultGenerationCategories,
        @Value('${chat.semantic.related-terms.max-generation-categories:2}') Integer maxGenerationCategories
    ) {
        this.semanticTermStore = semanticTermStore
        this.promptFactory = promptFactory
        this.outputParser = outputParser
        this.textGenerationService = textGenerationService
        this.defaultContext = defaultContext
        this.defaultEmbeddingProvider = defaultEmbeddingProvider
        this.defaultEmbeddingModel = defaultEmbeddingModel
        this.defaultGenerationModel = defaultGenerationModel
        this.defaultGenerationCategories = parseCategories(defaultGenerationCategories)
        this.maxGenerationCategories = Math.max(maxGenerationCategories ?: 2, 1)
    }

    @Override
    RelatedTermsResult lookup(String inputText, String context, String locale, String embeddingProvider, String embeddingModel, String generationModel, int max, boolean generateWhenMissing, boolean includePrompts) {
        String resolvedContext = blankToDefault(context, defaultContext)
        String resolvedLocale = blankToDefault(locale, 'British')
        String resolvedProvider = blankToDefault(embeddingProvider, defaultEmbeddingProvider)
        String resolvedModel = blankToDefault(embeddingModel, defaultEmbeddingModel)
        String resolvedGenerationModel = blankToDefault(generationModel, defaultGenerationModel)
        int resolvedMax = max < 1 ? 10 : Math.min(max, 100)

        List<RelatedTerm> matches = semanticTermStore.findSimilar(inputText, resolvedContext, resolvedProvider, resolvedModel, resolvedMax)
        if (matches.isEmpty() && generateWhenMissing) {
            List<RelatedTerm> generated = generateTerms(inputText, resolvedContext, resolvedLocale, resolvedGenerationModel, resolvedMax)
            semanticTermStore.saveTerms(inputText, resolvedContext, generated)
            matches = generated.take(resolvedMax)
        }
        new RelatedTermsResult(
            inputText: inputText,
            normalizedInputText: InMemorySemanticTermStore.normalize(inputText),
            context: resolvedContext,
            locale: resolvedLocale,
            lookupStrategy: matches.isEmpty() ? 'LEXICAL_JACCARD_IN_MEMORY' : (generateWhenMissing ? 'GENERATED_OR_CACHED_TERMS' : 'LEXICAL_JACCARD_IN_MEMORY'),
            embeddingProvider: resolvedProvider,
            embeddingModel: resolvedModel,
            terms: matches,
            prompts: includePrompts ? promptFactory.prompts(inputText, resolvedContext, resolvedLocale) : []
        )
    }

    private List<RelatedTerm> generateTerms(String inputText, String context, String locale, String generationModel, int maxTerms) {
        Map<String, String> conceptPrompts = generationPromptMap(inputText, context, locale, defaultGenerationCategories.take(maxGenerationCategories))
        List<String> rawTerms = new ArrayList<String>()
        Map<String, String> relationByTerm = new LinkedHashMap<String, String>()
        for (Map.Entry<String, String> entry : conceptPrompts.entrySet()) {
            String output
            try {
                output = textGenerationService.generate(generationModel, entry.value)
            } catch (Throwable ignored) {
                continue
            }
            for (String term : outputParser.parseFlatList(output)) {
                String key = normalizeTerm(term)
                if (!key.isEmpty() && !relationByTerm.containsKey(key)) {
                    rawTerms.add(term.toLowerCase(Locale.ROOT))
                    relationByTerm.put(key, entry.key)
                }
            }
            if (rawTerms.size() >= Math.max(maxTerms * 2, 6)) {
                break
            }
        }

        List<String> removalTerms = buildRemovalTerms(context)
        List<String> filteredTerms = filterMatching(singleWords(removalTerms), rawTerms)
        List<RelatedTerm> out = new ArrayList<RelatedTerm>()
        Set<String> seen = new LinkedHashSet<String>()
        for (String term : filteredTerms) {
            String normalized = normalizeTerm(term)
            if (normalized.length() < 2 || !seen.add(normalized)) {
                continue
            }
            out.add(new RelatedTerm(
                text: term,
                normalizedText: normalized,
                relation: relationByTerm.get(normalized) ?: 'related',
                source: 'LLM_GENERATED',
                sourceModel: generationModel,
                score: BigDecimal.ONE,
                metadata: [
                    generationModel: generationModel
                ] as Map<String, Object>
            ))
        }
        out
    }

    private List<String> buildRemovalTerms(String context) {
        List<String> removal = new ArrayList<String>()
        removal.add('none')
        removal.addAll(outputParser.parseFlatList(context))
        removal.collect {String term -> term.toLowerCase(Locale.ROOT)}
    }

    private static Map<String, String> generationPromptMap(String concept, String context, String locale, List<String> categories) {
        Map<String, String> all = [
            synonym               : TermExpansionPromptFactory.synonym(concept, context, locale),
            neighbours            : TermExpansionPromptFactory.neighbours(concept, context, locale),
            hyponyms              : TermExpansionPromptFactory.hyponyms(concept, context, locale),
            usageSignals          : TermExpansionPromptFactory.usageSignals(concept, context, locale),
            euphemisms            : TermExpansionPromptFactory.euphemisms(concept, context, locale),
            alternativeExpressions: TermExpansionPromptFactory.alternativeExpressions(concept, context, locale),
            searchKeywords        : TermExpansionPromptFactory.searchKeywords(concept, context, locale),
            hypernyms             : TermExpansionPromptFactory.hypernyms(concept, context, locale)
        ] as Map<String, String>
        Map<String, String> selected = new LinkedHashMap<String, String>()
        for (String category : categories ?: []) {
            if (all.containsKey(category)) {
                selected.put(category, all.get(category))
            }
        }
        selected.isEmpty() ? all : selected
    }

    private static List<String> singleWords(List<String> list) {
        List<String> single = new ArrayList<String>()
        for (String words : list ?: []) {
            List<String> split = tokenizeRemoval(words)
            boolean concat = split.any {String word -> word.length() < 3}
            if (!concat) {
                for (String word : split) {
                    if (!single.contains(word)) {
                        single.add(word)
                    }
                }
            } else {
                String word = split.join(' ')
                if (!word.isEmpty() && !single.contains(word)) {
                    single.add(word)
                }
            }
        }
        single
    }

    private static List<String> tokenizeRemoval(String words) {
        if (words == null) {
            return []
        }
        words.toLowerCase(Locale.ROOT)
            .split(/[!@£$%^&*()_\-+=~`<,>.?\/:;"'\\| \t]+/)
            .findAll {String word -> !word.isEmpty()}
    }

    private static List<String> filterMatching(List<String> matching, List<String> from) {
        List<String> working = new ArrayList<String>(from ?: [])
        List<String> filtered = new ArrayList<String>(working.size())
        int f = 0
        while (f < working.size()) {
            String words = working.get(f)
            boolean removedOrTrimmed = false
            for (String word : matching ?: []) {
                if (word == null || word.trim().isEmpty()) {
                    continue
                }
                if (words == word || words.contains(' ' + word + ' ')) {
                    removedOrTrimmed = true
                    break
                }
                if (words.startsWith(word + ' ')) {
                    working.set(f, words.substring(word.length() + 1).trim())
                    removedOrTrimmed = true
                    break
                }
                if (words.endsWith(' ' + word)) {
                    working.set(f, words.substring(0, words.length() - word.length() - 1).trim())
                    removedOrTrimmed = true
                    break
                }
            }
            if (!removedOrTrimmed) {
                filtered.add(words)
                f++
            }
        }
        filtered
    }

    private static String normalizeTerm(String term) {
        InMemorySemanticTermStore.normalize(term)
    }

    private static String blankToDefault(String value, String defaultValue) {
        value == null || value.trim().isEmpty() ? defaultValue : value.trim()
    }

    private static List<String> parseCategories(String value) {
        if (value == null || value.trim().isEmpty()) {
            return ['synonym', 'neighbours', 'alternativeExpressions', 'searchKeywords', 'usageSignals'] as List<String>
        }
        value.split(/,/)
            .collect {String category -> category.trim()}
            .findAll {String category -> !category.isEmpty()}
    }
}

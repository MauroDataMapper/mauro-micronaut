package org.maurodata.service.chat.semantic

import groovy.transform.CompileStatic
import jakarta.inject.Singleton

@CompileStatic
@Singleton
class InMemorySemanticTermStore implements SemanticTermStore {

    private final List<RelatedTerm> terms = new ArrayList<RelatedTerm>()

    @Override
    void saveTerms(String inputText, String context, List<RelatedTerm> newTerms) {
        if (!newTerms) {
            return
        }
        synchronized (terms) {
            terms.addAll(newTerms)
        }
    }

    @Override
    List<RelatedTerm> findSimilar(String inputText, String context, String embeddingProvider, String embeddingModel, int max) {
        String normalizedInput = normalize(inputText)
        Set<String> queryTokens = tokens(normalizedInput)
        if (queryTokens.isEmpty()) {
            return []
        }

        List<RelatedTerm> snapshot
        synchronized (terms) {
            snapshot = new ArrayList<RelatedTerm>(terms)
        }

        List<RelatedTerm> scored = new ArrayList<RelatedTerm>()
        for (RelatedTerm term : snapshot) {
            if (!sameEmbeddingSpace(term, embeddingProvider, embeddingModel)) {
                continue
            }
            BigDecimal score = lexicalScore(queryTokens, term)
            if (score > BigDecimal.ZERO) {
                scored.add(copyWithScore(term, score))
            }
        }
        scored.sort {RelatedTerm left, RelatedTerm right ->
            int scoreCompare = right.score <=> left.score
            scoreCompare != 0 ? scoreCompare : left.text <=> right.text
        }
        scored.take(Math.max(max, 0))
    }

    private static boolean sameEmbeddingSpace(RelatedTerm term, String provider, String model) {
        Object termProvider = term.metadata?.get('embeddingProvider')
        Object termModel = term.metadata?.get('embeddingModel')
        if (provider != null && !provider.trim().isEmpty() && termProvider != null && termProvider != provider) {
            return false
        }
        if (model != null && !model.trim().isEmpty() && termModel != null && termModel != model) {
            return false
        }
        true
    }

    private static BigDecimal lexicalScore(Set<String> queryTokens, RelatedTerm term) {
        Set<String> termTokens = tokens(term.normalizedText ?: term.text)
        if (termTokens.isEmpty()) {
            return BigDecimal.ZERO
        }
        Set<String> intersection = new LinkedHashSet<String>(queryTokens)
        intersection.retainAll(termTokens)
        if (intersection.isEmpty()) {
            return BigDecimal.ZERO
        }
        Set<String> union = new LinkedHashSet<String>(queryTokens)
        union.addAll(termTokens)
        BigDecimal.valueOf(intersection.size()).divide(BigDecimal.valueOf(union.size()), 6, BigDecimal.ROUND_HALF_UP)
    }

    private static RelatedTerm copyWithScore(RelatedTerm term, BigDecimal score) {
        new RelatedTerm(
            text: term.text,
            normalizedText: term.normalizedText,
            relation: term.relation,
            source: term.source,
            sourceModel: term.sourceModel,
            score: score,
            metadata: term.metadata ?: [:]
        )
    }

    static String normalize(String text) {
        if (text == null) {
            return ''
        }
        text.toLowerCase(Locale.ROOT)
            .replaceAll(/[^a-z0-9]+/, ' ')
            .trim()
    }

    static Set<String> tokens(String text) {
        String normalized = normalize(text)
        if (normalized.isEmpty()) {
            return Collections.<String>emptySet()
        }
        new LinkedHashSet<String>(normalized.split(/\s+/).findAll {String token -> token.length() > 1})
    }
}

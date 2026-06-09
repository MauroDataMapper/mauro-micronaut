package org.maurodata.service.chat.semantic

import groovy.transform.CompileStatic
import jakarta.inject.Singleton

@CompileStatic
@Singleton
class TermExpansionPromptFactory {

    List<TermExpansionPrompt> prompts(String concept, String context, String locale = 'British') {
        [
            prompt('synonym', synonym(concept, context, locale)),
            prompt('neighbours', neighbours(concept, context, locale)),
            prompt('hypernyms', hypernyms(concept, context, locale)),
            prompt('hyponyms', hyponyms(concept, context, locale)),
            prompt('euphemisms', euphemisms(concept, context, locale)),
            prompt('alternativeExpressions', alternativeExpressions(concept, context, locale)),
            prompt('searchKeywords', searchKeywords(concept, context, locale)),
            prompt('operationalRules', operationalRules(concept, context, locale)),
            prompt('implicit', implicit(concept, context, locale)),
            prompt('usageSignals', usageSignals(concept, context, locale))
        ] as List<TermExpansionPrompt>
    }

    static String synonym(String concept, String context, String locale = 'British') {
        """${locale} spelling only. Using only terms that could appear in technical documentation or user queries, list precise true synonyms of "${concept}" within the context of "${context}".
If the concept of "${concept}" doesn't have any true synonyms, output NONE.
Output a single flat bullet list with no explanations. Return only the strongly specific, domain-relevant concepts as short noun phrases.
"""
    }

    static String neighbours(String concept, String context, String locale = 'British') {
        """${locale} spelling only. Without any explanations or asides or examples or caveats or conditionals, excluding true synonyms, list concepts that are closely related, strongly associated, or frequently co-mentioned with "${concept}" within "${context}".
If the concept of "${concept}" doesn't have any concepts that are closely related, strongly associated, or frequently co-mentioned, output NONE.
Output a single flat bullet list with no explanations. Return only the strongly specific, domain-relevant concepts as short noun phrases.
"""
    }

    static String hypernyms(String concept, String context, String locale = 'British') {
        """${locale} spelling only. Returning only domain-relevant hierarchical parents, identify and list broader categories, parent concepts or hypernyms of "${concept}" within "${context}".
Exclude hypernyms that are specifically "${concept}" related yet keep the hypernyms that are not related to "${concept}".
If the concept of "${concept}" doesn't have any hypernyms, output NONE.
Output a single flat bullet list with no explanations. Return only the strongly specific, domain-relevant concepts as short noun phrases.
"""
    }

    static String hyponyms(String concept, String context, String locale = 'British') {
        """${locale} spelling only. List specific subtypes, examples, or narrower concepts of "${concept}" within "${context}".
If the concept of "${concept}" doesn't have hyponyms, output NONE.
Output a single flat bullet list with no explanations. Return only the strongly specific, domain-relevant concepts as short noun phrases.
"""
    }

    static String euphemisms(String concept, String context, String locale = 'British') {
        """${locale} spelling only. Without any explanations or asides or examples or caveats or conditionals, list euphemisms of "${concept}" within the context of "${context}".
If the concept of "${concept}" doesn't have any euphemisms, output NONE.
Output a single flat bullet list with no explanations. Return only the strongly specific, domain-relevant concepts as short noun phrases.
"""
    }

    static String alternativeExpressions(String concept, String context, String locale = 'British') {
        """${locale} spelling only. List alternative formulations or ways of expressing "${concept}" in "${context}".
Output a single flat bullet list with no explanations. Return only the strongly specific, domain-relevant concepts as short noun phrases.
"""
    }

    static String searchKeywords(String concept, String context, String locale = 'British') {
        """${locale} spelling only. Without any explanations or asides or examples or caveats or conditionals, excluding any broad wide weakly specific hypernyms,
focussing on particular words that data-scientists will search for,
generate a concise bullet list of short noun phrases related to the search for "${concept}" in the context of "${context}".
Output a single flat bullet list with no explanations. Return only the strongly specific, domain-relevant concepts as short noun phrases.
"""
    }

    static String operationalRules(String concept, String context, String locale = 'British') {
        """${locale} spelling only. Focussing on what a rule-based system would need know to make inferences,
provide a simple definition of "${concept}" in "${context}". Summarise how the concept is measured, quantified, or derived
If the concept of "${concept}" has no rules that can be inferred, output NONE.
"""
    }

    static String implicit(String concept, String context, String locale = 'British') {
        """${locale} spelling only. Focussing on what a human expert would take for granted, list implicit meanings,
assumptions, or unstated implications associated with "${concept}" in "${context}".
If the concept of "${concept}" doesn't have any hidden implicit meanings,
assumptions, or unstated implications, output NONE.
Output a single flat bullet list with no explanations.
"""
    }

    static String usageSignals(String concept, String context, String locale = 'British') {
        """${locale} spelling only. Identify and list noun phrases that implicitly refer to "${concept}" in "${context}", without naming it directly.
If the concept of "${concept}" doesn't have any noun phrases that implicitly refer to it, output NONE.
Output a single flat bullet list with no explanations. Return only the strongly specific, domain-relevant concepts as short noun phrases.
"""
    }

    private static TermExpansionPrompt prompt(String category, String prompt) {
        new TermExpansionPrompt(category: category, prompt: prompt.trim())
    }
}

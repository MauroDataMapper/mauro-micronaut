package org.maurodata.plugin.chat.semantic

import org.maurodata.service.chat.semantic.*
import org.maurodata.service.search.*
import org.maurodata.service.semantic.*

import groovy.transform.CompileStatic
import jakarta.inject.Singleton

@CompileStatic
@Singleton
class TermExpansionOutputParser {

    List<String> parseFlatList(String output) {
        if (output == null || output.trim().isEmpty()) {
            return []
        }
        String trimmed = output.trim()
        if ('NONE'.equalsIgnoreCase(trimmed)) {
            return []
        }
        List<String> terms = new ArrayList<String>()
        for (String line : trimmed.split(/\r?\n/)) {
            String term = normalizeLine(line)
            if (!term.isEmpty() && !'NONE'.equalsIgnoreCase(term)) {
                terms.add(term)
            }
        }
        dedupe(terms)
    }

    private static String normalizeLine(String line) {
        if (line == null) {
            return ''
        }
        String text = line.trim()
        text = text.replaceFirst(/^[-*•]\s*/, '')
        text = text.replaceFirst(/^\d+[\.)]\s*/, '')
        text.trim()
    }

    private static List<String> dedupe(List<String> terms) {
        Set<String> seen = new LinkedHashSet<String>()
        List<String> out = new ArrayList<String>()
        for (String term : terms) {
            String key = term.toLowerCase(Locale.ROOT)
            if (seen.add(key)) {
                out.add(term)
            }
        }
        out
    }
}

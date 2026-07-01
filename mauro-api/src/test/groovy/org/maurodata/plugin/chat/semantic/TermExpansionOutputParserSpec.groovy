package org.maurodata.plugin.chat.semantic

import org.maurodata.service.chat.semantic.*

import org.maurodata.plugin.chat.semantic.*

import spock.lang.Specification

class TermExpansionOutputParserSpec extends Specification {

    TermExpansionOutputParser parser = new TermExpansionOutputParser()

    void 'parses flat bullet lists and removes duplicates'() {
        expect:
        parser.parseFlatList('''
            - diabetes mellitus
            * diabetic condition
            1. diabetes mellitus
            2) blood glucose disorder
        ''') == ['diabetes mellitus', 'diabetic condition', 'blood glucose disorder']
    }

    void 'none returns no terms'() {
        expect:
        parser.parseFlatList('NONE') == []
    }
}

package org.maurodata.service.chat.mcp

import spock.lang.Specification

class SearchToolHandlerSpec extends Specification {

    void 'model text asks for search intent choice for unexpanded keyword searches'() {
        given:
        SearchToolHandler handler = new SearchToolHandler(null, null)

        when:
        String modelText = handler.modelText([
            searchTerm  : 'diabetes',
            domainTypes : ['DataModel'],
            count       : 42,
            max         : 20,
            offset      : 0,
            nextOffset  : 20,
            hasMore     : true,
            withGuidance: true,
            searchIntent  : 'unsaid',
            items       : [
                [label: 'Adult Diabetes Assessment Form', domainType: 'DataModel', id: 'dm-1', description: '']
            ]
        ] as Map<String, Object>)

        then:
        modelText.contains('## Clarification Guidance')
        modelText.contains('keep the exact keyword search')
        modelText.contains('expand the keyword expression with related terms and alternatives')
        modelText.contains('searchIntent":"unsaid"')
        modelText.contains('withGuidance":true')
        modelText.contains('Tell the user the exact catalogue search term/expression used: diabetes')
        modelText.contains('When asking the user how to proceed, explicitly state that the current search used searchTerm "diabetes" and domainTypes [DataModel].')
        modelText.contains('Also state the current exact-term result count: 1 returned on this page, 42 total matching catalogue items.')
        modelText.contains('Still show the returned results from this page using the table format from Answer Instructions')
        modelText.contains('show the returned page results and then ask the focused choice question')
        modelText.contains('Even when asking that focused choice question, include the exact search term/expression and domain type filter used.')
    }

    void 'model text does not ask for search intent choice when search already expanded'() {
        given:
        SearchToolHandler handler = new SearchToolHandler(null, null)

        when:
        String modelText = handler.modelText([
            searchTerm  : 'diabetes OR diabetic',
            domainTypes : ['DataModel'],
            count       : 3,
            max         : 20,
            offset      : 0,
            nextOffset  : 3,
            hasMore     : false,
            withGuidance: true,
            searchIntent  : 'expanded',
            items       : [
                [label: 'Adult Diabetes Assessment Form', domainType: 'DataModel', id: 'dm-1', description: '']
            ]
        ] as Map<String, Object>)

        then:
        !modelText.contains('## Clarification Guidance')
        modelText.contains('Tell the user the exact catalogue search term/expression used: diabetes OR diabetic')
    }

    void 'model text provides OR retry call for zero result AND-style searches'() {
        given:
        SearchToolHandler handler = new SearchToolHandler(null, null)

        when:
        String modelText = handler.modelText([
            searchTerm  : 'maternity birth child parent pregnancy fetal baby infant newborn',
            domainTypes : ['DataModel'],
            count       : 0,
            max         : 20,
            offset      : 0,
            nextOffset  : 0,
            hasMore     : false,
            withGuidance: true,
            searchIntent  : 'exact',
            items       : []
        ] as Map<String, Object>)

        then:
        modelText.contains('This exact search returned no matches and appears to require all supplied words to be present.')
        modelText.contains('keyword/full-text search')
        modelText.contains('treats unquoted words as AND terms')
        modelText.contains('do not rewrite the terms semantically')
        modelText.contains('Retry with this exact OR keyword search call using the same user-supplied terms')
        modelText.contains('"searchTerm":"maternity OR birth OR child OR parent OR pregnancy OR fetal OR baby OR infant OR newborn"')
        modelText.contains('"domainTypes":["DataModel"]')
        modelText.contains('"searchIntent":"expanded"')
    }

}

package org.maurodata.plugin.chat.mcp

import org.maurodata.service.chat.mcp.*

import org.maurodata.plugin.chat.mcp.*

import spock.lang.Specification

class SearchToolHandlerSpec extends Specification {

    void 'model text provides final response search intent choice for unexpanded keyword searches'() {
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
        modelText.indexOf('## Answer Instructions') < modelText.indexOf('## Continuation Gate')
        modelText.contains('## Continuation Gate')
        modelText.contains('Choose exactly one branch flag for this tool result: CW or FR.')
        modelText.contains('CW = Continue Workflow')
        modelText.contains('FR = Final Response')
        modelText.contains('## Continue Workflow branch')
        modelText.contains('CW: Present any required current result briefly if needed, then execute the exact matching action in this turn.')
        modelText.contains('CW: Treat the current user request as sufficient confirmation for its requested steps.')
        modelText.contains('CW: Phrases such as "then take a closer look at the first one" are requested actions, not optional actions to offer.')
        modelText.contains('CW: Continue by calling the exact action rather than presenting that action as a user choice.')
        modelText.contains('CW: Skip FR instructions, pagination offers, refinement choices, and search-expansion questions')
        modelText.contains('## Final Response branch')
        modelText.contains('keeping the exact keyword search')
        modelText.contains('expanding the keyword expression with related terms and alternatives')
        modelText.contains('searchIntent":"unsaid"')
        modelText.contains('withGuidance":true')
        modelText.contains('COMMON: Tell the user the exact catalogue search term/expression used: diabetes')
        modelText.contains('FR: For a final response that asks about search expansion, state that the current search used searchTerm "diabetes" and domainTypes [DataModel].')
        modelText.contains('FR: Also state the current exact-term result count: 1 returned on this page, 42 total matching catalogue items.')
        modelText.contains('FR: Show the returned results from this page using the table format from Answer Instructions')
        modelText.contains('Use exactly these columns when possible: Label, Type, ID, Description.')
        modelText.contains('Escape pipe characters inside Markdown table cell values as \\|.')
        modelText.contains('When the user refers to an ordinal result such as the first one')
        modelText.contains('show the returned page results and ask the focused search-expansion choice question')
        modelText.contains('## END')
        modelText.contains('END: Use COMMON plus exactly one branch flag: CW or FR.')
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
        !modelText.contains('FR: For search expansion')
        modelText.contains('COMMON: Tell the user the exact catalogue search term/expression used: diabetes OR diabetic')
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
        modelText.contains('FR: This exact search returned no matches and appears to require all supplied words to be present.')
        modelText.contains('keyword/full-text search')
        modelText.contains('treats unquoted words as AND terms')
        modelText.contains('CW: Use the OR retry Follow-up Action to search the same user-supplied terms as alternatives.')
        modelText.contains('CW: Retry with this exact OR keyword search call using the same user-supplied terms')
        modelText.contains('"searchTerm":"maternity OR birth OR child OR parent OR pregnancy OR fetal OR baby OR infant OR newborn"')
        modelText.contains('"domainTypes":["DataModel"]')
        modelText.contains('"searchIntent":"expanded"')
    }

    void 'registry appends derived DataModel resource actions by default'() {
        given:
        ToolHandler handler = Stub(ToolHandler)
        handler.name() >> 'mauro_keyword_search'
        handler.description() >> 'Search catalogue'
        handler.inputSchema() >> [type: 'object']
        handler.routing() >> [:]
        handler.annotations() >> [:]
        handler.invoke(_ as Map<String, Object>) >> [
            items: [
                [label: 'Braden Risk Assessment', domainType: 'DataModel', id: 'dm-1']
            ]
        ]
        handler.modelText(_ as Map<String, Object>) >> 'base model text'
        LocalMcpRegistry registry = new LocalMcpRegistry([handler] as List<ToolHandler>, new ResultGuidanceService())

        when:
        ToolInvocationResult result = registry.invokeDetailed('mauro_keyword_search', [:] as Map<String, Object>)

        then:
        result.modelText.contains('base model text')
        result.modelText.contains('## Available Next Actions')
        result.modelText.contains('Read result 1 DataModel Braden Risk Assessment')
        result.modelText.contains('mauro-api://http-get/api/dataModels/dm-1')
        result.modelText.contains('execute that exact action in this turn')
        result.modelText.contains('The current user request is sufficient confirmation')
        result.modelText.contains('compare the requested-action checklist with the exact actions above')
    }

    void 'registry suppresses derived actions when withGuidance is false'() {
        given:
        ToolHandler handler = Stub(ToolHandler)
        handler.name() >> 'mauro_keyword_search'
        handler.description() >> 'Search catalogue'
        handler.inputSchema() >> [type: 'object']
        handler.routing() >> [:]
        handler.annotations() >> [:]
        handler.invoke(_ as Map<String, Object>) >> [
            items: [
                [label: 'Braden Risk Assessment', domainType: 'DataModel', id: 'dm-1']
            ]
        ]
        handler.modelText(_ as Map<String, Object>) >> 'base model text'
        LocalMcpRegistry registry = new LocalMcpRegistry([handler] as List<ToolHandler>, new ResultGuidanceService())

        when:
        ToolInvocationResult result = registry.invokeDetailed('mauro_keyword_search', [withGuidance: false] as Map<String, Object>)

        then:
        result.output.withGuidance == false
        result.modelText == 'base model text'
    }

    void 'combined search model text includes retrieval evidence'() {
        given:
        MauroSearchToolHandler handler = new MauroSearchToolHandler(null, null)

        when:
        String modelText = handler.modelText([
            searchTerm       : 'diabetes',
            domainTypes      : ['DataModel'],
            count            : 1,
            max              : 5,
            offset           : 0,
            nextOffset       : 1,
            hasMore          : false,
            withGuidance     : true,
            keywordCount     : 1,
            semanticAvailable: true,
            semanticRan      : true,
            semanticCount    : 1,
            mergedCount      : 1,
            items            : [
                [
                    label            : 'Adult Diabetes Form',
                    domainType       : 'DataModel',
                    id               : 'dm-1',
                    description      : '',
                    keywordRank      : 1,
                    semanticRank     : 2,
                    hybridScore      : 0.038,
                    semanticScore    : 0.82,
                    evidenceDetails  : [
                        [match: 'keyword', confidence: 'rank 1'],
                        [match: 'semantic', confidence: 'rank 2'],
                        [match: 'label', confidence: [similarity: 0.82, weighted: 1.107]]
                    ]
                ]
            ]
        ] as Map<String, Object>)

        then:
        modelText.contains('Keyword rank: 1')
        modelText.contains('Semantic rank: 2')
        modelText.contains('Hybrid score: 0.038')
        modelText.contains('Evidence: keyword (rank 1) | semantic (rank 2) | label (similarity=0.82, weighted=1.107)')
        modelText.contains('## Retrieval Evidence For Assistant')
        modelText.contains('Do not mention internal retrieval evidence')
        modelText.contains('Use columns Label, Type, ID, Description.')
    }

}

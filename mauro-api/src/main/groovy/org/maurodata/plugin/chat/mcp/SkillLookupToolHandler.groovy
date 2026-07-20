package org.maurodata.plugin.chat.mcp

import org.maurodata.service.chat.mcp.*

import groovy.transform.CompileStatic
import jakarta.inject.Singleton
import org.maurodata.service.chat.ChatPromptAssetDefinition
import org.maurodata.service.chat.ChatPromptAssetService

@CompileStatic
@Singleton
@McpToolDefinition(
    name = 'mauro_skill',
    description = 'Look up Mauro assistant skills, documentation guidance, glossary context, and usage guidance by id or query.',
    purpose = 'Retrieve modular Mauro guidance skills, including documentation guidance, glossary context, terminology help, data model exploration guidance, and search strategy.',
    useWhen = [
        'the routing index says a specific skill applies',
        'answering Mauro documentation, installation, configuration, Docker, administration, how-to, glossary, or usage-guide questions',
        'answering a Mauro-specific question that needs guidance beyond a direct catalogue search',
        'resolving answer ambiguity by looking up relevant skills and their guidance to choose the best one to apply'
    ],
    avoidWhen = [
        'finding live catalogue content when mauro_search can answer directly',
        'the previous tool result already contains enough information to answer'
    ],
    examples = [
        'look up mauro-docs-guide for installation documentation',
        'look up mauro-glossary for a Mauro concept definition',
        'look up mauro-data-model-explorer for model structure guidance'
    ],
    inputSchema = '{"type":"object","properties":{"id":{"type":"string","description":"Exact skill id to fetch"},"query":{"type":"string","description":"Search terms for relevant skills"},"list":{"type":"boolean","description":"When true, return all available skills"},"includeInstruction":{"type":"boolean","description":"Whether to include full instruction text; defaults to true"}}}'
)
class SkillLookupToolHandler extends AbstractAnnotatedToolHandler {

    private final ChatPromptAssetService promptAssetService

    SkillLookupToolHandler(ChatPromptAssetService promptAssetService) {
        super(SkillLookupToolHandler)
        this.promptAssetService = promptAssetService
    }

    @Override
    protected Map<String, Object> doInvoke(Map<String, Object> arguments) {
        String id = asString(arguments.get('id'))
        String query = extractQuery(arguments)
        boolean includeInstruction = asBoolean(arguments.get('includeInstruction'), true)

        List<ChatPromptAssetDefinition> matches
        boolean broadened = false
        if (id != null && !id.trim().isEmpty()) {
            ChatPromptAssetDefinition skill = promptAssetService.findAsset(id)
            matches = skill == null || !isSkill(skill) ? [] : [skill]
        } else if (isListRequest(arguments) || query == null || query.trim().isEmpty()) {
            matches = lookupSkills()
        } else {
            matches = promptAssetService.searchAssets(query)
                .findAll {ChatPromptAssetDefinition skill -> isSkill(skill)}
            if (matches.isEmpty()) {
                broadened = true
                matches = lookupSkills()
            }
        }

        [
            query      : query,
            broadened  : broadened,
            count      : matches.size(),
            skills     : matches.collect {ChatPromptAssetDefinition skill -> toMap(skill, includeInstruction)}
        ] as Map<String, Object>
    }

    @Override
    String modelText(Map<String, Object> result) {
        List<?> skills = result.get('skills') instanceof List ? (List<?>) result.get('skills') : Collections.emptyList()
        int totalCount = asInteger(result.get('count')) ?: skills.size()
        boolean broadened = Boolean.TRUE.equals(result.get('broadened'))

        List<String> status = [
            'Tool mauro_skill succeeded.'
        ]
        List<String> metadata = [
            "Available assistant skills found: ${totalCount}",
            "Broadened result: ${broadened}"
        ] as List<String>
        List<String> interpretation = new ArrayList<String>()
        if (broadened) {
            interpretation.add('No exact skill matched the query, so the result was broadened to all available skills.')
            interpretation.add('Choose the most relevant skill from this list if one applies.')
        }

        List<String> output = new ArrayList<String>()
        for (int i = 0; i < skills.size(); i++) {
            Object skillObj = skills.get(i)
            if (!(skillObj instanceof Map)) {
                continue
            }
            @SuppressWarnings('unchecked')
            Map<String, Object> skill = (Map<String, Object>) skillObj
            StringBuilder line = new StringBuilder(512)
            line.append(i + 1)
                .append('. ')
                .append(asText(skill.get('name'), 'Unnamed skill'))
                .append(' (id: ')
                .append(asText(skill.get('id'), 'unknown'))
                .append(') - ')
                .append(asText(skill.get('description'), 'No description'))
            output.add(line.toString())
            List<String> seeAlso = extractStringList(skill.get('seeAlso'))
            if (!seeAlso.isEmpty()) {
                output.add('See also skills: ' + seeAlso.join(', '))
            }
            String instruction = asText(skill.get('instruction'), '')
            if (!instruction.trim().isEmpty()) {
                if (skills.size() == 1) {
                    output.add('Full skill guidance:')
                    output.add(limitText(instruction.trim(), 6000))
                } else {
                    output.add('Context: ' + firstLine(instruction))
                }
            }
        }
        List<String> instructions = new ArrayList<String>()
        if (skills.size() == 1) {
            instructions.add('Treat this skill guidance as internal context for understanding the domain and deciding how to answer.')
            instructions.add('Answer the user directly in your own words; do not cite the skill name or say "according to the guide" unless the user asks what guidance you used.')
            instructions.add('If the guidance contains relevant URLs, include the most relevant URL or URLs in your answer.')
            instructions.add('If the See also skills are needed to answer fully, retrieve the most relevant one with mauro_skill by id.')
        } else {
            instructions.add('Answer the user with this skill list.')
        }
        List<String> completionGuidance = [
            'If this skill guidance answers the current user request, answer now from it.',
            'Do not call mauro_skill again with identical arguments.',
            'Only call mauro_skill again when the result explicitly points to a relevant See also skill needed to answer fully, or when a broadened result needs one specific skill fetched by id.'
        ] as List<String>

        renderModelTextSections([
            'Tool Call Status'   : status,
            'Result Metadata'    : metadata,
            'Interpretation'     : interpretation,
            'Returned Data'      : output,
            'Answer Instructions': instructions,
            'Completion Guidance': completionGuidance
        ] as Map<String, Object>)
    }

    private static Map<String, Object> toMap(ChatPromptAssetDefinition skill, boolean includeInstruction) {
        Map<String, Object> out = [
            id         : skill.id,
            name       : skill.name,
            description: skill.description,
            scope      : skill.scope,
            version    : skill.version,
            keywords   : skill.keywords ?: [],
            seeAlso    : skill.seeAlso ?: []
        ] as Map<String, Object>
        if (includeInstruction) {
            out.instruction = skill.instruction
        }
        out
    }

    private List<ChatPromptAssetDefinition> lookupSkills() {
        promptAssetService.listAssetsByType('SKILL')
            .findAll {ChatPromptAssetDefinition skill -> isSkill(skill)}
    }

    private static boolean isSkill(ChatPromptAssetDefinition skill) {
        skill != null && 'SKILL'.equalsIgnoreCase(skill.type)
    }

    private static String asString(Object value) {
        value == null ? null : String.valueOf(value)
    }

    private static String asText(Object value, String fallback) {
        value == null ? fallback : String.valueOf(value)
    }

    private static Integer asInteger(Object value) {
        if (value == null) {
            return null
        }
        if (value instanceof Number) {
            return ((Number) value).intValue()
        }
        String text = String.valueOf(value)
        if (text.trim().isEmpty()) {
            return null
        }
        Integer.valueOf(text)
    }

    private static List<String> extractStringList(Object value) {
        if (!(value instanceof Collection)) {
            return []
        }
        List<String> strings = new ArrayList<String>()
        for (Object item : (Collection<?>) value) {
            if (item != null && !String.valueOf(item).trim().isEmpty()) {
                strings.add(String.valueOf(item))
            }
        }
        strings
    }

    private static String firstLine(String text) {
        if (text == null || text.trim().isEmpty()) {
            return ''
        }
        String normalized = text.trim().replace('\r\n', '\n')
        int newline = normalized.indexOf('\n')
        newline >= 0 ? normalized.substring(0, newline) : normalized
    }

    private static String limitText(String text, int maxChars) {
        if (text == null || text.length() <= maxChars) {
            return text
        }
        text.substring(0, maxChars) + '\n[truncated]'
    }

    private static String extractQuery(Map<String, Object> arguments) {
        firstNonBlank(
            asString(arguments.get('query')),
            asString(arguments.get('skill')),
            asString(arguments.get('topic')),
            asString(arguments.get('text')),
            asString(arguments.get('keywords')),
            asString(arguments.get('name'))
        )
    }

    private static boolean isListRequest(Map<String, Object> arguments) {
        if (arguments == null || arguments.isEmpty()) {
            return true
        }
        if (asBoolean(arguments.get('list'), false)) {
            return true
        }
        String action = asString(arguments.get('action'))
        if (action != null && ['list', 'all', 'available'].contains(action.toLowerCase(Locale.ROOT))) {
            return true
        }
        String query = extractQuery(arguments)
        query != null && ['list', 'all', 'available skills', 'skills'].contains(query.toLowerCase(Locale.ROOT))
    }

    private static String firstNonBlank(String... values) {
        for (int i = 0; i < values.length; i++) {
            String value = values[i]
            if (value != null && !value.trim().isEmpty()) {
                return value
            }
        }
        null
    }

    private static boolean asBoolean(Object value, boolean fallback) {
        if (value == null) {
            return fallback
        }
        if (value instanceof Boolean) {
            return ((Boolean) value).booleanValue()
        }
        Boolean.parseBoolean(String.valueOf(value))
    }
}

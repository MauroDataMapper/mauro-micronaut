package org.maurodata.service.chat

import jakarta.inject.Singleton
import org.maurodata.plugin.chat.api.chat.SkillSummaryDto

@Singleton
class ChatSkillsRegistryService implements ChatSkillService {

    private final ChatSkillDefinitionLoader skillDefinitionLoader

    ChatSkillsRegistryService(ChatSkillDefinitionLoader skillDefinitionLoader) {
        this.skillDefinitionLoader = skillDefinitionLoader
    }

    @Override
    List<SkillSummaryDto> listSkills() {
        listSkillDefinitions().collect {ChatSkillDefinition definition ->
            new SkillSummaryDto(
                id: definition.id,
                name: definition.name,
                description: definition.description,
                scope: definition.scope,
                version: definition.version
            )
        }
    }

    @Override
    List<ChatSkillDefinition> listSkillDefinitions() {
        skillDefinitionLoader.listDefinitions()
    }

    @Override
    List<ChatSkillDefinition> listPersonaDefinitions() {
        listSkillDefinitions()
            .findAll {ChatSkillDefinition skill -> 'PERSONA'.equalsIgnoreCase(skill.type)}
            .sort {ChatSkillDefinition left, ChatSkillDefinition right ->
                Integer leftPriority = left.priority != null ? left.priority : Integer.valueOf(1000)
                Integer rightPriority = right.priority != null ? right.priority : Integer.valueOf(1000)
                int priorityCompare = leftPriority <=> rightPriority
                priorityCompare != 0 ? priorityCompare : left.id <=> right.id
            }
    }

    @Override
    ChatSkillDefinition findSkill(String id) {
        if (id == null || id.trim().isEmpty()) {
            return null
        }
        listSkillDefinitions().find {ChatSkillDefinition skill -> skill.id == id}
    }

    @Override
    List<ChatSkillDefinition> searchSkills(String query) {
        if (query == null || query.trim().isEmpty()) {
            return listSkillDefinitions()
        }
        String lower = query.toLowerCase(Locale.ROOT)
        List<ChatSkillDefinition> definitions = listSkillDefinitions()
        List<ChatSkillDefinition> primaryMatches = definitions.findAll {ChatSkillDefinition skill ->
            skill.id.toLowerCase(Locale.ROOT).contains(lower) ||
                skill.name.toLowerCase(Locale.ROOT).contains(lower) ||
                (skill.keywords ?: []).any {String keyword -> keyword.toLowerCase(Locale.ROOT).contains(lower)}
        }
        if (!primaryMatches.isEmpty()) {
            return primaryMatches
        }
        definitions.findAll {ChatSkillDefinition skill ->
            containsIgnoreCase(skill.description, lower) ||
                containsIgnoreCase(skill.instruction, lower)
        }
    }

    private static boolean containsIgnoreCase(String value, String lowerNeedle) {
        value != null && value.toLowerCase(Locale.ROOT).contains(lowerNeedle)
    }
}

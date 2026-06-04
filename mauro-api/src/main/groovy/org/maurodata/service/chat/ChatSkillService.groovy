package org.maurodata.service.chat

import org.maurodata.api.chat.SkillSummaryDto

interface ChatSkillService {
    List<SkillSummaryDto> listSkills()
    List<ChatSkillDefinition> listSkillDefinitions()
    List<ChatSkillDefinition> listPersonaDefinitions()
    ChatSkillDefinition findSkill(String id)
    List<ChatSkillDefinition> searchSkills(String query)
}

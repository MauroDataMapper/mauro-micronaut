package org.maurodata.api.chat

import org.maurodata.api.MauroApi
import org.maurodata.api.Paths

import io.micronaut.http.annotation.Get

@MauroApi
interface ChatSkillsApi {

    @Get(Paths.CHAT_SKILLS)
    List<SkillSummaryDto> listSkills()
}

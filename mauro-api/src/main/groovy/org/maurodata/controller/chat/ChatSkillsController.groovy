package org.maurodata.controller.chat

import groovy.transform.CompileStatic
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.security.annotation.Secured
import io.micronaut.security.rules.SecurityRule
import org.maurodata.api.Paths
import org.maurodata.api.chat.ChatSkillsApi
import org.maurodata.api.chat.SkillSummaryDto
import org.maurodata.audit.Audit
import org.maurodata.service.chat.ChatSkillService

@CompileStatic
@Controller
@Secured(SecurityRule.IS_ANONYMOUS)
class ChatSkillsController implements ChatSkillsApi {

    private final ChatSkillService chatSkillService

    ChatSkillsController(ChatSkillService chatSkillService) {
        this.chatSkillService = chatSkillService
    }

    @Override
    @Audit
    @Get(Paths.CHAT_SKILLS)
    List<SkillSummaryDto> listSkills() {
        chatSkillService.listSkills()
    }
}

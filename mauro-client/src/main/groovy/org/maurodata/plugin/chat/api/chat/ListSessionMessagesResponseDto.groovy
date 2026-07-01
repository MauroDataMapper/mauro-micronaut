package org.maurodata.plugin.chat.api.chat

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.Introspected

@Introspected
@CompileStatic
class ListSessionMessagesResponseDto {
    List<MessageDto> items = []
    String nextPageToken
}

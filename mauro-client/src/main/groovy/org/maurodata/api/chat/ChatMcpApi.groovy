package org.maurodata.api.chat

import org.maurodata.api.MauroApi
import org.maurodata.api.Paths

import io.micronaut.http.HttpStatus
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Delete
import io.micronaut.http.annotation.PathVariable
import io.micronaut.http.annotation.Post
import io.micronaut.http.annotation.Put
import io.micronaut.http.annotation.Status
import jakarta.validation.Valid

@MauroApi
interface ChatMcpApi {

    @Post(Paths.CHAT_MCP_SERVERS)
    McpServerDto addServer(@Body @Valid UpsertMcpServerRequest request)

    @Put(Paths.CHAT_MCP_SERVER)
    McpServerDto updateServer(@PathVariable String serverId, @Body @Valid UpsertMcpServerRequest request)

    @Status(HttpStatus.NO_CONTENT)
    @Delete(Paths.CHAT_MCP_SERVER)
    void removeServer(@PathVariable String serverId)
}

package org.maurodata.plugin.chat.controller

import groovy.transform.CompileStatic
import io.micronaut.http.HttpStatus
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.PathVariable
import io.micronaut.http.annotation.Post
import io.micronaut.http.exceptions.HttpStatusException
import io.micronaut.security.annotation.Secured
import io.micronaut.security.rules.SecurityRule
import io.swagger.v3.oas.annotations.Operation
import jakarta.inject.Inject
import org.maurodata.audit.Audit
import org.maurodata.controller.model.AdministeredItemReader
import org.maurodata.domain.datamodel.DataType
import org.maurodata.domain.model.AdministeredItem
import org.maurodata.domain.security.Role
import org.maurodata.plugin.chat.api.Paths
import org.maurodata.plugin.chat.semantic.SetSemanticIndexRepository
import org.maurodata.plugin.chat.semantic.SetSemanticIndexingService
import org.maurodata.plugin.chat.semantic.SetSemanticSearchResponse
import org.maurodata.plugin.chat.api.search.SetSemanticSearchApi
import org.maurodata.plugin.chat.semantic.SetSemanticSearchRequest
import org.maurodata.plugin.chat.semantic.SetSemanticSearchService
import org.maurodata.security.AccessControlService

import java.util.function.BiPredicate

/** Plugin endpoints; ordinary catalogue/context semantic search remains unchanged. */
@CompileStatic
@Controller
@Secured(SecurityRule.IS_ANONYMOUS)
class SetSemanticSearchController implements AdministeredItemReader, SetSemanticSearchApi {
    @Inject
    SetSemanticSearchService setSearch
    @Inject
    SetSemanticIndexingService setIndexing
    @Inject
    SetSemanticIndexRepository setIndexes
    @Inject
    AccessControlService accessControlService

    @Audit(level = Audit.AuditLevel.FILE_ONLY)
    @Operation(summary = "Search semantic sets", description = "Returns sets or requested catalogue projections within an optional model or folder scope, ranked by similarity to a text query.")
    @Post(Paths.SEMANTIC_SET_SEARCH)
    SetSemanticSearchResponse search(@Body SetSemanticSearchRequest request) {
        unprocessableEntity { setSearch.search(request, reader()) }
    }

    @Audit(level = Audit.AuditLevel.FILE_ONLY)
    @Operation(summary = "Find candidate sets", description = "Discovers sets or requested catalogue projections using a source Terminology, CodeSet, DataType or DataElement. Supports destination model or folder scope and pagination; scores are discovery evidence.")
    @Post(Paths.SEMANTIC_SET_CANDIDATES)
    SetSemanticSearchResponse candidates(@PathVariable String domainType, @PathVariable UUID setId, @Body SetSemanticSearchRequest request) {
        requireReadableSet(domainType, setId)
        unprocessableEntity { setSearch.candidates(request, domainType, setId, reader()) }
    }

    @Audit
    @Operation(summary = "Queue derived set index rebuilds", description = "Queues derived set indexes for the requested model or folder scope and embedding profile. Requires administrator access.")
    @Post(Paths.SEMANTIC_SET_INDEX_REBUILD)
    Map<String, Object> rebuild(@PathVariable UUID modelId, @Body SetSemanticSearchRequest request) {
        accessControlService.checkAdministrator()
        unprocessableEntity { setIndexing.requestRebuild(setSearch.profile(request), request.corpus, modelId) }
    }

    @Audit(level = Audit.AuditLevel.FILE_ONLY)
    @Operation(summary = "Get derived set index status", description = "Returns generation status, freshness and completeness metadata for the model scope and embedding profile. Requires administrator access.")
    @Post(Paths.SEMANTIC_SET_INDEX_STATUS)
    Map<String, Object> status(@PathVariable UUID modelId, @Body SetSemanticSearchRequest request) {
        accessControlService.checkAdministrator()
        unprocessableEntity { setIndexes.state(setSearch.profile(request), request.corpus, modelId) }
    }

    private BiPredicate<String, UUID> reader() {
        { String type, UUID id ->
            try {
                AdministeredItem item = findAdministeredItem(type, id)
                accessControlService.canDoRole(Role.READER, item)
            } catch (HttpStatusException missing) {
                if (missing.status == HttpStatus.NOT_FOUND) return false
                throw missing
            }
        } as BiPredicate<String, UUID>
    }

    private void requireReadableSet(String type, UUID id) {
        if (!(type in ['Terminology', 'CodeSet', 'DataType', 'DataElement'])) throw new HttpStatusException(HttpStatus.UNPROCESSABLE_ENTITY, 'Unsupported set type')
        AdministeredItem item = findAdministeredItem(type, id)
        if (!accessControlService.canDoRole(Role.READER, item)) throw new HttpStatusException(HttpStatus.NOT_FOUND, 'Set not found')
        if (item instanceof DataType && !((DataType) item).isEnumerationType() && !((DataType) item).isModelType()) {
            throw new HttpStatusException(HttpStatus.UNPROCESSABLE_ENTITY, 'DataType must be an EnumerationType or ModelDataType')
        }
    }

    private static <T> T unprocessableEntity(Closure<T> action) {
        try { action.call() }
        catch (IllegalArgumentException invalid) { throw new HttpStatusException(HttpStatus.UNPROCESSABLE_ENTITY, invalid.message) }
    }
}

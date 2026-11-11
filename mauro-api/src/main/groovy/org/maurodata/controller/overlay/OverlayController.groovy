package org.maurodata.controller.overlay

import groovy.transform.CompileStatic
import io.micronaut.http.HttpStatus
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Post
import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.Put
import io.micronaut.http.annotation.Delete
import io.micronaut.http.annotation.Get
import io.micronaut.http.exceptions.HttpStatusException
import io.micronaut.security.annotation.Secured
import io.micronaut.security.rules.SecurityRule
import io.micronaut.transaction.annotation.Transactional
import org.maurodata.api.Paths
import org.maurodata.api.model.ModelSuccessorRequestDTO
import org.maurodata.api.overlay.OverlayApi
import org.maurodata.api.datamodel.DataModelOverlayRequestDTO
import org.maurodata.api.overlay.OverlayApplicationDTO
import org.maurodata.api.overlay.OverlayMatchRequestDTO
import org.maurodata.api.overlay.OverlayApplicationResponseDTO
import org.maurodata.api.overlay.OverlayMatchResultDTO
import org.maurodata.api.overlay.OverlayRuleMoveDTO
import org.maurodata.controller.model.ModelController
import org.maurodata.domain.model.AdministeredItem
import org.maurodata.domain.datamodel.DataModel
import org.maurodata.domain.overlay.Overlay
import org.maurodata.domain.overlay.OverlayService
import org.maurodata.domain.overlay.OverlayRule
import org.maurodata.domain.security.Role
import org.maurodata.persistence.cache.ModelCacheableRepository
import org.maurodata.persistence.cache.ModelCacheableRepository.FolderCacheableRepository
import org.maurodata.persistence.cache.ModelCacheableRepository.DataModelCacheableRepository
import org.maurodata.service.overlay.OverlayApplicationService
import org.maurodata.service.overlay.OverlayRuleService
import org.maurodata.service.overlay.DataModelOverlayGeneratorService
import org.maurodata.domain.model.version.CreateNewVersionData
import org.maurodata.web.ListResponse
import org.maurodata.web.PaginationParams
import org.maurodata.domain.model.version.FinaliseData
import org.maurodata.api.model.PermissionsDTO
import io.micronaut.core.annotation.NonNull
import io.micronaut.core.annotation.Nullable
import org.maurodata.audit.Audit

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse

@Controller
@CompileStatic
@Secured(SecurityRule.IS_ANONYMOUS)
class OverlayController extends ModelController<Overlay> implements OverlayApi {

    @Audit
    @Transactional
    @Operation(operationId = 'createOverlaySuccessor', summary = 'Create an Overlay successor', description = 'Links an existing draft Overlay as a version successor.')
    @Post(Paths.OVERLAY_SUCCESSOR)
    @Override
    Overlay createSuccessor(UUID id, @Body @NonNull ModelSuccessorRequestDTO request) {
        super.createSuccessor(id, request) as Overlay
    }
    final OverlayApplicationService overlayApplicationService
    final ModelCacheableRepository.OverlayCacheableRepository overlayCacheableRepository
    @jakarta.inject.Inject OverlayRuleService overlayRuleService
    @jakarta.inject.Inject DataModelOverlayGeneratorService dataModelOverlayGeneratorService
    @jakarta.inject.Inject DataModelCacheableRepository dataModelCacheableRepository

    OverlayController(ModelCacheableRepository.OverlayCacheableRepository overlayRepository,
                      FolderCacheableRepository folderRepository,
                      OverlayService overlayService,
                      OverlayApplicationService overlayApplicationService) {
        super(Overlay, overlayRepository, folderRepository, overlayService)
        this.overlayCacheableRepository = overlayRepository
        this.overlayApplicationService = overlayApplicationService
    }

    @Override
    Overlay show(UUID id) {
        Overlay overlay = super.show(id) as Overlay
        overlay.overlayRules = overlayRuleService.readAll(id)
        overlay
    }

    @Override
    Overlay create(@NonNull UUID folderId, @Body @NonNull Overlay overlay) {
        List<OverlayRule> rules = overlay.overlayRules
        Overlay created = super.create(folderId, overlay) as Overlay
        if (rules != null) overlayRuleService.replaceAll(created.id, rules)
        created.overlayRules = overlayRuleService.readAll(created.id)
        created
    }

    @Override
    @Audit
    @Transactional
    @Post(Paths.OVERLAY_FROM_DATA_MODEL)
    @Operation(operationId = 'generateOverlayFromDataModel', summary = 'Generate an Overlay from a DataModel', description = 'Creates an Overlay from descriptions and writing metadata on the DataModel.')
    Overlay generateOverlayFromDataModel(UUID id, @Body DataModelOverlayRequestDTO request) {
        if (!request?.folderId) throw new HttpStatusException(HttpStatus.BAD_REQUEST, 'folderId is required')
        DataModel source = dataModelCacheableRepository.loadWithContent(id)
        if (!source) throw new HttpStatusException(HttpStatus.NOT_FOUND, 'DataModel not found')
        accessControlService.checkRole(Role.EDITOR, source)
        source.setAssociations()
        List<OverlayRule> rules = dataModelOverlayGeneratorService.generate(source)
        Overlay generated = new Overlay(label: request.label?.trim() ?: source.label + ' Overlay',
            description: request.description ?: source.description, overlayRules: rules)
        create(request.folderId, generated)
    }

    @Override
    Overlay update(UUID id, @Body @NonNull Overlay overlay) {
        requireWritableOverlay(id)
        List<OverlayRule> rules = overlay.overlayRules
        Overlay updated = super.update(id, overlay) as Overlay
        if (rules != null) overlayRuleService.replaceAll(id, rules)
        updated.overlayRules = overlayRuleService.readAll(id)
        updated
    }

    @Override
    Overlay cleanBody(Overlay overlay, boolean strictProperties = true) {
        // OverlayRules are its defining collection. Keep all other collection
        // properties subject to ItemController's guarded request-body handling.
        List<OverlayRule> overlayRules = overlay.overlayRules
        overlay.overlayRules = null
        try {
            super.cleanBody(overlay, strictProperties)
        } finally {
            overlay.overlayRules = overlayRules
        }
        validateOverlayRules(overlayRules)
        overlay
    }

    private static void validateOverlayRules(List<OverlayRule> overlayRules) {
        overlayRules?.each { OverlayRule overlayRule ->
            if (!overlayRule?.match?.path) {
                throw new HttpStatusException(HttpStatus.UNPROCESSABLE_ENTITY, 'OverlayRule entries require a path match')
            }
            if (overlayRule.match.matchFirstNodeIdentifierSuffix &&
                (!overlayRule.match.path.nodes || !overlayRule.match.path.nodes[0].identifier)) {
                throw new HttpStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    'First-node suffix matching requires a non-empty first selector identifier')
            }
            overlayRule.actions?.each { action ->
                if (!action?.type) {
                    throw new HttpStatusException(HttpStatus.UNPROCESSABLE_ENTITY, 'Overlay actions require a type')
                }
            }
        }
    }

    @Override
    HttpResponse delete(UUID id, @Body @Nullable Overlay overlay, @Nullable Boolean permanent) {
        super.delete(id, overlay, permanent)
    }

    @Override
    ListResponse<Overlay> list(UUID folderId, @Nullable PaginationParams params = new PaginationParams()) {
        withoutOverlayRules(super.list(folderId, params) as ListResponse<Overlay>)
    }

    @Override
    ListResponse<Overlay> listAll(@Nullable PaginationParams params = new PaginationParams()) {
        withoutOverlayRules(super.listAll(params) as ListResponse<Overlay>)
    }

    @Override
    @Get(Paths.OVERLAY_RULES_PAGED)
    ListResponse<OverlayRule> listRules(UUID overlayId, @Nullable PaginationParams params = new PaginationParams()) {
        Overlay overlay = requireOverlay(overlayId, Role.READER)
        overlayRuleService.list(overlay.id, params)
    }

    @Override
    OverlayRule showRule(UUID overlayId, UUID ruleId) {
        requireOverlay(overlayId, Role.READER)
        overlayRuleService.read(overlayId, ruleId)
    }

    @Override
    OverlayRule createRule(UUID overlayId, @Body @NonNull OverlayRule rule) {
        requireWritableOverlay(overlayId)
        validateOverlayRules([rule])
        overlayRuleService.create(overlayId, rule)
    }

    @Override
    OverlayRule updateRule(UUID overlayId, UUID ruleId, @Body @NonNull OverlayRule rule) {
        requireWritableOverlay(overlayId)
        validateOverlayRules([rule])
        overlayRuleService.update(overlayId, ruleId, rule)
    }

    @Override
    HttpResponse deleteRule(UUID overlayId, UUID ruleId) {
        requireWritableOverlay(overlayId)
        overlayRuleService.delete(overlayId, ruleId)
        HttpResponse.noContent()
    }

    @Override
    OverlayRule moveRule(UUID overlayId, UUID ruleId, @Body @NonNull OverlayRuleMoveDTO move) {
        requireWritableOverlay(overlayId)
        overlayRuleService.move(overlayId, ruleId, move?.beforeRuleId, move?.afterRuleId)
    }

    private Overlay requireOverlay(UUID overlayId, Role role) {
        Overlay overlay = overlayCacheableRepository.readById(overlayId)
        if (!overlay) throw new HttpStatusException(HttpStatus.NOT_FOUND, 'Overlay not found')
        accessControlService.checkRole(role, overlay)
        overlay
    }

    private Overlay requireWritableOverlay(UUID overlayId) {
        Overlay overlay = requireOverlay(overlayId, Role.EDITOR)
        if (overlay.finalised) throw new HttpStatusException(HttpStatus.UNPROCESSABLE_ENTITY, 'Cannot modify rules in a finalised Overlay')
        overlay
    }

    private static ListResponse<Overlay> withoutOverlayRules(ListResponse<Overlay> response) {
        response.items = response.items.collect { Overlay overlay ->
            Overlay summary = (Overlay) overlay.clone()
            summary.overlayRules = null
            summary
        }
        response
    }

    @Override
    Overlay createNewBranchModelVersion(UUID id, @Body @Nullable CreateNewVersionData data) {
        Overlay branch = super.createNewBranchModelVersion(id, data) as Overlay
        branch.overlayRules = overlayRuleService.readAll(branch.id)
        branch
    }

    @Override
    Overlay finalise(UUID id, @Body FinaliseData data) {
        Overlay finalised = super.finalise(id, data) as Overlay
        finalised.overlayRules = overlayRuleService.readAll(id)
        finalised
    }

    @Override
    @Audit
    @Operation(summary = "Move an overlay", description = "Moves an overlay to another folder.")
    @Put(Paths.OVERLAYS_MOVE)
    @Transactional
    Overlay moveFolder(UUID id, String destination) { super.moveFolder(id, destination) as Overlay }

    @Override
    PermissionsDTO permissions(UUID id) { super.permissions(id) }

    @Override
    @Post(Paths.OVERLAYS_APPLY)
    @Transactional
    @Audit
    OverlayApplicationResponseDTO apply(@Body OverlayApplicationDTO request) {
        if (!request?.rootDomainType || !request.rootId || request.overlayIds == null) {
            throw new HttpStatusException(HttpStatus.BAD_REQUEST, 'rootDomainType, rootId and overlayIds are required')
        }
        AdministeredItem root = readAdministeredItem(request.rootDomainType, request.rootId)
        accessControlService.checkRole(Role.EDITOR, root)
        request.overlayIds.each { UUID overlayId ->
            Overlay overlay = overlayCacheableRepository.readById(overlayId)
            accessControlService.checkRole(Role.READER, overlay)
        }
        overlayApplicationService.apply(request)
    }

    @Post(Paths.OVERLAYS_MATCHES)
    @Audit
    List<OverlayMatchResultDTO> matches(@Body OverlayMatchRequestDTO request) {
        if (!request?.rootDomainType || !request.rootId || request.overlayIds == null) {
            throw new HttpStatusException(HttpStatus.BAD_REQUEST, 'rootDomainType, rootId and overlayIds are required')
        }
        AdministeredItem root = readAdministeredItem(request.rootDomainType, request.rootId)
        accessControlService.checkRole(Role.EDITOR, root)
        request.overlayIds.each { UUID overlayId -> accessControlService.checkRole(Role.READER, overlayCacheableRepository.readById(overlayId)) }
        overlayApplicationService.matches(request)
    }

    @Audit
    @Operation(summary = "Update an overlay", description = "Updates an overlay.")
    @Put(Paths.OVERLAYS_READ_BY_AUTHENTICATED)
    @Transactional
    Overlay allowReadByAuthenticated(UUID id) { super.putReadByAuthenticated(id) as Overlay }

    @Audit
    @ApiResponse(responseCode = "204", description = "No content - deleted successfully")
    @Operation(summary = "Delete an overlay", description = "Deletes an overlay.")
    @Delete(Paths.OVERLAYS_READ_BY_AUTHENTICATED)
    @Transactional
    HttpResponse revokeReadByAuthenticated(UUID id) { super.deleteReadByAuthenticated(id) }

    @Audit
    @Operation(summary = "Update an overlay", description = "Updates an overlay.")
    @Put(Paths.OVERLAYS_READ_BY_EVERYONE)
    @Transactional
    Overlay allowReadByEveryone(UUID id) { super.putReadByEveryone(id) as Overlay }

    @Audit
    @ApiResponse(responseCode = "204", description = "No content - deleted successfully")
    @Operation(summary = "Delete an overlay", description = "Deletes an overlay.")
    @Delete(Paths.OVERLAYS_READ_BY_EVERYONE)
    @Transactional
    HttpResponse revokeReadByEveryone(UUID id) { super.deleteReadByEveryone(id) }
}

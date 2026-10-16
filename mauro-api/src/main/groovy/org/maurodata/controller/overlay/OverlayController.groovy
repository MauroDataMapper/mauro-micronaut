package org.maurodata.controller.overlay

import groovy.transform.CompileStatic
import io.micronaut.http.HttpStatus
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Post
import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.Put
import io.micronaut.http.annotation.Delete
import io.micronaut.http.exceptions.HttpStatusException
import io.micronaut.security.annotation.Secured
import io.micronaut.security.rules.SecurityRule
import io.micronaut.transaction.annotation.Transactional
import org.maurodata.api.Paths
import org.maurodata.api.overlay.OverlayApi
import org.maurodata.api.overlay.OverlayApplicationDTO
import org.maurodata.api.overlay.OverlayMatchRequestDTO
import org.maurodata.api.overlay.OverlayApplicationResponseDTO
import org.maurodata.api.overlay.OverlayMatchResultDTO
import org.maurodata.controller.model.ModelController
import org.maurodata.domain.model.AdministeredItem
import org.maurodata.domain.overlay.Overlay
import org.maurodata.domain.overlay.OverlayService
import org.maurodata.domain.overlay.OverlayRule
import org.maurodata.domain.security.Role
import org.maurodata.persistence.cache.ModelCacheableRepository
import org.maurodata.persistence.cache.ModelCacheableRepository.FolderCacheableRepository
import org.maurodata.service.overlay.OverlayApplicationService
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
    final OverlayApplicationService overlayApplicationService
    final ModelCacheableRepository.OverlayCacheableRepository overlayCacheableRepository

    OverlayController(ModelCacheableRepository.OverlayCacheableRepository overlayRepository,
                      FolderCacheableRepository folderRepository,
                      OverlayService overlayService,
                      OverlayApplicationService overlayApplicationService) {
        super(Overlay, overlayRepository, folderRepository, overlayService)
        this.overlayCacheableRepository = overlayRepository
        this.overlayApplicationService = overlayApplicationService
    }

    @Override
    Overlay show(UUID id) { super.show(id) as Overlay }

    @Override
    Overlay create(@NonNull UUID folderId, @Body @NonNull Overlay overlay) { super.create(folderId, overlay) as Overlay }

    @Override
    Overlay update(UUID id, @Body @NonNull Overlay overlay) { super.update(id, overlay) as Overlay }

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
        super.list(folderId, params) as ListResponse<Overlay>
    }

    @Override
    ListResponse<Overlay> listAll(@Nullable PaginationParams params = new PaginationParams()) {
        super.listAll(params) as ListResponse<Overlay>
    }

    @Override
    Overlay createNewBranchModelVersion(UUID id, @Body @Nullable CreateNewVersionData data) {
        super.createNewBranchModelVersion(id, data) as Overlay
    }

    @Override
    Overlay finalise(UUID id, @Body FinaliseData data) { super.finalise(id, data) as Overlay }

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

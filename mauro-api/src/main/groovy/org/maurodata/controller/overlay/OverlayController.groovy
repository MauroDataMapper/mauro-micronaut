package org.maurodata.controller.overlay

import groovy.transform.CompileStatic
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Post
import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.Put
import io.micronaut.http.annotation.Delete
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

@Controller
@CompileStatic
@Secured(SecurityRule.IS_ANONYMOUS)
class OverlayController extends ModelController<Overlay> implements OverlayApi {
    final OverlayApplicationService overlayApplicationService
    final ModelCacheableRepository.OverlayCacheableRepository overlayCacheableRepository

    OverlayController(ModelCacheableRepository.OverlayCacheableRepository overlayRepository,
                      FolderCacheableRepository folderRepository,
                      OverlayApplicationService overlayApplicationService) {
        super(Overlay, overlayRepository, folderRepository)
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
        // Rules are the Overlay's defining collection. Keep all other collection
        // properties subject to ItemController's guarded request-body handling.
        List<OverlayRule> rules = overlay.rules
        overlay.rules = null
        try {
            super.cleanBody(overlay, strictProperties)
        } finally {
            overlay.rules = rules
        }
        validateRules(rules)
        overlay
    }

    private static void validateRules(List<OverlayRule> rules) {
        rules?.each { OverlayRule rule ->
            if (!rule?.match?.path) {
                throw new io.micronaut.http.exceptions.HttpStatusException(io.micronaut.http.HttpStatus.UNPROCESSABLE_ENTITY, 'Overlay rules require a path match')
            }
            rule.actions?.each { action ->
                if (!action?.type || action.value == null) {
                    throw new io.micronaut.http.exceptions.HttpStatusException(io.micronaut.http.HttpStatus.UNPROCESSABLE_ENTITY, 'Overlay actions require a type and value')
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
    PermissionsDTO permissions(UUID id) { super.permissions(id) }

    @Override
    @Post(Paths.OVERLAYS_APPLY)
    @Transactional
    @Audit
    OverlayApplicationResponseDTO apply(@Body OverlayApplicationDTO request) {
        if (!request?.rootDomainType || !request.rootId || request.overlayIds == null) {
            throw new io.micronaut.http.exceptions.HttpStatusException(io.micronaut.http.HttpStatus.BAD_REQUEST, 'rootDomainType, rootId and overlayIds are required')
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
            throw new io.micronaut.http.exceptions.HttpStatusException(io.micronaut.http.HttpStatus.BAD_REQUEST, 'rootDomainType, rootId and overlayIds are required')
        }
        AdministeredItem root = readAdministeredItem(request.rootDomainType, request.rootId)
        accessControlService.checkRole(Role.EDITOR, root)
        request.overlayIds.each { UUID overlayId -> accessControlService.checkRole(Role.READER, overlayCacheableRepository.readById(overlayId)) }
        overlayApplicationService.matches(request)
    }

    @Put(Paths.OVERLAYS_READ_BY_AUTHENTICATED)
    Overlay allowReadByAuthenticated(UUID id) { super.putReadByAuthenticated(id) as Overlay }

    @Delete(Paths.OVERLAYS_READ_BY_AUTHENTICATED)
    HttpResponse revokeReadByAuthenticated(UUID id) { super.deleteReadByAuthenticated(id) }

    @Put(Paths.OVERLAYS_READ_BY_EVERYONE)
    Overlay allowReadByEveryone(UUID id) { super.putReadByEveryone(id) as Overlay }

    @Delete(Paths.OVERLAYS_READ_BY_EVERYONE)
    HttpResponse revokeReadByEveryone(UUID id) { super.deleteReadByEveryone(id) }
}

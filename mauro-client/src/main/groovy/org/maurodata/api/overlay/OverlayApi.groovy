package org.maurodata.api.overlay

import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Post
import org.maurodata.api.MauroApi
import org.maurodata.api.Paths
import org.maurodata.api.model.ModelApi
import org.maurodata.domain.overlay.Overlay
import org.maurodata.domain.model.version.CreateNewVersionData
import org.maurodata.domain.model.version.FinaliseData
import org.maurodata.api.model.PermissionsDTO
import org.maurodata.web.ListResponse
import org.maurodata.web.PaginationParams
import io.micronaut.core.annotation.NonNull
import io.micronaut.core.annotation.Nullable
import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.Delete
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Put

@MauroApi
interface OverlayApi extends ModelApi<Overlay> {
    @Get(Paths.OVERLAYS_ID_ROUTE)
    Overlay show(UUID id)

    @Post(Paths.FOLDER_OVERLAYS_ROUTE)
    Overlay create(UUID folderId, @Body @NonNull Overlay overlay)

    @Put(Paths.OVERLAYS_ID_ROUTE)
    Overlay update(UUID id, @Body @NonNull Overlay overlay)

    @Delete(Paths.OVERLAYS_ID_ROUTE)
    HttpResponse delete(UUID id, @Body @Nullable Overlay overlay, @Nullable Boolean permanent)

    @Get(Paths.FOLDER_OVERLAYS_ROUTE)
    ListResponse<Overlay> list(UUID folderId)

    @Get(Paths.FOLDER_OVERLAYS_ROUTE_PAGED)
    ListResponse<Overlay> list(UUID folderId, @Nullable PaginationParams params)

    @Get(Paths.OVERLAYS_LIST)
    ListResponse<Overlay> listAll()

    @Get(Paths.OVERLAYS_LIST_PAGED)
    ListResponse<Overlay> listAll(@Nullable PaginationParams params)

    @Put(Paths.OVERLAYS_BRANCH_MODEL_VERSION)
    Overlay createNewBranchModelVersion(UUID id, @Body @Nullable CreateNewVersionData createNewVersionData)

    @Put(Paths.OVERLAYS_FINALISE)
    Overlay finalise(UUID id, @Body FinaliseData finaliseData)

    @Get(Paths.OVERLAYS_PERMISSIONS)
    PermissionsDTO permissions(UUID id)

    @Put(Paths.OVERLAYS_READ_BY_AUTHENTICATED)
    Overlay allowReadByAuthenticated(UUID id)

    @Delete(Paths.OVERLAYS_READ_BY_AUTHENTICATED)
    HttpResponse revokeReadByAuthenticated(UUID id)

    @Put(Paths.OVERLAYS_READ_BY_EVERYONE)
    Overlay allowReadByEveryone(UUID id)

    @Delete(Paths.OVERLAYS_READ_BY_EVERYONE)
    HttpResponse revokeReadByEveryone(UUID id)

    @Post(Paths.OVERLAYS_APPLY)
    OverlayApplicationResponseDTO apply(@Body OverlayApplicationDTO request)

    @Post(Paths.OVERLAYS_MATCHES)
    List<OverlayMatchResultDTO> matches(@Body OverlayMatchRequestDTO request)
}

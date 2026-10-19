package org.maurodata.controller.facet

import groovy.transform.CompileStatic
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Delete
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Post
import io.micronaut.security.annotation.Secured
import io.micronaut.security.rules.SecurityRule
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import jakarta.inject.Inject
import org.maurodata.api.Paths
import org.maurodata.api.facet.DataFlowNetworkApi
import org.maurodata.api.facet.DataFlowSourceCreateDTO
import org.maurodata.api.facet.DataComponentCreateDTO
import org.maurodata.api.link.ItemLinkDTO
import org.maurodata.audit.Audit
import org.maurodata.controller.model.ItemController
import org.maurodata.domain.facet.ItemLink
import org.maurodata.domain.model.AdministeredItem
import org.maurodata.domain.datamodel.DataComponent
import org.maurodata.domain.datamodel.DataModel
import org.maurodata.domain.security.Role
import org.maurodata.persistence.cache.FacetCacheableRepository
import org.maurodata.service.path.DataFlowNetworkService
import org.maurodata.web.ListResponse

@CompileStatic
@Controller
@Secured(SecurityRule.IS_ANONYMOUS)
class DataFlowNetworkController extends ItemController<ItemLink> implements DataFlowNetworkApi {
    @Inject DataFlowNetworkService dataFlowNetworkService

    DataFlowNetworkController(FacetCacheableRepository.ItemLinkCacheableRepository repository) {
        super(repository)
    }

    @Override
    @Audit
    @Operation(operationId = 'listDataFlowNetworkSources', summary = 'List incoming data flow paths', description = 'Returns the stored incoming data flow paths for an item.')
    @Get(Paths.DATA_FLOW_NETWORK_SOURCE_LIST)
    ListResponse<ItemLinkDTO> list(String domainType, UUID domainId) {
        AdministeredItem target = readAdministeredItem(domainType, domainId)
        accessControlService.checkRole(Role.READER, target)
        ListResponse.from(dataFlowNetworkService.list(target).collect { toDTO(it) })
    }

    @Override
    @Audit
    @Operation(operationId = 'createDataFlowNetworkSource', summary = 'Create an incoming data flow path', description = 'Stores a path describing an incoming data flow source.')
    @Post(Paths.DATA_FLOW_NETWORK_SOURCE_LIST)
    ItemLinkDTO create(String domainType, UUID domainId, @Body DataFlowSourceCreateDTO request) {
        AdministeredItem target = readAdministeredItem(domainType, domainId)
        accessControlService.checkRole(Role.EDITOR, target)
        ItemLink created = dataFlowNetworkService.create(target, request?.path)
        toDTO(created)
    }

    @Override
    @Audit
    @Operation(operationId = 'showDataFlowNetworkSource', summary = 'Show an incoming data flow path', description = 'Returns a stored data flow path without requiring its source to resolve.')
    @Get(Paths.DATA_FLOW_NETWORK_SOURCE_ID)
    ItemLinkDTO show(String domainType, UUID domainId, UUID id) {
        AdministeredItem target = readAdministeredItem(domainType, domainId)
        accessControlService.checkRole(Role.READER, target)
        toDTO(dataFlowNetworkService.show(target, id))
    }

    @Override
    @Audit(deletedObjectDomainType = ItemLink)
    @ApiResponse(responseCode = '204', description = 'No content - deleted successfully')
    @Operation(operationId = 'deleteDataFlowNetworkSource', summary = 'Delete an incoming data flow path', description = 'Deletes a stored data flow path.')
    @Delete(Paths.DATA_FLOW_NETWORK_SOURCE_ID)
    HttpResponse delete(String domainType, UUID domainId, UUID id) {
        AdministeredItem target = readAdministeredItem(domainType, domainId)
        accessControlService.checkRole(Role.EDITOR, target)
        dataFlowNetworkService.delete(target, id) ? HttpResponse.status(HttpStatus.NO_CONTENT) : HttpResponse.status(HttpStatus.NOT_FOUND)
    }

    @Override
    @Audit
    @Operation(operationId = 'listDataFlowNetworkComponents', summary = 'List DataFlow components', description = 'Returns the DataFlow components in a DataModel.')
    @Get(Paths.DATA_FLOW_NETWORK_COMPONENT_LIST)
    ListResponse<DataComponent> listDataFlowNetworkComponents(UUID dataModelId) {
        ListResponse.from(dataFlowNetworkService.listDataFlowNetworkComponents(dataModelId))
    }

    @Override
    @Audit(parentDomainType = DataModel, parentIdParamName = 'dataModelId')
    @Operation(operationId = 'createDataFlowNetworkComponent', summary = 'Create a DataFlow component', description = 'Creates a DataFlow component and its consumes and produces links.')
    @Post(Paths.DATA_FLOW_NETWORK_COMPONENT_LIST)
    DataComponent createDataFlowNetworkComponent(UUID dataModelId, @Body DataComponentCreateDTO request) {
        dataFlowNetworkService.createDataFlowNetworkComponent(dataModelId, request)
    }

    @Override
    @Audit
    @Operation(operationId = 'showDataFlowNetworkComponent', summary = 'Show a DataFlow component', description = 'Returns a DataFlow component in a DataModel.')
    @Get(Paths.DATA_FLOW_NETWORK_COMPONENT_ID)
    DataComponent showDataFlowNetworkComponent(UUID dataModelId, UUID id) {
        dataFlowNetworkService.showDataFlowNetworkComponent(dataModelId, id)
    }

    @Override
    @Audit(parentDomainType = DataModel, parentIdParamName = 'dataModelId', deletedObjectDomainType = DataComponent)
    @ApiResponse(responseCode = '204', description = 'No content - deleted successfully')
    @Operation(operationId = 'deleteDataFlowNetworkComponent', summary = 'Delete a DataFlow component', description = 'Deletes a DataFlow component.')
    @Delete(Paths.DATA_FLOW_NETWORK_COMPONENT_ID)
    HttpResponse deleteDataFlowNetworkComponent(UUID dataModelId, UUID id) {
        dataFlowNetworkService.deleteDataFlowNetworkComponent(dataModelId, id)
            ? HttpResponse.status(HttpStatus.NO_CONTENT)
            : HttpResponse.status(HttpStatus.NOT_FOUND)
    }

    private static ItemLinkDTO toDTO(ItemLink facet) {
        new ItemLinkDTO(id: facet.id, type: facet.type, path: facet.path?.toString())
    }
}

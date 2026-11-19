package org.maurodata.service.link

import groovy.transform.CompileStatic
import io.micronaut.http.HttpStatus
import io.micronaut.http.exceptions.HttpStatusException
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.maurodata.api.link.ItemLinkAnnotationDTO
import org.maurodata.api.link.ItemLinkDTO
import org.maurodata.api.link.ItemLinkGraphDTO
import org.maurodata.api.link.ItemLinkItemDTO
import org.maurodata.api.link.ItemLinkKindDTO
import org.maurodata.api.link.ItemLinkMetadataDTO
import org.maurodata.api.link.ItemLinkModelDTO
import org.maurodata.domain.datamodel.DataClass
import org.maurodata.domain.datamodel.DataComponent
import org.maurodata.domain.datamodel.DataElement
import org.maurodata.domain.datamodel.DataModel
import org.maurodata.domain.facet.Annotation
import org.maurodata.domain.facet.Metadata
import org.maurodata.domain.model.AdministeredItem
import org.maurodata.domain.model.Model
import org.maurodata.domain.model.ModelItem
import org.maurodata.domain.security.Role
import org.maurodata.persistence.cache.AdministeredItemCacheableRepository
import org.maurodata.persistence.cache.FacetCacheableRepository
import org.maurodata.persistence.model.PathRepository
import org.maurodata.security.AccessControlService

@CompileStatic
@Singleton
class ItemLinkGraphService {
    @Inject List<ItemLinkGraphProvider> linkProviders
    @Inject ItemLinkPathResolver itemLinkPathResolver
    @Inject PathRepository pathRepository
    @Inject FacetCacheableRepository.MetadataCacheableRepository metadataRepository
    @Inject FacetCacheableRepository.AnnotationCacheableRepository annotationRepository
    @Inject AdministeredItemCacheableRepository.DataClassCacheableRepository dataClassRepository
    @Inject AdministeredItemCacheableRepository.DataElementCacheableRepository dataElementRepository
    @Inject AdministeredItemCacheableRepository.DataComponentCacheableRepository dataComponentRepository
    @Inject AccessControlService accessControlService

    List<ItemLinkKindDTO> supportedKinds() {
        providersByType().values().collect { ItemLinkGraphProvider provider ->
            new ItemLinkKindDTO(type: provider.type, label: provider.label)
        }.sort(false) { ItemLinkKindDTO kind -> kind.type }
    }

    ItemLinkGraphDTO traverse(AdministeredItem root, List<String> requestedTypes) {
        traverse(root, requestedTypes, false, false, false)
    }

    ItemLinkGraphDTO traverse(AdministeredItem root, List<String> requestedTypes, boolean includeChildren) {
        traverse(root, requestedTypes, includeChildren, false, false)
    }

    ItemLinkGraphDTO traverse(AdministeredItem root, List<String> requestedTypes, boolean includeChildren,
                              boolean includeMetadata, boolean includeAnnotations) {
        if (!requestedTypes) {
            throw new HttpStatusException(HttpStatus.BAD_REQUEST, 'At least one link type must be selected')
        }

        Map<String, ItemLinkGraphProvider> providers = providersByType()
        List<String> types = requestedTypes.findAll { it != null && !it.trim().isEmpty() }.unique()
        if (!types) {
            throw new HttpStatusException(HttpStatus.BAD_REQUEST, 'At least one link type must be selected')
        }
        List<String> unsupported = types.findAll { !providers.containsKey(it) }
        if (unsupported) {
            throw new HttpStatusException(HttpStatus.BAD_REQUEST, "Unsupported link type(s): ${unsupported.join(', ')}")
        }
        List<ItemLinkGraphProvider> selectedProviders = types.collect { providers[it] }
            .sort(false) { ItemLinkGraphProvider provider -> provider.type }

        ItemLinkGraphDTO graph = new ItemLinkGraphDTO()
        Map<String, ItemLinkItemDTO> itemsByKey = [:]
        Map<String, AdministeredItem> domainItemsByKey = [:]
        Set<String> linksById = [] as Set<String>
        Set<String> expanded = [] as Set<String>
        Set<String> queued = [] as Set<String>
        ItemLinkPathResolver.ResolutionContext resolutionContext = itemLinkPathResolver.newResolutionContext()

        addItem(root, graph, itemsByKey, domainItemsByKey, resolutionContext)
        List<AdministeredItem> frontier = [root]
        queued.add(itemKey(root))

        // Preload selected incoming links for eligible direct children. Child tables are
        // joined to ItemLink rows in SQL; only matching child rows are hydrated.
        Map<String, Map<UUID, List<ItemLinkGraphReference>>> preloadedLinks = [:]
        if (includeChildren) {
            selectedProviders.each { ItemLinkGraphProvider provider ->
                preloadedLinks[provider.type] = groupByTarget(provider.findLinksOwnedByDirectChildren(root))
            }
            Set<UUID> participatingIds = [] as Set<UUID>
            preloadedLinks.values().each { Map<UUID, List<ItemLinkGraphReference>> grouped ->
                participatingIds.addAll(grouped.keySet())
            }
            List<AdministeredItem> children = loadChildren(root, participatingIds)
                .findAll { AdministeredItem child -> accessControlService.canDoRole(Role.READER, child) }
                .sort(false) { AdministeredItem left, AdministeredItem right -> compareModelItems(left, right) }
            children.each { AdministeredItem child ->
                String childKey = itemKey(child)
                if (!itemsByKey.containsKey(childKey)) {
                    addItem(child, graph, itemsByKey, domainItemsByKey, resolutionContext)
                }
                if (queued.add(childKey)) {
                    frontier.add(child)
                }
            }
        }

        while (frontier) {
            List<AdministeredItem> current = frontier.findAll { AdministeredItem item ->
                !expanded.contains(itemKey(item))
            }
            if (!current) {
                break
            }
            current.each { AdministeredItem item -> expanded.add(itemKey(item)) }

            // One batched link query per selected provider for this breadth-first frontier.
            Map<String, Map<UUID, List<ItemLinkGraphReference>>> linksByProvider = [:]
            selectedProviders.each { ItemLinkGraphProvider provider ->
                Map<UUID, List<ItemLinkGraphReference>> linksByTarget = [:]
                Map<UUID, List<ItemLinkGraphReference>> preloaded = preloadedLinks[provider.type] ?: [:]
                current.each { AdministeredItem target ->
                    List<ItemLinkGraphReference> cached = preloaded[target.id]
                    if (cached != null) {
                        linksByTarget[target.id] = cached
                    }
                }
                List<UUID> missingTargetIds = current*.id
                    .findAll { UUID targetId -> !linksByTarget.containsKey(targetId) }.unique()
                if (missingTargetIds) {
                    groupByTarget(provider.findLinksOwnedByTargets(missingTargetIds)).each {
                        UUID targetId, List<ItemLinkGraphReference> references ->
                            linksByTarget[targetId] = references
                    }
                }
                linksByProvider[provider.type] = linksByTarget
            }

            List<AdministeredItem> nextFrontier = []
            current.each { AdministeredItem target ->
                selectedProviders.each { ItemLinkGraphProvider provider ->
                    List<ItemLinkGraphReference> references = (linksByProvider[provider.type][target.id] ?: [])
                        .sort(false) { ItemLinkGraphReference reference -> reference.sourcePath?.toString() ?: '' }
                    references.each { ItemLinkGraphReference reference ->
                        ResolvedItemLinkPath resolved = itemLinkPathResolver.resolve(resolutionContext, target, reference.sourcePath)
                        AdministeredItem source = resolved.source
                        addLink(graph, linksById, new ItemLinkDTO(
                            id: reference.id,
                            type: reference.type,
                            path: resolved.path?.toString(),
                            sourceId: source?.id,
                            sourceDomainType: source?.domainType,
                            targetId: target.id,
                            targetDomainType: target.domainType
                        ))
                        if (source) {
                            String sourceKey = itemKey(source)
                            if (!itemsByKey.containsKey(sourceKey)) {
                                addItem(source, graph, itemsByKey, domainItemsByKey, resolutionContext)
                            }
                            if (!expanded.contains(sourceKey) && queued.add(sourceKey)) {
                                nextFrontier.add(source)
                            }
                        }
                    }
                }
            }
            frontier = nextFrontier
        }

        if (includeMetadata) {
            addMetadata(graph, domainItemsByKey)
        }
        if (includeAnnotations) {
            addAnnotations(graph, domainItemsByKey)
        }
        graph
    }

    private List<AdministeredItem> loadChildren(AdministeredItem parent, Set<UUID> participatingIds) {
        if (!participatingIds) {
            return []
        }
        List<AdministeredItem> result = []
        if (parent instanceof DataModel) {
            result.addAll(dataClassRepository.readAllByIdIn(participatingIds))
            result.addAll(dataComponentRepository.readAllByIdIn(participatingIds))
        } else if (parent instanceof DataClass) {
            result.addAll(dataClassRepository.readAllByIdIn(participatingIds))
            result.addAll(dataElementRepository.findAllByIdIn(participatingIds))
        }
        result
    }

    private static Map<UUID, List<ItemLinkGraphReference>> groupByTarget(List<ItemLinkGraphReference> references) {
        Map<UUID, List<ItemLinkGraphReference>> grouped = [:]
        references.each { ItemLinkGraphReference reference ->
            if (!grouped.containsKey(reference.targetId)) {
                grouped[reference.targetId] = []
            }
            grouped[reference.targetId] << reference
        }
        grouped
    }

    private static int compareModelItems(AdministeredItem left, AdministeredItem right) {
        Integer leftOrder = (left as ModelItem).order ?: 0
        Integer rightOrder = (right as ModelItem).order ?: 0
        leftOrder <=> rightOrder ?: (left.label ?: '') <=> (right.label ?: '')
    }

    private Map<String, ItemLinkGraphProvider> providersByType() {
        Map<String, ItemLinkGraphProvider> result = [:]
        linkProviders.each { ItemLinkGraphProvider provider -> result[provider.type] = provider }
        result
    }

    private static void addLink(ItemLinkGraphDTO graph, Set<String> linksById, ItemLinkDTO link) {
        if (link.id && linksById.add(link.id.toString())) {
            graph.links << link
        }
    }

    private void addItem(AdministeredItem item, ItemLinkGraphDTO graph,
                         Map<String, ItemLinkItemDTO> itemsByKey,
                         Map<String, AdministeredItem> domainItemsByKey,
                         ItemLinkPathResolver.ResolutionContext resolutionContext) {
        if (item.id && !resolutionContext.parentItemsLoaded.contains(item.id)) {
            pathRepository.readParentItems(item)
            resolutionContext.parentItemsLoaded.add(item.id)
        }
        item.updatePath()
        item.updateBreadcrumbs()
        Model owner = item.owner
        if (owner) {
            if (owner.id && !resolutionContext.parentItemsLoaded.contains(owner.id)) {
                pathRepository.readParentItems(owner)
                resolutionContext.parentItemsLoaded.add(owner.id)
            }
            owner.updatePath()
        }
        itemLinkPathResolver.rememberTargetModel(resolutionContext, item, owner)

        ItemLinkItemDTO dto = new ItemLinkItemDTO(
            id: item.id,
            domainType: item.domainType,
            label: item.label,
            description: item.description,
            breadcrumbs: item.breadcrumbs,
            owningModel: owner ? new ItemLinkModelDTO(
                id: owner.id,
                domainType: owner.domainType,
                label: owner.label,
                finalised: owner.finalised,
                branchName: owner.branchName,
                modelVersion: owner.modelVersion,
                modelVersionTag: owner.modelVersionTag
            ) : null
        )
        String key = itemKey(item)
        itemsByKey[key] = dto
        domainItemsByKey[key] = item
        graph.items << dto
    }

    private void addMetadata(ItemLinkGraphDTO graph, Map<String, AdministeredItem> domainItemsByKey) {
        if (!domainItemsByKey) {
            return
        }
        List<UUID> itemIds = domainItemsByKey.values()*.id.unique()
        Map<UUID, List<ItemLinkMetadataDTO>> metadataByItemId = [:]
        (metadataRepository.readAllByMultiFacetAwareItemIdIn(itemIds) as List<Metadata>).each { Metadata metadata ->
            if (!metadataByItemId.containsKey(metadata.multiFacetAwareItemId)) {
                metadataByItemId[metadata.multiFacetAwareItemId] = []
            }
            metadataByItemId[metadata.multiFacetAwareItemId] << new ItemLinkMetadataDTO(
                id: metadata.id, namespace: metadata.namespace, key: metadata.key, value: metadata.value)
        }
        graph.items.each { ItemLinkItemDTO dto ->
            dto.metadata = (metadataByItemId[dto.id] ?: []).sort(false) { ItemLinkMetadataDTO value ->
                "${value.namespace ?: ''}|${value.key ?: ''}"
            }
        }
    }

    private void addAnnotations(ItemLinkGraphDTO graph, Map<String, AdministeredItem> domainItemsByKey) {
        if (!domainItemsByKey) {
            return
        }
        List<UUID> itemIds = domainItemsByKey.values()*.id.unique()
        List<Annotation> allAnnotations = annotationRepository.readAllByMultiFacetAwareItemIdInAndParentAnnotationIdIsNull(itemIds) as List<Annotation>
        List<Annotation> frontier = allAnnotations
        Set<UUID> fetchedParents = [] as Set<UUID>
        Map<UUID, List<Annotation>> childrenByParentId = [:]
        while (frontier) {
            List<UUID> parentIds = frontier*.id
                .findAll { UUID id -> id != null && fetchedParents.add(id) }
            if (!parentIds) {
                break
            }
            List<Annotation> children = annotationRepository.readAllByParentAnnotationIdIn(parentIds) as List<Annotation>
            children.each { Annotation child ->
                if (!childrenByParentId.containsKey(child.parentAnnotationId)) {
                    childrenByParentId[child.parentAnnotationId] = []
                }
                childrenByParentId[child.parentAnnotationId] << child
            }
            frontier = children
        }
        Map<UUID, List<ItemLinkAnnotationDTO>> topLevelByItemId = [:]
        allAnnotations.each { Annotation annotation ->
            if (!topLevelByItemId.containsKey(annotation.multiFacetAwareItemId)) {
                topLevelByItemId[annotation.multiFacetAwareItemId] = []
            }
            topLevelByItemId[annotation.multiFacetAwareItemId] << toAnnotationDTO(annotation, childrenByParentId)
        }
        graph.items.each { ItemLinkItemDTO dto ->
            dto.annotations = (topLevelByItemId[dto.id] ?: []).sort(false) { ItemLinkAnnotationDTO annotation -> annotation.label ?: '' }
        }
    }

    private ItemLinkAnnotationDTO toAnnotationDTO(Annotation annotation, Map<UUID, List<Annotation>> childrenByParentId) {
        new ItemLinkAnnotationDTO(
            id: annotation.id,
            label: annotation.label,
            description: annotation.description,
            childAnnotations: (childrenByParentId[annotation.id] ?: []).sort(false) { Annotation child -> child.label ?: '' }
                .collect { Annotation child -> toAnnotationDTO(child, childrenByParentId) }
        )
    }

    private static String itemKey(AdministeredItem item) {
        "${item.domainType?.toLowerCase()}:${item.id}"
    }
}

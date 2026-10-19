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
import org.maurodata.domain.facet.Annotation
import org.maurodata.domain.facet.Metadata
import org.maurodata.domain.model.AdministeredItem
import org.maurodata.domain.model.Model
import org.maurodata.persistence.cache.FacetCacheableRepository
import org.maurodata.persistence.model.PathRepository
import org.maurodata.security.AccessControlService

@CompileStatic
@Singleton
class ItemLinkGraphService {
    @Inject List<IncomingItemLinkProvider> linkProviders
    @Inject ItemLinkPathResolver itemLinkPathResolver
    @Inject PathRepository pathRepository
    @Inject AccessControlService accessControlService
    @Inject FacetCacheableRepository.MetadataCacheableRepository metadataRepository
    @Inject FacetCacheableRepository.AnnotationCacheableRepository annotationRepository

    List<ItemLinkKindDTO> supportedKinds() {
        providersByType().values().collect { IncomingItemLinkProvider provider ->
            new ItemLinkKindDTO(type: provider.type, label: provider.label)
        }.sort(false) { ItemLinkKindDTO kind -> kind.type }
    }

    ItemLinkGraphDTO traverse(AdministeredItem root, List<String> requestedTypes) {
        if (!requestedTypes) {
            throw new HttpStatusException(HttpStatus.BAD_REQUEST, 'At least one link type must be selected')
        }

        Map<String, IncomingItemLinkProvider> providers = providersByType()
        List<String> types = requestedTypes.findAll { it != null && !it.trim().isEmpty() }.unique()
        if (!types) {
            throw new HttpStatusException(HttpStatus.BAD_REQUEST, 'At least one link type must be selected')
        }
        List<String> unsupported = types.findAll { !providers.containsKey(it) }
        if (unsupported) {
            throw new HttpStatusException(HttpStatus.BAD_REQUEST, "Unsupported link type(s): ${unsupported.join(', ')}")
        }
        List<IncomingItemLinkProvider> selectedProviders = types.collect { providers[it] }
            .sort(false) { IncomingItemLinkProvider provider -> provider.type }

        ItemLinkGraphDTO graph = new ItemLinkGraphDTO()
        LinkedHashMap<String, ItemLinkItemDTO> itemsByKey = new LinkedHashMap<>()
        LinkedHashSet<String> expanded = new LinkedHashSet<>()
        ArrayDeque<AdministeredItem> queue = new ArrayDeque<>()
        addItem(root, graph, itemsByKey)
        queue.add(root)

        while (!queue.isEmpty()) {
            AdministeredItem target = queue.removeFirst()
            String targetKey = itemKey(target)
            if (!expanded.add(targetKey)) continue

            selectedProviders.each { IncomingItemLinkProvider provider ->
                provider.findIncomingLinks(target).sort(false) { IncomingItemLink link -> link.path?.toString() ?: '' }
                    .each { IncomingItemLink link ->
                        ResolvedItemLinkPath resolved = itemLinkPathResolver.resolve(target, link.path)
                        AdministeredItem source = resolved.source
                        graph.links << new ItemLinkDTO(
                            id: link.id,
                            type: link.type,
                            path: resolved.path?.toString(),
                            sourceId: source?.id,
                            sourceDomainType: source?.domainType,
                            targetId: target.id,
                            targetDomainType: target.domainType
                        )
                        if (source) {
                            String sourceKey = itemKey(source)
                            if (!itemsByKey.containsKey(sourceKey)) {
                                addItem(source, graph, itemsByKey)
                                queue.addLast(source)
                            }
                        }
                    }
            }
        }
        graph
    }

    private Map<String, IncomingItemLinkProvider> providersByType() {
        Map<String, IncomingItemLinkProvider> result = [:]
        linkProviders.each { IncomingItemLinkProvider provider -> result[provider.type] = provider }
        result
    }

    private void addItem(AdministeredItem item, ItemLinkGraphDTO graph, Map<String, ItemLinkItemDTO> itemsByKey) {
        pathRepository.readParentItems(item)
        item.updatePath()
        Model owner = item.owner
        if (owner) {
            pathRepository.readParentItems(owner)
            owner.updatePath()
        }

        List<Metadata> metadata = metadataRepository.readAllByMultiFacetAwareItemIdIn([item.id]) as List<Metadata>
        List<Annotation> annotations = annotationRepository.readAllByMultiFacetAwareItemIdInAndParentAnnotationIdIsNull([item.id]) as List<Annotation>
        ItemLinkItemDTO dto = new ItemLinkItemDTO(
            id: item.id,
            domainType: item.domainType,
            label: item.label,
            description: item.description,
            owningModel: owner ? new ItemLinkModelDTO(
                id: owner.id,
                domainType: owner.domainType,
                label: owner.label,
                finalised: owner.finalised,
                branchName: owner.branchName,
                modelVersion: owner.modelVersion,
                modelVersionTag: owner.modelVersionTag
            ) : null,
            metadata: metadata.collect { Metadata value ->
                new ItemLinkMetadataDTO(id: value.id, namespace: value.namespace, key: value.key, value: value.value)
            }.sort(false) { ItemLinkMetadataDTO value -> "${value.namespace ?: ''}|${value.key ?: ''}" },
            annotations: annotations.collect { Annotation annotation -> toAnnotationDTO(annotation) }
                .sort(false) { ItemLinkAnnotationDTO annotation -> annotation.label ?: '' }
        )
        itemsByKey[itemKey(item)] = dto
        graph.items << dto
    }

    private ItemLinkAnnotationDTO toAnnotationDTO(Annotation annotation) {
        List<Annotation> children = annotation.childAnnotations ?: (annotation.id ?
            annotationRepository.readAllByParentAnnotationIdIn([annotation.id]) as List<Annotation> : [])
        new ItemLinkAnnotationDTO(
            id: annotation.id,
            label: annotation.label,
            description: annotation.description,
            childAnnotations: children.collect { Annotation child -> toAnnotationDTO(child) }
                .sort(false) { ItemLinkAnnotationDTO child -> child.label ?: '' }
        )
    }

    private static String itemKey(AdministeredItem item) {
        "${item.domainType?.toLowerCase()}:${item.id}"
    }
}

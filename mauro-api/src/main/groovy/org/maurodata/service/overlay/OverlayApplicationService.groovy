package org.maurodata.service.overlay

import groovy.transform.CompileStatic
import jakarta.inject.Singleton
import io.micronaut.http.HttpStatus
import io.micronaut.http.exceptions.HttpStatusException
import io.micronaut.transaction.annotation.Transactional
import org.maurodata.api.overlay.OverlayApplicationDTO
import org.maurodata.api.overlay.OverlayApplicationResponseDTO
import org.maurodata.api.overlay.OverlayMatchRequestDTO
import org.maurodata.api.overlay.OverlayMatchResultDTO
import org.maurodata.domain.facet.Metadata
import org.maurodata.domain.model.AdministeredItem
import org.maurodata.domain.model.Model
import org.maurodata.domain.overlay.Overlay
import org.maurodata.domain.overlay.OverlayAction
import org.maurodata.domain.overlay.OverlayActionPolicy
import org.maurodata.domain.overlay.OverlayMatch
import org.maurodata.domain.overlay.OverlayRule
import org.maurodata.persistence.ContentsService
import org.maurodata.persistence.cache.FacetCacheableRepository.MetadataCacheableRepository
import org.maurodata.persistence.model.PathRepository
import org.maurodata.persistence.service.RepositoryService
import org.maurodata.visitor.GenericDomainTraversalVisitor

@CompileStatic
@Singleton
class OverlayApplicationService {
    static final String NAMESPACE = 'org.maurodata.overlay'
    static final String SOURCES_KEY = 'source'

    final RepositoryService repositoryService
    final ContentsService contentsService
    final PathRepository pathRepository
    final MetadataCacheableRepository metadataRepository

    OverlayApplicationService(RepositoryService repositoryService, ContentsService contentsService,
                              PathRepository pathRepository, MetadataCacheableRepository metadataRepository) {
        this.repositoryService = repositoryService
        this.contentsService = contentsService
        this.pathRepository = pathRepository
        this.metadataRepository = metadataRepository
    }

    @Transactional
    OverlayApplicationResponseDTO apply(OverlayApplicationDTO request) {
        if (!request?.rootDomainType || !request.rootId || request.overlayIds == null) {
            throw new HttpStatusException(HttpStatus.BAD_REQUEST, 'rootDomainType, rootId and overlayIds are required')
        }
        AdministeredItem root = repositoryService.getAdministeredItemRepository(request.rootDomainType)?.readById(request.rootId) as AdministeredItem
        if (!root) throw new HttpStatusException(HttpStatus.NOT_FOUND, 'Root item not found')
        contentsService.loadWithContent(root)
        pathRepository.readParentItems(root)
        root.updatePath()

        List<Overlay> overlays = readOverlays(request.overlayIds)
        List<AdministeredItem> items = collectItems(root)

        List<PlannedAction> plan = []
        overlays.each { Overlay overlay ->
            overlay.overlayRules?.each { OverlayRule overlayRule ->
                List<AdministeredItem> matches = items.findAll { AdministeredItem item -> matchesPath(item, root, overlayRule.match) }
                if (matches.size() > 1 && !(overlayRule.match?.allowMultipleModels)) {
                    Set<UUID> matchingModels = matches.collect { nearestModelId(it, root) }.findAll { it != null } as Set<UUID>
                    if (matchingModels.size() > 1) throw new HttpStatusException(HttpStatus.CONFLICT, "Overlay [$overlay.label] OverlayRule is ambiguous across Models; specify modelIdentifier or allowMultipleModels")
                }
                matches.each { AdministeredItem item -> overlayRule.actions?.each { OverlayAction action ->
                    String pathAttribute = overlayRule.match?.path?.attribute
                    if (pathAttribute && action.field && normalizeField(action.field) != pathAttribute) {
                        throw new HttpStatusException(HttpStatus.BAD_REQUEST, 'Overlay match attribute and action field must agree')
                    }
                    plan.add(new PlannedAction(overlay, item, action, action.field ?: pathAttribute))
                } }
            }
        }

        int applied = 0
        plan.each { PlannedAction planned -> if (applyAction(planned.item, planned.action, planned.field)) applied++ }
        new OverlayApplicationResponseDTO(rootDomainType: root.domainType, rootId: root.id,
            overlayIds: overlays*.id, matchedActions: plan.size(), appliedActions: applied)
    }

    @Transactional(readOnly = true)
    List<OverlayMatchResultDTO> matches(OverlayMatchRequestDTO request) {
        if (!request?.itemDomainType || !request.itemId) throw new HttpStatusException(HttpStatus.BAD_REQUEST, 'itemDomainType and itemId are required')
        AdministeredItem root = loadRoot(request.rootDomainType, request.rootId)
        List<AdministeredItem> items = collectItems(root)
        AdministeredItem target = items.find { it.domainType.equalsIgnoreCase(request.itemDomainType) && it.id == request.itemId }
        if (!target) throw new HttpStatusException(HttpStatus.NOT_FOUND, 'Target item is not within the supplied root')
        List<OverlayMatchResultDTO> result = []
        readOverlays(request.overlayIds).each { Overlay overlay ->
            overlay.overlayRules?.each { OverlayRule overlayRule ->
                if (matchesPath(target, root, overlayRule.match)) {
                    result.add(new OverlayMatchResultDTO(overlayId: overlay.id, overlayRuleId: overlayRule.id,
                        overlayActionIds: overlayRule.actions*.id))
                }
            }
        }
        result
    }

    private AdministeredItem loadRoot(String domainType, UUID id) {
        if (!domainType || !id) throw new HttpStatusException(HttpStatus.BAD_REQUEST, 'rootDomainType and rootId are required')
        AdministeredItem root = repositoryService.getAdministeredItemRepository(domainType)?.readById(id) as AdministeredItem
        if (!root) throw new HttpStatusException(HttpStatus.NOT_FOUND, 'Root item not found')
        contentsService.loadWithContent(root)
        pathRepository.readParentItems(root)
        root.updatePath()
        root
    }

    private List<Overlay> readOverlays(List<UUID> ids) {
        if (ids == null) throw new HttpStatusException(HttpStatus.BAD_REQUEST, 'overlayIds is required')
        ids.collect { UUID id ->
            Overlay overlay = repositoryService.getModelRepository(Overlay)?.readById(id) as Overlay
            if (!overlay) throw new HttpStatusException(HttpStatus.NOT_FOUND, "Overlay not found: $id")
            overlay
        }
    }

    private static List<AdministeredItem> collectItems(AdministeredItem root) {
        List<AdministeredItem> items = []
        GenericDomainTraversalVisitor visitor = new GenericDomainTraversalVisitor()
        visitor.onEnter(AdministeredItem, { AdministeredItem item ->
            item.updatePath()
            items.add(item)
        })
        root.accept(visitor)
        items
    }

    private static boolean matchesPath(AdministeredItem item, AdministeredItem root, OverlayMatch match) {
        item.path?.matchesRelativeTo(root.path, match?.path)
    }

    private static UUID nearestModelId(AdministeredItem item, AdministeredItem root) {
        AdministeredItem current = item
        while (current && current != root) {
            if (current instanceof Model) return current.id
            current = current.parent
        }
        root instanceof Model ? root.id : null
    }

    private boolean applyAction(AdministeredItem item, OverlayAction action, String requestedField) {
        if (!action || isEmptyValue(action.value)) return false
        OverlayActionPolicy policy
        try { policy = OverlayActionPolicy.fromIdentifier(action.policy ?: 'always') }
        catch (IllegalArgumentException exception) { throw new HttpStatusException(HttpStatus.BAD_REQUEST, exception.message) }

        boolean accepted
        if ((action.type ?: '').equalsIgnoreCase('value')) {
            String field = normalizeField(requestedField)
            Class propertyType = writableScalarPropertyType(item, field)
            Object before = item[field]
            accepted = policy == OverlayActionPolicy.ALWAYS ||
                (policy == OverlayActionPolicy.OVERWRITE_IF_EMPTY && isEmptyValue(before))
            if (policy == OverlayActionPolicy.ADD) throw new HttpStatusException(HttpStatus.BAD_REQUEST, 'Add policy is only supported for metadata values')
            if (accepted) {
                item[field] = convertValue(action.value, propertyType, field)
                repositoryService.getAdministeredItemRepository(item.class).update(item)
            }
        } else if ((action.type ?: '').equalsIgnoreCase('metadata')) {
            if (!action.namespace || !action.key) throw new HttpStatusException(HttpStatus.BAD_REQUEST, 'Metadata actions require namespace and key')
            String incoming = scalarString(action.value)
            Metadata facet = item.metadata?.find { it.namespace == action.namespace && it.key == action.key }
            String before = facet?.value
            String result
            if (policy == OverlayActionPolicy.ALWAYS) result = incoming
            else if (policy == OverlayActionPolicy.OVERWRITE_IF_EMPTY) result = isEmptyValue(before) ? incoming : before
            else result = mergeCsv(before, incoming)
            accepted = policy == OverlayActionPolicy.ALWAYS || policy == OverlayActionPolicy.ADD || isEmptyValue(before)
            if (accepted) {
                if (!facet) {
                    facet = new Metadata(id: UUID.randomUUID(), namespace: action.namespace, key: action.key, value: result, multiFacetAwareItem: item,
                                         multiFacetAwareItemId: item.id, multiFacetAwareItemDomainType: item.domainType)
                    item.metadata.add(facet)
                    metadataRepository.save(facet)
                } else {
                    facet.value = result
                    metadataRepository.update(facet)
                }
            }
        } else {
            throw new HttpStatusException(HttpStatus.BAD_REQUEST, "Unsupported Overlay action type: $action.type")
        }
        if (accepted) ensureSource(item, action.source)
        accepted
    }

    private static String normalizeField(String field) {
        field?.startsWith('@') ? field.substring(1) : field
    }

    private static Class writableScalarPropertyType(AdministeredItem item, String field) {
        if (!field) throw new HttpStatusException(HttpStatus.BAD_REQUEST, 'Overlay value actions require a field or path attribute')
        Set<String> protectedFields = ['id', 'version', 'dateCreated', 'lastUpdated', 'createdBy', 'catalogueUser',
            'domainType', 'path', 'localPath', 'breadcrumbs', 'availableActions', 'parent', 'owner', 'folder',
            'metadata', 'classifiers', 'edits', 'summaryMetadata', 'rules', 'annotations', 'referenceFiles',
            'semanticLinks', 'modelVersion', 'modelVersionTag', 'branchName', 'modelType'] as Set<String>
        if (protectedFields.contains(field)) throw new HttpStatusException(HttpStatus.BAD_REQUEST, "Protected Overlay value field: $field")
        def property = item.hasProperty(field)
        if (!property?.properties?.setter) throw new HttpStatusException(HttpStatus.BAD_REQUEST, "Unsupported Overlay value field: $field")
        Class propertyType = property.type
        if (Collection.isAssignableFrom(propertyType) || Map.isAssignableFrom(propertyType) || propertyType.isArray() ||
            !(CharSequence.isAssignableFrom(propertyType) || propertyType == Boolean || Number.isAssignableFrom(propertyType) || propertyType.isEnum())) {
            throw new HttpStatusException(HttpStatus.BAD_REQUEST, "Overlay value field must be a writable scalar property: $field")
        }
        propertyType
    }

    private static Object convertValue(Object value, Class type, String field) {
        if (type.isInstance(value)) return value
        String text = scalarString(value)
        try {
            if (type == String || CharSequence.isAssignableFrom(type)) return text
            if (type == Boolean || type == boolean) {
                if (!text.equalsIgnoreCase('true') && !text.equalsIgnoreCase('false')) throw new IllegalArgumentException()
                return Boolean.valueOf(text)
            }
            if (Number.isAssignableFrom(type)) {
                BigDecimal number = new BigDecimal(text)
                if (type == Integer || type == int) return number.intValueExact()
                if (type == Long || type == long) return number.longValueExact()
                if (type == Short || type == short) return number.shortValueExact()
                if (type == Byte || type == byte) return number.byteValueExact()
                if (type == Float || type == float) return number.floatValue()
                if (type == Double || type == double) return number.doubleValue()
                if (type == BigDecimal) return number
            }
            if (type.isEnum()) {
                for (Object enumConstant : type.enumConstants) {
                    Enum enumValue = (Enum) enumConstant
                    if (enumValue.name() == text) return enumValue
                }
                throw new IllegalArgumentException("Unknown enum value: $text")
            }
        } catch (Exception ignored) {
            throw new HttpStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Value cannot be converted for Overlay field: $field")
        }
        throw new HttpStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Value cannot be converted for Overlay field: $field")
    }

    private static String scalarString(Object value) {
        if (value instanceof Collection || value instanceof Map || value.getClass().isArray()) {
            throw new HttpStatusException(HttpStatus.BAD_REQUEST, 'Overlay values must be scalar')
        }
        String.valueOf(value)
    }

    static boolean isEmptyValue(Object value) {
        if (value == null) return true
        if (value instanceof CharSequence) return value.toString().trim().length() == 0
        if (value instanceof Collection) return ((Collection) value).isEmpty()
        if (value instanceof Map) return ((Map) value).isEmpty()
        false
    }

    private void ensureSource(AdministeredItem item, String source) {
        if (!source?.trim()) return
        Metadata sources = item.metadata.find { it.namespace == NAMESPACE && it.key == SOURCES_KEY }
        List<String> values = parseCsv(sources?.value)
        if (!values.contains(source)) values.add(source)
        String value = encodeCsv(values)
        if (!sources) {
            sources = new Metadata(id: UUID.randomUUID(), namespace: NAMESPACE, key: SOURCES_KEY, value: value, multiFacetAwareItem: item,
                                  multiFacetAwareItemId: item.id, multiFacetAwareItemDomainType: item.domainType)
            item.metadata.add(sources)
            metadataRepository.save(sources)
        } else {
            sources.value = value
            metadataRepository.update(sources)
        }
    }

    private static String mergeCsv(String current, String incoming) {
        List<String> merged = parseCsv(current)
        parseCsv(incoming).each { if (!merged.contains(it)) merged.add(it) }
        encodeCsv(merged)
    }

    private static List<String> parseCsv(String text) {
        List<String> fields = []
        if (text == null || text.isEmpty()) return fields
        StringBuilder field = new StringBuilder()
        boolean quoted = false
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i)
            if (ch == '"' as char) {
                if (quoted && i + 1 < text.length() && text.charAt(i + 1) == '"' as char) { field.append('"'); i++ }
                else quoted = !quoted
            } else if (ch == ',' as char && !quoted) { fields.add(field.toString()); field.setLength(0) }
            else field.append(ch)
        }
        fields.add(field.toString())
        fields
    }

    private static String encodeCsv(List<String> fields) {
        fields.collect { String value ->
            value.contains(',') || value.contains('"') || value.contains('\n') ? '"' + value.replace('"', '""') + '"' : value
        }.join(',')
    }

    private static class PlannedAction {
        final Overlay overlay
        final AdministeredItem item
        final OverlayAction action
        final String field
        PlannedAction(Overlay overlay, AdministeredItem item, OverlayAction action, String field) { this.overlay = overlay; this.item = item; this.action = action; this.field = field }
    }
}

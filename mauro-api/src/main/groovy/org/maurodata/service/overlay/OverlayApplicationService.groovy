package org.maurodata.service.overlay

import groovy.transform.CompileStatic
import jakarta.inject.Singleton
import io.micronaut.http.HttpStatus
import io.micronaut.http.exceptions.HttpStatusException
import io.micronaut.transaction.annotation.Transactional
import org.maurodata.api.overlay.OverlayApplicationDTO
import org.maurodata.api.overlay.OverlayApplicationResponseDTO
import org.maurodata.api.overlay.OverlayApplicationMatchSampleDTO
import org.maurodata.api.overlay.OverlayApplicationActionSampleDTO
import org.maurodata.api.overlay.OverlayAnnotationActionResultDTO
import org.maurodata.api.overlay.OverlayMatchRequestDTO
import org.maurodata.api.overlay.OverlayMatchResultDTO
import org.maurodata.domain.facet.Metadata
import org.maurodata.domain.facet.Annotation
import org.maurodata.domain.model.AdministeredItem
import org.maurodata.domain.model.Model
import org.maurodata.domain.security.Role
import org.maurodata.domain.model.Path
import org.maurodata.domain.folder.Folder
import org.maurodata.domain.datamodel.DataClass
import org.maurodata.domain.datamodel.DataElement
import org.maurodata.domain.datamodel.DataModel
import org.maurodata.domain.overlay.Overlay
import org.maurodata.domain.overlay.OverlayAction
import org.maurodata.domain.overlay.OverlayActionPolicy
import org.maurodata.domain.model.ModelPathMatch
import org.maurodata.domain.overlay.OverlayRule
import org.maurodata.persistence.ContentsService
import org.maurodata.persistence.cache.FacetCacheableRepository.MetadataCacheableRepository
import org.maurodata.persistence.cache.FacetCacheableRepository.AnnotationCacheableRepository
import org.maurodata.persistence.model.PathRepository
import org.maurodata.persistence.datamodel.DataClassRepository
import org.maurodata.persistence.datamodel.DataElementRepository
import org.maurodata.persistence.cache.ModelCacheableRepository.DataModelCacheableRepository
import org.maurodata.persistence.cache.ModelCacheableRepository.FolderCacheableRepository
import org.maurodata.persistence.overlay.OverlayRuleRepository
import org.maurodata.persistence.overlay.OverlayRuleTargetDTO
import org.maurodata.persistence.service.RepositoryService
import org.maurodata.visitor.GenericDomainTraversalVisitor
import org.maurodata.security.AccessControlService

import java.util.IdentityHashMap

@CompileStatic
@Singleton
class OverlayApplicationService {
    static final String NAMESPACE = 'org.maurodata.overlay'
    static final String SOURCES_KEY = 'source'
    static final int MAX_ANNOTATION_MATCHED_PATHS = 50

    final RepositoryService repositoryService
    final ContentsService contentsService
    final PathRepository pathRepository
    final MetadataCacheableRepository metadataRepository
    final AnnotationCacheableRepository annotationRepository
    final OverlayRuleRepository overlayRuleRepository
    final AccessControlService accessControlService
    @jakarta.inject.Inject DataClassRepository dataClassRepository
    @jakarta.inject.Inject DataElementRepository dataElementRepository
    @jakarta.inject.Inject DataModelCacheableRepository dataModelRepository
    @jakarta.inject.Inject FolderCacheableRepository folderRepository

    OverlayApplicationService(RepositoryService repositoryService, ContentsService contentsService,
                              PathRepository pathRepository, MetadataCacheableRepository metadataRepository,
                              AnnotationCacheableRepository annotationRepository, OverlayRuleRepository overlayRuleRepository,
                              AccessControlService accessControlService) {
        this.repositoryService = repositoryService
        this.contentsService = contentsService
        this.pathRepository = pathRepository
        this.metadataRepository = metadataRepository
        this.annotationRepository = annotationRepository
        this.overlayRuleRepository = overlayRuleRepository
        this.accessControlService = accessControlService
    }

    @Transactional
    OverlayApplicationResponseDTO apply(OverlayApplicationDTO request) {
        if (!request?.rootDomainType || !request.rootId || request.overlayIds == null) {
            throw new HttpStatusException(HttpStatus.BAD_REQUEST, 'rootDomainType, rootId and overlayIds are required')
        }
        AdministeredItem root = repositoryService.getAdministeredItemRepository(request.rootDomainType)?.readById(request.rootId) as AdministeredItem
        if (!root) throw new HttpStatusException(HttpStatus.NOT_FOUND, 'Root item not found')
        if (!request.dryRun && root instanceof Model && root.finalised) {
            throw new HttpStatusException(HttpStatus.UNPROCESSABLE_ENTITY, 'Cannot apply an Overlay to a finalised root')
        }
        if (request.onlyNewItemsComparedWithRootId && !(root instanceof DataModel)) {
            throw new HttpStatusException(HttpStatus.BAD_REQUEST,
                'onlyNewItemsComparedWithRootId requires a DataModel root')
        }
        pathRepository.readParentItems(root)
        root.updatePath()

        DataModel comparisonRoot
        List<AdministeredItem> comparisonItems = []
        if (request.onlyNewItemsComparedWithRootId) {
            comparisonRoot = dataModelRepository.loadWithContent(request.onlyNewItemsComparedWithRootId)
            if (!comparisonRoot) throw new HttpStatusException(HttpStatus.NOT_FOUND, 'Comparison DataModel not found')
            accessControlService.checkRole(Role.READER, comparisonRoot)
            comparisonRoot.setAssociations()
            pathRepository.readParentItems(comparisonRoot)
            comparisonRoot.updatePath()
            comparisonItems = collectItems(comparisonRoot)
        }

        List<Overlay> overlays = readOverlays(request.overlayIds)
        IdentityHashMap<OverlayRule, List<AdministeredItem>> indexedMatches
        List<AdministeredItem> items = []
        if (canResolveDataModelRules(overlays)) {
            indexedMatches = findDataModelRuleMatches(root, overlays)
        } else {
            contentsService.loadWithContent(root)
            pathRepository.readParentItems(root)
            root.updatePath()
            items = collectItems(root)
            indexedMatches = indexSuffixMatches(items, overlays)
        }

        List<PlannedAction> plan = []
        int matchedActionCount = 0
        OverlayApplicationMatchSampleDTO sampleMatch
        List<OverlayAnnotationActionResultDTO> annotationActionResults = []
        IdentityHashMap<OverlayAction, OverlayAnnotationActionResultDTO> annotationTraces = new IdentityHashMap<>()
        overlays.each { Overlay overlay ->
            overlay.overlayRules?.each { OverlayRule overlayRule ->
                List<AdministeredItem> matches = indexedMatches.containsKey(overlayRule) ? indexedMatches.get(overlayRule) :
                    items.findAll { AdministeredItem item -> matchesPath(item, root, overlayRule.match) }
                if (matches.size() > 1 && !(overlayRule.match?.allowMultipleModels)) {
                    Set<UUID> matchingModels = matches.collect { nearestModelId(it, root) }.findAll { it != null } as Set<UUID>
                    if (matchingModels.size() > 1) throw new HttpStatusException(HttpStatus.CONFLICT, "Overlay [$overlay.label] OverlayRule is ambiguous across Models; specify modelIdentifier or allowMultipleModels")
                }
                overlayRule.actions?.each { OverlayAction action ->
                    if ((action.type ?: '').equalsIgnoreCase('annotation') && !annotationTraces.containsKey(action)) {
                        OverlayAnnotationActionResultDTO result = new OverlayAnnotationActionResultDTO(
                            overlayId: overlay.id, overlayRuleId: overlayRule.id, actionId: action.id,
                            label: action.label, selectorPath: overlayRule.match?.path?.toString())
                        annotationTraces.put(action, result)
                        annotationActionResults.add(result)
                    }
                }
                if (sampleMatch == null && matches) {
                    AdministeredItem item = matches.first()
                    sampleMatch = new OverlayApplicationMatchSampleDTO(overlayId: overlay.id,
                        overlayRuleId: overlayRule.id, itemDomainType: item.domainType, itemId: item.id,
                        itemLabel: item.label, itemPath: item.path?.toString(),
                        selectorPath: overlayRule.match?.path?.toString(),
                        actionIds: overlayRule.actions*.id,
                        actions: overlayRule.actions.collect { OverlayAction action -> sampleAction(action) })
                }
                matches.each { AdministeredItem item -> overlayRule.actions?.each { OverlayAction action ->
                    matchedActionCount++
                    String pathAttribute = overlayRule.match?.path?.attribute
                    if (pathAttribute && action.field && normalizeField(action.field) != pathAttribute) {
                        throw new HttpStatusException(HttpStatus.BAD_REQUEST, 'Overlay match attribute and action field must agree')
                    }
                    OverlayAnnotationActionResultDTO annotationTrace = annotationTraces.get(action)
                    if (annotationTrace) {
                        annotationTrace.matchedCount++
                        if (annotationTrace.matchedPaths.size() < MAX_ANNOTATION_MATCHED_PATHS) {
                            annotationTrace.matchedPaths.add(item.path?.toString())
                        } else {
                            annotationTrace.matchedPathsTruncated = true
                        }
                    }
                    if (comparisonRoot && comparisonItems.any { AdministeredItem oldItem ->
                        matchesPath(oldItem, comparisonRoot, overlayRule.match)
                    }) return
                    plan.add(new PlannedAction(overlay, overlayRule, item, action, action.field ?: pathAttribute, annotationTrace))
                } }
            }
        }

        PlannedAction finalisedTarget = plan.find { PlannedAction planned -> planned.item.owner?.finalised }
        if (finalisedTarget && !request.dryRun) {
            throw new HttpStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                "Cannot apply an Overlay to an item owned by a finalised Model: ${finalisedTarget.item.path}")
        }

        int applied = 0
        plan.each { PlannedAction planned ->
            if (applyAction(planned.item, planned.action, planned.field, request.dryRun)) {
                applied++
                if (planned.annotationTrace) {
                    if (request.dryRun) planned.annotationTrace.wouldApplyCount++
                    else planned.annotationTrace.appliedCount++
                }
            }
        }
        new OverlayApplicationResponseDTO(rootDomainType: root.domainType, rootId: root.id,
            overlayIds: overlays*.id, matchedActions: matchedActionCount,
            appliedActions: request.dryRun ? 0 : applied, dryRun: request.dryRun,
            wouldApplyActions: applied, skippedActions: matchedActionCount - applied, sampleMatch: sampleMatch,
            annotationActionResults: annotationActionResults)
    }

    @Transactional(readOnly = true)
    List<OverlayMatchResultDTO> matches(OverlayMatchRequestDTO request) {
        if (!request?.itemDomainType || !request.itemId) throw new HttpStatusException(HttpStatus.BAD_REQUEST, 'itemDomainType and itemId are required')
        AdministeredItem root = loadRoot(request.rootDomainType, request.rootId)
        AdministeredItem target = repositoryService.getAdministeredItemRepository(request.itemDomainType)?.readById(request.itemId) as AdministeredItem
        if (target) {
            pathRepository.readParentItems(target)
            target.updatePath()
        }
        if (!target || !target.path?.matchesRelativeTo(root.path, new Path(''), false)) {
            throw new HttpStatusException(HttpStatus.NOT_FOUND, 'Target item is not within the supplied root')
        }
        List<Overlay> overlays = readOverlays(request.overlayIds)
        List<OverlayMatchResultDTO> result = []
        overlays.each { Overlay overlay ->
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
        pathRepository.readParentItems(root)
        root.updatePath()
        root
    }

    private List<Overlay> readOverlays(List<UUID> ids) {
        if (ids == null) throw new HttpStatusException(HttpStatus.BAD_REQUEST, 'overlayIds is required')
        ids.collect { UUID id ->
            Overlay overlay = repositoryService.getModelRepository(Overlay)?.readById(id) as Overlay
            if (!overlay) throw new HttpStatusException(HttpStatus.NOT_FOUND, "Overlay not found: $id")
            overlay.overlayRules = overlayRuleRepository.findAllByOverlayIdOrderByOrderAsc(id)
            overlay
        }
    }

    private static boolean canResolveDataModelRules(List<Overlay> overlays) {
        for (Overlay overlay : overlays) {
            if (overlay.overlayRules == null) continue
            for (OverlayRule rule : overlay.overlayRules) {
                List<Path.PathNode> nodes = rule.match?.path?.nodes ?: []
                if (nodes.isEmpty()) continue
                if (!nodes[0].prefix?.equalsIgnoreCase('dc')) return false
                if (nodes.size() == 1) continue
                if (nodes.size() != 2 || !nodes[1].prefix?.equalsIgnoreCase('de')) return false
            }
        }
        true
    }

    /**
     * Resolve DataClass and DataElement selectors in SQL and hydrate only the resulting items.
     * The path matcher still verifies root scoping and the complete selector against each
     * candidate, including model identifiers and whole-path suffix behavior.
     */
    private IdentityHashMap<OverlayRule, List<AdministeredItem>> findDataModelRuleMatches(
        AdministeredItem root, List<Overlay> overlays) {
        IdentityHashMap<OverlayRule, List<AdministeredItem>> matches = new IdentityHashMap<>()
        Map<UUID, OverlayRule> ruleById = [:]
        overlays.each { Overlay overlay ->
            overlay.overlayRules?.each { OverlayRule rule ->
                matches.put(rule, [])
                ruleById.put(rule.id, rule)
                if (!rule.match?.path?.nodes) {
                    if (matchesPath(root, root, rule.match)) matches.get(rule).add(root)
                }
            }
        }
        List<UUID> modelIds = modelIdsWithin(root)
        if (!modelIds) return matches
        List<OverlayRuleTargetDTO> targets = overlayRuleRepository.findDataModelRuleTargets(overlays*.id, modelIds)
        Set<UUID> dataClassIds = targets.findAll { OverlayRuleTargetDTO target -> target.itemDomainType == 'DataClass' }*.itemId as Set<UUID>
        Set<UUID> dataElementIds = targets.findAll { OverlayRuleTargetDTO target -> target.itemDomainType == 'DataElement' }*.itemId as Set<UUID>
        Map<UUID, AdministeredItem> itemsById = [:]
        if (dataClassIds) {
            dataClassRepository.findAllByIdIn(dataClassIds).each { DataClass item -> itemsById.put(item.id, item) }
        }
        if (dataElementIds) {
            dataElementRepository.findAllByIdIn(dataElementIds).each { DataElement item -> itemsById.put(item.id, item) }
        }
        itemsById.values().each { AdministeredItem item ->
            pathRepository.readParentItems(item)
            item.updatePath()
        }
        targets.each { OverlayRuleTargetDTO target ->
            OverlayRule rule = ruleById[target.overlayRuleId]
            AdministeredItem item = itemsById[target.itemId]
            if (rule && item && matchesPath(item, root, rule.match)) matches.get(rule).add(item)
        }
        matches
    }

    private List<UUID> modelIdsWithin(AdministeredItem root) {
        if (root instanceof DataModel) return [root.id]
        if (!(root instanceof Folder)) {
            Model owner = root.owner
            if (owner instanceof DataModel) return [owner.id]
        }
        if (!(root instanceof Folder)) return []

        Set<UUID> folderIds = [root.id] as Set<UUID>
        Set<UUID> frontier = [root.id] as Set<UUID>
        while (frontier) {
            List<Folder> children = folderRepository.readAllByFolderIdIn(frontier)
            frontier = children*.id.findAll { UUID id -> folderIds.add(id) } as Set<UUID>
        }
        dataModelRepository.readAllByFolderIdIn(folderIds)*.id as List<UUID>
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

    private static boolean matchesPath(AdministeredItem item, AdministeredItem root, ModelPathMatch match) {
        item.path?.matchesRelativeTo(root.path, match?.path, true, match?.matchPathSuffix ?: false,
            match?.matchFirstNodeIdentifierSuffix ?: false)
    }

    /**
     * Index selectors whose first node uses an explicit identifier-suffix match,
     * optionally followed by an exact DataElement identifier. Other selector forms
     * retain the general matcher.
     */
    private static IdentityHashMap<OverlayRule, List<AdministeredItem>> indexSuffixMatches(
        List<AdministeredItem> items, List<Overlay> overlays) {
        IdentityHashMap<OverlayRule, List<AdministeredItem>> matchesByRule = new IdentityHashMap<>()
        SuffixMatchNode trie = new SuffixMatchNode()

        overlays.each { Overlay overlay ->
            overlay.overlayRules?.each { OverlayRule rule ->
                List<Path.PathNode> selector = rule.match?.path?.nodes
                if (!rule.match?.matchPathSuffix || !selector || selector[0].prefix?.toLowerCase() != 'dc' ||
                    !rule.match?.matchFirstNodeIdentifierSuffix || selector.size() > 2 ||
                    (selector.size() == 2 && selector[1].prefix?.toLowerCase() != 'de')) return

                matchesByRule.put(rule, [])
                SuffixMatchNode node = trie
                String tableSuffix = selector[0].identifier
                for (int i = tableSuffix.length() - 1; i >= 0; i--) {
                    Character character = tableSuffix.charAt(i)
                    if (!node.children.containsKey(character)) node.children.put(character, new SuffixMatchNode())
                    node = node.children.get(character)
                }
                if (selector.size() == 1) {
                    node.tableRules.add(rule)
                } else {
                    String columnIdentifier = selector[1].identifier
                    if (!node.elementRules.containsKey(columnIdentifier)) node.elementRules.put(columnIdentifier, [])
                    node.elementRules.get(columnIdentifier).add(rule)
                }
            }
        }

        if (matchesByRule.isEmpty()) return matchesByRule

        items.each { AdministeredItem item ->
            List<Path.PathNode> pathNodes = item.path?.nodes
            if (!pathNodes) return
            Path.PathNode terminal = pathNodes.last()
            boolean isDataElement = terminal.prefix?.equalsIgnoreCase('de')
            boolean isDataClass = terminal.prefix?.equalsIgnoreCase('dc')
            if (!isDataElement && !isDataClass) return
            if (isDataElement && pathNodes.size() < 2) return

            String tableIdentifier = isDataClass ? terminal.identifier : pathNodes[pathNodes.size() - 2].identifier
            if (isDataElement && !pathNodes[pathNodes.size() - 2].prefix?.equalsIgnoreCase('dc')) return
            SuffixMatchNode node = trie
            collectSuffixMatches(node, item, isDataElement ? terminal.identifier : null, matchesByRule)
            for (int i = tableIdentifier.length() - 1; i >= 0; i--) {
                node = node.children.get(tableIdentifier.charAt(i))
                if (node == null) break
                collectSuffixMatches(node, item, isDataElement ? terminal.identifier : null, matchesByRule)
            }
        }
        matchesByRule
    }

    private static void collectSuffixMatches(SuffixMatchNode node, AdministeredItem item, String elementIdentifier,
                                             IdentityHashMap<OverlayRule, List<AdministeredItem>> matchesByRule) {
        if (elementIdentifier == null) {
            node.tableRules.each { OverlayRule rule -> matchesByRule.get(rule).add(item) }
        } else {
            node.elementRules.get(elementIdentifier)?.each { OverlayRule rule -> matchesByRule.get(rule).add(item) }
        }
    }

    private static OverlayApplicationActionSampleDTO sampleAction(OverlayAction action) {
        String value = action.value?.toString() ?: ''
        int previewLength = Math.min(value.length(), 240)
        String description = action.description ?: ''
        int descriptionPreviewLength = Math.min(description.length(), 240)
        new OverlayApplicationActionSampleDTO(id: action.id, type: action.type, field: action.field,
            namespace: action.namespace, key: action.key, label: action.label, policy: action.policy, source: action.source,
            valuePreview: value.substring(0, previewLength), valueTruncated: value.length() > previewLength,
            descriptionPreview: description.substring(0, descriptionPreviewLength),
            descriptionTruncated: description.length() > descriptionPreviewLength)
    }

    private static class SuffixMatchNode {
        Map<Character, SuffixMatchNode> children = [:]
        List<OverlayRule> tableRules = []
        Map<String, List<OverlayRule>> elementRules = [:]
    }

    private static UUID nearestModelId(AdministeredItem item, AdministeredItem root) {
        AdministeredItem current = item
        while (current && current != root) {
            if (current instanceof Model) return current.id
            current = current.parent
        }
        root instanceof Model ? root.id : null
    }

    private boolean applyAction(AdministeredItem item, OverlayAction action, String requestedField, boolean dryRun = false) {
        if (!action) return false
        if ((action.type ?: '').equalsIgnoreCase('annotation')) {
            return applyAnnotationAction(item, action, dryRun)
        }
        if (isEmptyValue(action.value)) return false
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
            if (accepted && !dryRun) {
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
            if (accepted && !dryRun) {
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
        if (accepted && !dryRun) ensureSource(item, action.source)
        accepted
    }

    private boolean applyAnnotationAction(AdministeredItem item, OverlayAction action, boolean dryRun = false) {
        if (!action.label?.trim()) throw new HttpStatusException(HttpStatus.BAD_REQUEST, 'Annotation actions require a label')
        if (action.description == null) throw new HttpStatusException(HttpStatus.BAD_REQUEST, 'Annotation actions require a description')
        OverlayActionPolicy policy
        try { policy = OverlayActionPolicy.fromIdentifier(action.policy ?: 'always') }
        catch (IllegalArgumentException exception) { throw new HttpStatusException(HttpStatus.BAD_REQUEST, exception.message) }
        if (policy == OverlayActionPolicy.ADD) throw new HttpStatusException(HttpStatus.BAD_REQUEST, 'Add policy is only supported for metadata values')

        Annotation annotation = item.annotations?.find { Annotation current -> current.label == action.label }
        boolean accepted = policy == OverlayActionPolicy.ALWAYS || isEmptyValue(annotation?.description)
        if (accepted && !dryRun) {
            if (!annotation) {
                annotation = new Annotation(label: action.label, description: action.description,
                    multiFacetAwareItem: item, multiFacetAwareItemId: item.id,
                    multiFacetAwareItemDomainType: item.domainType, catalogueUser: accessControlService.getUser())
                item.annotations.add(annotation)
                annotationRepository.save(annotation)
            } else {
                annotation.description = action.description
                annotationRepository.update(annotation)
            }
        }
        if (accepted && !dryRun) ensureSource(item, action.source)
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
        final OverlayRule rule
        final AdministeredItem item
        final OverlayAction action
        final String field
        final OverlayAnnotationActionResultDTO annotationTrace
        PlannedAction(Overlay overlay, OverlayRule rule, AdministeredItem item, OverlayAction action, String field,
                      OverlayAnnotationActionResultDTO annotationTrace) {
            this.overlay = overlay
            this.rule = rule
            this.item = item
            this.action = action
            this.field = field
            this.annotationTrace = annotationTrace
        }
    }
}

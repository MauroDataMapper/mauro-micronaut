package org.maurodata.service.inspect

import org.maurodata.controller.model.AvailableActions
import org.maurodata.domain.classifier.ClassificationScheme
import org.maurodata.domain.classifier.Classifier
import org.maurodata.domain.dataflow.DataClassComponent
import org.maurodata.domain.dataflow.DataElementComponent
import org.maurodata.domain.dataflow.DataFlow
import org.maurodata.domain.datamodel.DataClass
import org.maurodata.domain.datamodel.DataElement
import org.maurodata.domain.datamodel.DataModel
import org.maurodata.domain.datamodel.DataType
import org.maurodata.domain.datamodel.EnumerationValue
import org.maurodata.domain.facet.Metadata
import org.maurodata.domain.facet.SemanticLink
import org.maurodata.domain.facet.VersionLink
import org.maurodata.domain.folder.Folder
import org.maurodata.domain.model.AdministeredItem
import org.maurodata.domain.model.Model
import org.maurodata.domain.security.Role
import org.maurodata.domain.terminology.CodeSet
import org.maurodata.domain.terminology.Term
import org.maurodata.domain.terminology.Terminology
import org.maurodata.persistence.cache.AdministeredItemCacheableRepository
import org.maurodata.persistence.cache.AdministeredItemCacheableRepository.DataClassCacheableRepository
import org.maurodata.persistence.cache.FacetCacheableRepository
import org.maurodata.persistence.facet.VersionLinkRepository
import org.maurodata.persistence.model.PathRepository
import org.maurodata.persistence.service.RepositoryService
import org.maurodata.security.AccessControlService

import groovy.transform.CompileStatic
import io.micronaut.http.HttpStatus
import io.micronaut.http.exceptions.HttpStatusException
import io.micronaut.security.authentication.AuthorizationException
import jakarta.inject.Inject
import jakarta.inject.Singleton

@CompileStatic
@Singleton
class InspectService {

    private static final Set<String> ALLOWED_DOMAIN_TYPES = [
        ClassificationScheme.simpleName,
        Classifier.simpleName,
        CodeSet.simpleName,
        DataClass.simpleName,
        DataClassComponent.simpleName,
        DataElement.simpleName,
        DataElementComponent.simpleName,
        DataFlow.simpleName,
        DataModel.simpleName,
        DataType.simpleName,
        DataType.DataTypeKind.PRIMITIVE_TYPE.stringValue,
        DataType.DataTypeKind.ENUMERATION_TYPE.stringValue,
        DataType.DataTypeKind.REFERENCE_TYPE.stringValue,
        DataType.DataTypeKind.MODEL_TYPE.stringValue,
        EnumerationValue.simpleName,
        Folder.simpleName,
        'VersionedFolder',
        Term.simpleName,
        Terminology.simpleName
    ] as Set<String>

    @Inject
    RepositoryService repositoryService

    @Inject
    AccessControlService accessControlService

    @Inject
    PathRepository pathRepository

    @Inject
    DataClassCacheableRepository dataClassRepository

    @Inject
    VersionLinkRepository versionLinkRepository

    @Inject
    FacetCacheableRepository.SemanticLinkCacheableRepository semanticLinkRepository

    Map<String, Object> inspect(String domainType, UUID id) {
        validateDomainType(domainType)

        AdministeredItem item = loadItem(domainType, id)
        accessControlService.checkRole(Role.READER, item)

        toInspectMap(item, true, [] as Set<UUID>)
    }

    String overview(String domainType, UUID id) {
        Map<String, Object> inspected = inspect(domainType, id)

        List<String> lines = []
        List<String> descriptionLines = descriptionLines(inspected)
        if (descriptionLines) {
            appendSection(lines, 'Description', descriptionLines)
        }

        List<String> structureLines = []
        appendTree(inspected, structureLines, '', '', true)
        appendSection(lines, 'Structure', structureLines)

        List<String> versionLines = collectVersionLines(inspected)
        if (versionLines) {
            appendSection(lines, 'Versioning', versionLines)
        }

        List<String> relationshipLines = collectRelationshipLines(inspected)
        if (relationshipLines) {
            appendSection(lines, 'Relationships', relationshipLines)
        }

        lines.join('\n')
    }

    private static List<String> descriptionLines(Map<String, Object> item) {
        String description = item.description?.toString()
        description ? description.readLines() : []
    }

    private static void appendSection(List<String> lines, String title, List<String> content) {
        if (lines) {
            lines << ''
        }
        lines << title
        lines << '-' * title.length()
        lines.addAll(content)
    }

    private void validateDomainType(String domainType) {
        if (!domainType || !ALLOWED_DOMAIN_TYPES.any {String allowed -> allowed.equalsIgnoreCase(domainType)}) {
            throw new HttpStatusException(HttpStatus.NOT_FOUND, "Domain type [$domainType] not found")
        }
    }

    private AdministeredItem loadItem(String domainType, UUID id) {
        AdministeredItemCacheableRepository repository = repositoryService.getAdministeredItemRepository(domainType)
        if (!repository) {
            throw new HttpStatusException(HttpStatus.NOT_FOUND, "Domain type [$domainType] not found")
        }

        AdministeredItem item = DataModel.simpleName.equalsIgnoreCase(domainType) ?
            repository.findById(id) as AdministeredItem :
            repository.loadWithContent(id) as AdministeredItem
        if (!item) {
            throw new HttpStatusException(HttpStatus.NOT_FOUND, 'AdministeredItem not found by ID')
        }

        updateDerivedProperties(item)
        item
    }

    private void appendTree(Map<String, Object> item, List<String> lines, String prefix, String connector, boolean root) {
        if (root) {
            lines << itemTitle(item)
        } else {
            lines << "${prefix}${connector}${itemTitle(item)}".toString()
        }

        List<Map<String, Object>> children = structuralChildren(item)
        String childPrefix = root ? '' : "${prefix}${connector == '└─ ' ? '   ' : '│  '}".toString()
        children.eachWithIndex {Map<String, Object> child, int index ->
            boolean last = index == children.size() - 1
            appendTree(child, lines, childPrefix, last ? '└─ ' : '├─ ', false)
        }
    }

    private List<Map<String, Object>> structuralChildren(Map<String, Object> item) {
        List<String> childKeys = structuralChildKeys(item)
        List<Map<String, Object>> children = []
        childKeys.each {String key ->
            children.addAll(mapList(item[key]))
        }
        children
    }

    private static List<String> structuralChildKeys(Map<String, Object> item) {
        switch (item.domainType) {
            case Folder.simpleName:
            case 'VersionedFolder':
                return ['childFolders', 'dataModels', 'terminologies', 'codeSets', 'classificationSchemes']
            case DataModel.simpleName:
                return ['dataClasses']
            case DataClass.simpleName:
                return ['dataClasses', 'dataElements']
            case ClassificationScheme.simpleName:
                return ['classifiers']
            case Classifier.simpleName:
                return ['childClassifiers']
            case Terminology.simpleName:
            case CodeSet.simpleName:
                return ['terms']
            case DataType.DataTypeKind.ENUMERATION_TYPE.stringValue:
                return ['enumerationValues']
            default:
                return []
        }
    }

    private List<String> collectVersionLines(Map<String, Object> item) {
        List<String> lines = []
        Set<String> seen = new LinkedHashSet<>()
        eachItem(item) {Map<String, Object> current ->
            Map<String, Object> versioning = current.versioning as Map<String, Object>
            List<Map<String, Object>> links = mapList(versioning?.versionLinks)
            if (links) {
                String currentId = current.id?.toString()
                String graphKey = ([currentId] + links.collect {Map<String, Object> link -> link.id?.toString() }).join(':')
                if (seen.add(graphKey)) {
                    if (lines) {
                        lines << ''
                    }
                    lines << versionNode(current)

                    int maxLinkTypeLength = links.collect {Map<String, Object> link ->
                        versionLinkLabel(currentId, link).length()
                    }.max() ?: 0

                    links.eachWithIndex {Map<String, Object> link, int index ->
                        Map<String, Object> linkedModel = linkedVersionModel(currentId, link)
                        if (linkedModel) {
                            boolean last = index == links.size() - 1
                            String linkType = versionLinkLabel(currentId, link)
                            lines << "${last ? '└─' : '├─'} ${linkType.padRight(maxLinkTypeLength)} ──> ${versionNode(linkedModel)}".toString()
                        }
                    }
                }
            }
        }
        lines
    }

    private static Map<String, Object> linkedVersionModel(String currentId, Map<String, Object> link) {
        Map<String, Object> source = link.sourceModel as Map<String, Object>
        Map<String, Object> target = link.targetModel as Map<String, Object>
        if (source?.id?.toString() == currentId) {
            return target
        }
        if (target?.id?.toString() == currentId) {
            return source
        }
        target ?: source
    }

    private static String versionLinkLabel(String currentId, Map<String, Object> link) {
        Map<String, Object> source = link.sourceModel as Map<String, Object>
        String linkType = link.linkType?.toString() ?: 'VERSION_LINK'
        if (source?.id?.toString() != currentId) {
            return linkType
        }

        switch (linkType) {
            case VersionLink.NEW_MODEL_VERSION_OF:
                return 'HAS_NEW_MODEL_VERSION'
            case VersionLink.NEW_FORK_OF:
                return 'HAS_NEW_FORK'
            default:
                return linkType
        }
    }

    private static String versionNode(Map<String, Object> model) {
        Map<String, Object> versioning = model.versioning as Map<String, Object>
        Object branchName = versioning?.branchName ?: model.branchName ?: Model.DEFAULT_BRANCH_NAME
        Object modelVersion = versioning?.modelVersion ?: model.modelVersion
        Object finalised = versioning?.finalised != null ? versioning.finalised : model.finalised
        String status = finalised ? 'finalised' : 'draft'
        "o  ${branchName ?: ''}${modelVersion ? " ${modelVersion}" : ''} [${status}]".toString()
    }

    private List<String> collectRelationshipLines(Map<String, Object> item) {
        Set<String> lines = new LinkedHashSet<>()
        eachItem(item) {Map<String, Object> current ->
            collectClassifierLines(current, lines)
            collectSemanticLinkLines(current, lines)
            collectDataTypeLine(current, lines)
            collectDataFlowLines(current, lines)
            collectComponentLines(current, lines)
        }
        lines as List<String>
    }

    private void collectClassifierLines(Map<String, Object> item, Set<String> lines) {
        if (item.domainType == ClassificationScheme.simpleName) {
            return
        }
        mapList(item.classifiers).each {Map<String, Object> classifier ->
            lines.add("${qualifiedItemTitle(item)} --classified-as--> Classifier: ${classifier.label}".toString())
        }
    }

    private void collectSemanticLinkLines(Map<String, Object> item, Set<String> lines) {
        mapList(item.links).each {Map<String, Object> link ->
            Map<String, Object> source = link.sourceMultiFacetAwareItem as Map<String, Object>
            Map<String, Object> target = link.targetMultiFacetAwareItem as Map<String, Object>
            if (source && target) {
                lines.add("${qualifiedReferenceTitle(source, item)} --${relationshipLabel(link.linkType)}--> ${qualifiedReferenceTitle(target, item)}".toString())
            }
        }
    }

    private void collectDataTypeLine(Map<String, Object> item, Set<String> lines) {
        Map<String, Object> dataType = item.dataType as Map<String, Object>
        if (dataType) {
            lines.add("${qualifiedItemTitle(item)} --uses-type--> ${itemTitle(dataType)}".toString())
        }
    }

    private void collectDataFlowLines(Map<String, Object> item, Set<String> lines) {
        if (item.domainType != DataFlow.simpleName) {
            return
        }

        Map<String, Object> source = item.source as Map<String, Object>
        Map<String, Object> target = item.target as Map<String, Object>
        if (source && target) {
            lines.add("${qualifiedItemTitle(source)} --data-flow:${item.label ?: ''}--> ${qualifiedItemTitle(target)}".toString())
        }
    }

    private void collectComponentLines(Map<String, Object> item, Set<String> lines) {
        if (item.domainType == DataClassComponent.simpleName) {
            addManyToManyLines(mapList(item.sourceDataClasses), mapList(item.targetDataClasses), "data-class-component:${item.label ?: ''}".toString(), lines)
        } else if (item.domainType == DataElementComponent.simpleName) {
            addManyToManyLines(mapList(item.sourceDataElements), mapList(item.targetDataElements), "data-element-component:${item.label ?: ''}".toString(), lines)
        }
    }

    private void addManyToManyLines(List<Map<String, Object>> sources, List<Map<String, Object>> targets, String relationship, Set<String> lines) {
        sources.each {Map<String, Object> source ->
            targets.each {Map<String, Object> target ->
                lines.add("${qualifiedItemTitle(source)} --${relationship}--> ${qualifiedItemTitle(target)}".toString())
            }
        }
    }

    private void eachItem(Map<String, Object> item, @DelegatesTo(value = Map, strategy = Closure.DELEGATE_FIRST) Closure closure) {
        closure.call(item)
        structuralChildren(item).each {Map<String, Object> child ->
            eachItem(child, closure)
        }
        mapList(item.dataClassComponents).each {Map<String, Object> child ->
            eachItem(child, closure)
        }
        mapList(item.dataElementComponents).each {Map<String, Object> child ->
            eachItem(child, closure)
        }
    }

    private static String itemTitle(Map<String, Object> item) {
        "${item.domainType}: ${itemLabel(item)}".toString()
    }

    private static String qualifiedItemTitle(Map<String, Object> item) {
        List<Map<String, Object>> pathNodes = mapList(item.path)
        List<String> parts = pathNodes.collect {Map<String, Object> pathNode ->
            pathNodeTitle(pathNode)
        }.findAll {String part -> part}

        if (!parts) {
            parts = [itemTitle(item)]
        }
        parts.join(' / ')
    }

    private static String qualifiedReferenceTitle(Map<String, Object> reference, Map<String, Object> context) {
        reference.id?.toString() == context.id?.toString() ? qualifiedItemTitle(context) : referenceTitle(reference)
    }

    private static String referenceTitle(Map<String, Object> item) {
        "${item.domainType}: ${item.label}".toString()
    }

    private static String itemLabel(Map<String, Object> item) {
        (item.label ?: item.code ?: item.key ?: item.value ?: item.id)?.toString()
    }

    private static String pathNodeTitle(Map<String, Object> pathNode) {
        String domainType = pathNodeDomainType(pathNode.prefix?.toString())
        String identifier = pathNode.identifier?.toString()
        domainType && identifier ? "${domainType}: ${identifier}".toString() : null
    }

    private static String pathNodeDomainType(String prefix) {
        switch (prefix) {
            case 'vf':
                return 'VersionedFolder'
            case 'fo':
                return 'Folder'
            case 'dm':
                return DataModel.simpleName
            case 'dc':
                return DataClass.simpleName
            case 'de':
                return DataElement.simpleName
            case 'dt':
                return DataType.simpleName
            case 'ev':
                return EnumerationValue.simpleName
            case 'te':
                return Terminology.simpleName
            case 'tm':
                return Term.simpleName
            case 'cs':
                return CodeSet.simpleName
            case 'cl':
                return Classifier.simpleName
            case 'cls':
                return ClassificationScheme.simpleName
            case 'df':
                return DataFlow.simpleName
            case 'dcc':
                return DataClassComponent.simpleName
            case 'dec':
                return DataElementComponent.simpleName
            default:
                return null
        }
    }

    private static String relationshipLabel(Object label) {
        label?.toString()?.replaceAll(/([a-z])([A-Z])/, '$1-$2')?.replaceAll(/\s+/, '-')?.toLowerCase() ?: 'links-to'
    }

    private static List<Map<String, Object>> mapList(Object value) {
        ((value ?: []) as List<Object>).findAll {Object item ->
            item instanceof Map
        }.collect {Object item ->
            item as Map<String, Object>
        }
    }

    private AdministeredItem loadReferencedItem(AdministeredItem item) {
        if (!item?.id) {
            return item
        }

        AdministeredItem loaded = loadItem(item.domainType, item.id)
        loaded
    }

    private Map<String, Object> toInspectMap(AdministeredItem item, boolean recursiveChildren, Set<UUID> activePath) {
        if (!item) {
            return null
        }

        updateDerivedProperties(item)

        Map<String, Object> out = baseMap(item)
        if (activePath.contains(item.id)) {
            out.cycle = true
            return out
        }

        Set<UUID> nextActivePath = ([] as Set<UUID>) + activePath
        nextActivePath.add(item.id)

        addDomainSpecificFields(item, out)
        addCommonMaps(item, out)
        addNonRecursiveInclusions(item, out)

        if (recursiveChildren) {
            addRecursiveChildren(item, out, nextActivePath)
        }

        out
    }

    private Map<String, Object> toReferencedMap(AdministeredItem item) {
        if (!item) {
            return null
        }
        AdministeredItem loaded = loadReferencedItem(item)
        canRead(loaded) ? toInspectMap(loaded, false, [] as Set<UUID>) : null
    }

    private List<Map<String, Object>> toReferencedList(Collection<? extends AdministeredItem> items) {
        ((items ?: []) as List<AdministeredItem>).collect {AdministeredItem child ->
            toReferencedMap(child)
        }.findAll {Map<String, Object> child -> child != null}
    }

    private List<Map<String, Object>> toRecursiveList(Collection<? extends AdministeredItem> items, Set<UUID> activePath) {
        ((items ?: []) as List<AdministeredItem>).findAll {AdministeredItem child ->
            canRead(child)
        }.collect {AdministeredItem child ->
            toInspectMap(loadReferencedItem(child), true, activePath)
        }.findAll {Map<String, Object> child -> child != null}
    }

    private List<Map<String, Object>> toNonRecursiveList(Collection<? extends AdministeredItem> items) {
        ((items ?: []) as List<AdministeredItem>).findAll {AdministeredItem child ->
            canRead(child)
        }.collect {AdministeredItem child ->
            toInspectMap(child, false, [] as Set<UUID>)
        }.findAll {Map<String, Object> child -> child != null}
    }

    private Map<String, Object> baseMap(AdministeredItem item) {
        Map<String, Object> out = new LinkedHashMap<>(16)
        out.label = item.label
        out.description = item.description
        out.id = item.id
        out.domainType = item.domainType
        out
    }

    private void addCommonMaps(AdministeredItem item, Map<String, Object> out) {
        out.metadata = metadataMap(item.metadata)
        out.classifiers = item.classifiers.collect {Classifier classifier -> classifierSummary(classifier)}
        out.path = pathList(item)
        out.annotations = item.annotations
        if (item instanceof DataElement) {
            out.links = semanticLinks(item)
        }
    }

    private static Map<String, Object> classifierSummary(Classifier classifier) {
        Map<String, Object> out = new LinkedHashMap<>(2)
        out.id = classifier.id
        out.label = classifier.label
        out
    }

    private static Map<String, String> metadataMap(Collection<Metadata> metadata) {
        ((metadata ?: []) as List<Metadata>).collectEntries {Metadata md ->
            ["${md.namespace}:${md.key}".toString(), md.value]
        } as Map<String, String>
    }

    private static List<Map<String, Object>> pathList(AdministeredItem item) {
        List<Map<String, Object>> pathNodes = item.path?.nodes?.collect {pathNode ->
            Map<String, Object> out = new LinkedHashMap<>(4)
            out.prefix = pathNode.prefix
            out.identifier = pathNode.identifier
            if (pathNode.modelIdentifier) {
                out.modelIdentifier = pathNode.modelIdentifier
            }
            if (pathNode.attribute) {
                out.attribute = pathNode.attribute
            }
            out
        } as List<Map<String, Object>>
        pathNodes ?: []
    }

    private void addDomainSpecificFields(AdministeredItem item, Map<String, Object> out) {
        if (item instanceof Model) {
            Model model = (Model) item
            Map<String, Object> versioning = new LinkedHashMap<>(5)
            versioning.finalised = model.finalised
            versioning.modelVersion = model.modelVersion
            versioning.modelVersionTag = model.modelVersionTag
            versioning.branchName = model.branchName
            versioning.documentationVersion = model.documentationVersion
            if (shouldReadVersionLinks(model)) {
                versioning.versionLinks = versionLinks(model)
            }
            out.versioning = versioning

            Map<String, Object> ownership = new LinkedHashMap<>(2)
            if (model.organisation) {
                ownership.organisation = model.organisation
            }
            if (model.author) {
                ownership.author = model.author
            }
            if (ownership) {
                out.ownership = ownership
            }
        }

        if (item instanceof DataClass) {
            DataClass dataClass = (DataClass) item
            out.minMultiplicity = dataClass.minMultiplicity
            out.maxMultiplicity = dataClass.maxMultiplicity
        } else if (item instanceof DataElement) {
            DataElement dataElement = (DataElement) item
            out.minMultiplicity = dataElement.minMultiplicity
            out.maxMultiplicity = dataElement.maxMultiplicity
        } else if (item instanceof DataModel) {
            out.type = ((DataModel) item).dataModelType
        } else if (item instanceof DataType) {
            DataType dataType = (DataType) item
            out.units = dataType.units
            out.modelResourceDomainType = dataType.modelResourceDomainType
            out.modelResourceId = dataType.modelResourceId
        } else if (item instanceof EnumerationValue) {
            EnumerationValue enumerationValue = (EnumerationValue) item
            out.category = enumerationValue.category
            out.key = enumerationValue.key
            out.value = enumerationValue.value
        } else if (item instanceof Term) {
            Term term = (Term) item
            out.code = term.code
            out.definition = term.definition
            out.url = term.url
            out.isParent = term.isParent
            out.depth = term.depth
        } else if (item instanceof DataFlow) {
            DataFlow dataFlow = (DataFlow) item
            out.definition = dataFlow.definition
            out.diagramLayout = dataFlow.diagramLayout
        } else if (item instanceof DataClassComponent) {
            out.definition = ((DataClassComponent) item).definition
        } else if (item instanceof DataElementComponent) {
            out.definition = ((DataElementComponent) item).definition
        }
    }

    private boolean shouldReadVersionLinks(Model model) {
        isVersioningOwnerCandidate(model) && !hasVersionedFolderAncestor(model) && ownsVersioning(model)
    }

    private boolean isVersioningOwnerCandidate(Model model) {
        model instanceof DataModel ||
            model instanceof Terminology ||
            model instanceof CodeSet ||
            model instanceof ClassificationScheme ||
            isVersionedFolder(model)
    }

    private boolean hasVersionedFolderAncestor(Model model) {
        !isVersionedFolder(model) && (model.path?.nodes?.any {pathNode ->
            pathNode.prefix == 'vf'
        } ?: false)
    }

    private boolean isVersionedFolder(Model model) {
        model instanceof Folder && model.domainType == 'VersionedFolder'
    }

    private boolean ownsVersioning(Model model) {
        Model modelWithVersion = model.getModelWithVersion()
        modelWithVersion?.id == model.id
    }

    private List<Map<String, Object>> versionLinks(Model model) {
        List<Map<String, Object>> out = []
        out.addAll(versionLinksFrom(model))
        out.addAll(versionLinksTo(model))
        out
    }

    private List<Map<String, Object>> versionLinksFrom(Model sourceModel) {
        ((sourceModel.versionLinks ?: []) as List<VersionLink>).collect {VersionLink versionLink ->
            Model targetModel = loadModel(versionLink.targetModelDomainType, versionLink.targetModelId)
            targetModel ? versionLinkMap(versionLink, sourceModel, targetModel) : null
        }.findAll {Map<String, Object> versionLink -> versionLink != null}
    }

    private List<Map<String, Object>> versionLinksTo(Model targetModel) {
        ((versionLinkRepository.findSourceModels(targetModel.id) ?: []) as List<VersionLink>).collect {VersionLink versionLink ->
            Model sourceModel = loadModel(versionLink.multiFacetAwareItemDomainType, versionLink.multiFacetAwareItemId)
            sourceModel ? versionLinkMap(versionLink, sourceModel, targetModel) : null
        }.findAll {Map<String, Object> versionLink -> versionLink != null}
    }

    private Map<String, Object> versionLinkMap(VersionLink versionLink, Model sourceModel, Model targetModel) {
        Map<String, Object> out = new LinkedHashMap<>(4)
        out.id = versionLink.id
        out.linkType = versionLink.versionLinkType
        out.sourceModel = modelVersionReference(sourceModel)
        out.targetModel = modelVersionReference(targetModel)
        out
    }

    private Map<String, Object> modelVersionReference(Model model) {
        updateDerivedProperties(model)

        Map<String, Object> out = new LinkedHashMap<>(8)
        out.id = model.id
        out.domainType = model.domainType
        out.label = model.label
        out.finalised = model.finalised
        out.modelVersion = model.modelVersion
        out.modelVersionTag = model.modelVersionTag
        out.branchName = model.branchName
        out.documentationVersion = model.documentationVersion
        out
    }

    private Model loadModel(String domainType, UUID id) {
        if (!domainType || !id) {
            return null
        }

        AdministeredItemCacheableRepository repository = repositoryService.getAdministeredItemRepository(domainType)
        AdministeredItem item = repository?.findById(id) as AdministeredItem
        item instanceof Model ? item as Model : null
    }

    private List<Map<String, Object>> semanticLinks(AdministeredItem item) {
        List<Map<String, Object>> out = []
        out.addAll(semanticLinksFrom(item))
        out.addAll(semanticLinksTo(item))
        out
    }

    private List<Map<String, Object>> semanticLinksFrom(AdministeredItem sourceItem) {
        ((sourceItem.semanticLinks ?: []) as List<SemanticLink>).collect {SemanticLink semanticLink ->
            AdministeredItem targetItem = loadAdministeredItem(semanticLink.targetMultiFacetAwareItemDomainType, semanticLink.targetMultiFacetAwareItemId)
            targetItem && canRead(targetItem) ? semanticLinkMap(semanticLink, sourceItem, targetItem) : null
        }.findAll {Map<String, Object> semanticLink -> semanticLink != null}
    }

    private List<Map<String, Object>> semanticLinksTo(AdministeredItem targetItem) {
        ((semanticLinkRepository.readAllByTargetMultiFacetAwareItemId(targetItem.id) ?: []) as List<SemanticLink>).collect {SemanticLink semanticLink ->
            AdministeredItem sourceItem = loadAdministeredItem(semanticLink.multiFacetAwareItemDomainType, semanticLink.multiFacetAwareItemId)
            sourceItem && canRead(sourceItem) ? semanticLinkMap(semanticLink, sourceItem, targetItem) : null
        }.findAll {Map<String, Object> semanticLink -> semanticLink != null}
    }

    private Map<String, Object> semanticLinkMap(SemanticLink semanticLink, AdministeredItem sourceItem, AdministeredItem targetItem) {
        Map<String, Object> out = new LinkedHashMap<>(6)
        out.id = semanticLink.id
        out.domainType = SemanticLink.simpleName
        out.linkType = semanticLink.linkType?.label
        out.unconfirmed = semanticLink.unconfirmed
        out.sourceMultiFacetAwareItem = administeredItemReference(sourceItem)
        out.targetMultiFacetAwareItem = administeredItemReference(targetItem)
        out
    }

    private Map<String, Object> administeredItemReference(AdministeredItem item) {
        Map<String, Object> out = new LinkedHashMap<>(3)
        out.id = item.id
        out.domainType = item.domainType
        out.label = item.label
        out
    }

    private AdministeredItem loadAdministeredItem(String domainType, UUID id) {
        if (!domainType || !id) {
            return null
        }

        AdministeredItemCacheableRepository repository = repositoryService.getAdministeredItemRepository(domainType)
        repository?.findById(id) as AdministeredItem
    }

    private void addRecursiveChildren(AdministeredItem item, Map<String, Object> out, Set<UUID> activePath) {
        if (item instanceof Folder) {
            Folder folder = (Folder) item
            out.childFolders = toRecursiveList(folder.childFolders, activePath)
            out.dataModels = toRecursiveList(folder.dataModels, activePath)
            out.terminologies = toRecursiveList(folder.terminologies, activePath)
            out.codeSets = toRecursiveList(folder.codeSets, activePath)
            out.classificationSchemes = toRecursiveList(folder.classificationSchemes, activePath)
        } else if (item instanceof DataModel) {
            out.dataClasses = toRecursiveList(dataClassRepository.readAllByDataModelAndParentDataClassIsNull((DataModel) item), activePath)
        } else if (item instanceof DataClass) {
            DataClass dataClass = (DataClass) item
            out.dataClasses = toRecursiveList(dataClass.dataClasses, activePath)
            out.dataElements = toRecursiveList(dataClass.dataElements, activePath)
        } else if (item instanceof ClassificationScheme) {
            out.classifiers = toRecursiveList(((ClassificationScheme) item).csClassifiers, activePath)
        } else if (item instanceof Classifier) {
            out.childClassifiers = toRecursiveList(((Classifier) item).childClassifiers, activePath)
        } else if (item instanceof Terminology) {
            out.terms = toRecursiveList(((Terminology) item).terms, activePath)
        }
    }

    private void addNonRecursiveInclusions(AdministeredItem item, Map<String, Object> out) {
        if (item instanceof CodeSet) {
            out.terms = toReferencedList(((CodeSet) item).terms)
        } else if (item instanceof DataElement) {
            out.dataType = toReferencedMap(((DataElement) item).dataType)
        } else if (item instanceof DataType) {
            out.enumerationValues = toNonRecursiveList(((DataType) item).enumerationValues)
        } else if (item instanceof DataFlow) {
            DataFlow dataFlow = (DataFlow) item
            out.source = toReferencedMap(dataFlow.source)
            out.target = toReferencedMap(dataFlow.target)
            out.dataClassComponents = toNonRecursiveList(dataFlow.dataClassComponents)
        } else if (item instanceof DataClassComponent) {
            DataClassComponent dataClassComponent = (DataClassComponent) item
            out.sourceDataClasses = toReferencedList(dataClassComponent.sourceDataClasses)
            out.targetDataClasses = toReferencedList(dataClassComponent.targetDataClasses)
            out.dataElementComponents = toNonRecursiveList(dataClassComponent.dataElementComponents)
        } else if (item instanceof DataElementComponent) {
            DataElementComponent dataElementComponent = (DataElementComponent) item
            out.sourceDataElements = toReferencedList(dataElementComponent.sourceDataElements)
            out.targetDataElements = toReferencedList(dataElementComponent.targetDataElements)
        }
    }

    private void updateDerivedProperties(AdministeredItem item) {
        pathRepository.readParentItems(item)
        item.updatePath()
        item.updateBreadcrumbs()
        AvailableActions.updateAvailableActions(item, accessControlService)
    }

    private boolean canRead(AdministeredItem item) {
        try {
            accessControlService.checkRole(Role.READER, item)
            return true
        } catch (AuthorizationException ignored) {
            return false
        }
    }
}

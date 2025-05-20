package uk.ac.ox.softeng.mauro.domain.diff

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import uk.ac.ox.softeng.mauro.domain.model.Model

import java.time.Instant

@Slf4j
@CompileStatic
class DiffBuilder {
    static final String LABEL = 'label'
    static final String DESCRIPTION = 'description'
    static final String ID_KEY = 'id'
    static final String DATE_CREATED_KEY = 'dateCreated'
    static final String LAST_UPDATED_KEY = 'lastUpdated'
    static final String CLASS_KEY = 'class'
    static final String FOLDER_KEY = 'folder'
    static final String METADATA = 'metadata'
    static final String ANNOTATION = 'annotations'
    static final String DOMAIN_TYPE = 'domainType'
    static final String RULE = 'rules'
    static final String CHILD_ANNOTATIONS = 'childAnnotations'
    static final String SUMMARY_METADATA = 'summaryMetadata'
    static final String SUMMARY_METADATA_TYPE = 'summaryMetadataType'
    static final String SUMMARY_METADATA_REPORT = 'summaryMetadataReports'
    static final String DATA_CLASSES = 'dataClasses'
    static final String DATA_TYPE = 'dataTypes'
    static final String DATA_ELEMENTS = 'dataElements'
    static final String ENUMERATION_VALUES = 'enumerationValues'
    static final String REFERENCE_FILES = 'referenceFiles'
    static final String ALIASES_STRING = 'aliasesString'
    static final String DATA_TYPE_PATH = 'dataTypePath'
    static final String MIN_MULTIPILICITY = 'minMultiplicity'
    static final String MAX_MULTIPILICITY = 'maxMultiplicity'
    static final String CATEGORY = 'category'
    static final String VALUE = 'value'
    static final String REPORT_DATE = 'reportDate'
    static final String CLASSIFIERS = 'classifiers'
    static final String LEFT_ID_KEY = 'leftId'
    static final String RIGHT_ID_KEY = 'rightId'
    static final String BRANCH_NAME = 'branchName'
    static final String PATH_MODEL_IDENTIFIER = 'pathModelIdentifier'
    static final String FILE_NAME = 'fileName'
    static final String LANGUAGE = 'language'
    static final String REPRESENTATION = 'representation'
    static final String REFERENCE_TYPE = 'referenceType'
    static final List<String> IGNORE_KEYS = [ID_KEY, DATE_CREATED_KEY, LAST_UPDATED_KEY, DOMAIN_TYPE, CLASS_KEY, FOLDER_KEY, LEFT_ID_KEY, RIGHT_ID_KEY]
    static final List<String> MODEL_COLLECTION_KEYS = [METADATA, ANNOTATION, RULE, SUMMARY_METADATA, SUMMARY_METADATA_REPORT, REFERENCE_FILES, DATA_CLASSES, DATA_TYPE, DATA_ELEMENTS,
   ENUMERATION_VALUES, REFERENCE_FILES, CLASSIFIERS, REFERENCE_TYPE]

    static ArrayDiff arrayDiff() {
        new ArrayDiff()
    }

    static <K extends DiffableItem> ObjectDiff<K> objectDiff(Class<K> objectClass) {
        new ObjectDiff<K>(objectClass)
    }

    static CollectionDTO createCollectionDiff(List<String> collectionKeys, Map<String, Object> modelProperties) {
        Map<String, Object> collectionMap = modelProperties.findAll {
            !isNull(it) && isCollection(it.value) && isRequiredCollection(it.key, collectionKeys)
        }
        CollectionDTO collectionDTO = new CollectionDTO()
        collectionMap.each { k, v -> collectionDTO.addField(k as String, v as Collection<DiffableItem>) }
        collectionDTO
    }

    static ObjectDiff buildBaseDiff(Model lhs, Model rhs) {
        Map lhsMap = lhs.properties
        lhsMap.removeAll { excluded(it as Map.Entry<String, Object>) }
        Map<String, String> leftStrFields = lhsMap.findAll { isAssignableFrom(it.value, String) } as Map<String, String>
        Map<String, Boolean> leftBooleanFields = lhsMap.findAll { isAssignableFrom(it.value, Boolean) } as Map<String, Boolean>
        Map<String, Instant> leftInstantFields = lhsMap.findAll { isAssignableFrom(it.value, Instant) } as Map<String, Instant>

        Map rhsMap = rhs.properties
        rhsMap.removeAll { excluded(it as Map.Entry<String, Object>) }
        Map<String, String> rightStrFields = rhsMap.findAll { isAssignableFrom(it.value, String) } as Map<String, String>
        Map<String, Boolean> rightBooleanFields = rhsMap.findAll { isAssignableFrom(it.value, Boolean) } as Map<String, Boolean>
        Map<String, Instant> rightInstantFields = rhsMap.findAll { isAssignableFrom(it.value, Instant) } as Map<String, Instant>

        String lhsId = lhs.id ?: "Left:Unsaved_${lhs.domainType}"
        String rhsId = rhs.id ?: "Right:Unsaved_${rhs.domainType}"
        Class<Model> diffClass = lhs.getClass()
        ObjectDiff baseDiff = newObjectDiff(diffClass, lhsId, rhsId)
        baseDiff.label = lhsMap.find { it.key == LABEL }.value

        buildStrings(baseDiff, leftStrFields as Map<String, Object>, rightStrFields as Map<String, Object>)
        buildField(Boolean, baseDiff, leftBooleanFields as Map<String, Object>, rightBooleanFields as Map<String, Object>)
        buildField(Instant, baseDiff, leftInstantFields as Map<String, Object>, rightInstantFields as Map<String, Object>)
        baseDiff
    }

    static ObjectDiff newObjectDiff(Class<Model> diffClass, String lhsId, String rhsId) {
        ObjectDiff objectDiff = new ObjectDiff(diffClass).leftId(lhsId).rightId(rhsId)
        objectDiff
    }

    static ObjectDiff diff(Model lhs, Model rhs, CollectionDTO lhsCollectionDTO, CollectionDTO rhsCollectionDTO) {
        ObjectDiff baseDiff = buildBaseDiff(lhs, rhs)
        buildCollection(baseDiff as ObjectDiff<DiffableItem>, lhsCollectionDTO, rhsCollectionDTO)
    }

    static ObjectDiff buildCollection(ObjectDiff<DiffableItem> objectDiff, CollectionDTO lhsCollectionDTO, CollectionDTO rhsCollectionDTO) {
        lhsCollectionDTO.fieldCollections.each {
            Collection<DiffableItem> rhsValue = getRhsCollection(it.key, rhsCollectionDTO)
            if (!it.value.isEmpty() || !rhsValue.isEmpty()) {
                String name = it.key
                Collection<DiffableItem> lhsValue = it.value
                objectDiff.appendCollection(name, lhsValue, rhsValue)
            }

        }
        Set<String> rhsOnlyKeys = rhsCollectionDTO.fieldCollections.keySet()
        //keys in RHS only not LHS
        rhsOnlyKeys.removeAll(lhsCollectionDTO.fieldCollections.keySet())
        rhsOnlyKeys.each {
            Collection<DiffableItem> rhsValue = getRhsCollection(it, rhsCollectionDTO)
            if (!rhsValue.isEmpty()) {
                objectDiff.appendCollection(it, null, rhsValue)
            }
        }
        objectDiff
    }

    static ObjectDiff buildStrings(ObjectDiff objectDiff, Map<String, Object> lhsMap, Map<String, Object> rhsMap) {
        buildField(String, objectDiff, lhsMap, rhsMap)
        objectDiff
    }

    static ObjectDiff buildField(Class<? extends Object> targetClass, ObjectDiff objectDiff, Map<String, Object> lhsMap, Map<String, Object> rhsMap) {
        if (!lhsMap.isEmpty()) {
            lhsMap.each { k, v ->
                if (rhsMap[k] != v) {
                    objectDiff.appendField(k as String, targetClass instanceof String ? clean(v as String) : v,
                            targetClass instanceof String ? clean(rhsMap[k] as String) : rhsMap[k])
                }
            }
        }
        Set<String> rhsKeys = rhsMap ? rhsMap.keySet() : new HashSet<String>()
        Set<String> lhsKeys = lhsMap ? lhsMap.keySet() : new HashSet<String>()
        if (rhsKeys.disjoint(lhsKeys)) {
            rhsMap.keySet().each {
                if (!lhsMap.keySet().contains(it)) {
                    objectDiff.appendField(it as String, null, targetClass instanceof String ? clean(rhsMap.get(it) as String) :
                            rhsMap.get(it))
                }
            }
        }
        objectDiff
    }


    static boolean isAssignableFrom(Object value, Class aClass) {
        null != value && value.getClass().isAssignableFrom(aClass)
    }


    static String clean(String field) {
        field?.trim() ?: null
    }

    static boolean isCollection(Object value) {
        if (value == null) {
            false
        } else {
            [Collection, Object[]].any {
                it.isAssignableFrom(value.getClass())
            }
        }
    }

    static boolean excluded(Map.Entry<String, Object> it) {
        IGNORE_KEYS.contains(it.key) || isNull(it)
    }

    static boolean isNull(Map.Entry<String, Object> it) {
        isNull(it.key) || isNull(it.value)
    }

    static boolean isRequiredCollection(String key, List<String> requiredKeys) {
        requiredKeys.contains(key)
    }

    static Collection<DiffableItem> getRhsCollection(String name, CollectionDTO rhsCollection) {
        rhsCollection.fieldCollections.get(name) ?: []
    }

    static boolean isNullOrEmpty(String str) {
        isNull(str) || str.allWhitespace
    }

    static boolean isNull(Object value) {
        null == value
    }

    static boolean isNullOrEmpty(Collection<Object> collection) {
        isNull(collection) || collection.isEmpty()
    }
}

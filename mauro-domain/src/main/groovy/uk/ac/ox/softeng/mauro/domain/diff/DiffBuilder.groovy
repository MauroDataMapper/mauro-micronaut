package uk.ac.ox.softeng.mauro.domain.diff

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import uk.ac.ox.softeng.mauro.domain.model.Model

@Slf4j
@CompileStatic
class DiffBuilder {
    static final String LABEL_KEY = 'label'
    static final String ID_KEY = 'id'
    static final String DATE_CREATED_KEY = 'dateCreated'
    static final String LAST_UPDATED_KEY = 'lastUpdated'
    static final String CLASS_KEY = 'class'
    static final String FOLDER_KEY = 'folder'
    static final String DATE_FINALISED_KEY = 'dateFinalised'
    static final String METADATA = 'metadata'
    static final String ANNOTATION = 'annotations'
    static final String DOMAIN_TYPE = 'domainType'
    static final String RULE = 'rules'
    static final List<String> IGNORE_KEYS = [ID_KEY, DATE_CREATED_KEY, LAST_UPDATED_KEY, DOMAIN_TYPE, CLASS_KEY, FOLDER_KEY]
    static final List<String> MODEL_COLLECTION_KEYS = [METADATA, ANNOTATION, RULE]

    static <K extends Diffable> ArrayDiff<K> arrayDiff() {
        new ArrayDiff<K>()
    }

    static CollectionDTO createCollectionDiff(List<String> collectionKeys, Map<String, Object> modelProperties) {
        Map<String, Object> collectionMap = modelProperties.findAll {
            !isNull(it) && isCollection(it.value) && isRequiredCollection(it.key, collectionKeys)
        }
        CollectionDTO collectionDTO = new CollectionDTO()
        collectionMap.each { k, v -> collectionDTO.addField(k as String, v as Collection<Object>) }
        collectionDTO
    }

    static ObjectDiff buildBaseDiff(Model lhs, Model rhs) {
        Map lhsMap = lhs.properties
        lhsMap.each { println("Map entry: ${it.key}, ${it.value}") }
        lhsMap.removeAll { excluded(it as Map.Entry<String, Object>) }
        Map leftStrFields = lhsMap.findAll { isAssignableFrom(it.value, String) }
        Map leftBooleanFields = lhsMap.findAll { isAssignableFrom(it.value, Boolean) }

        Map rhsMap = rhs.properties
        rhsMap.removeAll { excluded(it as Map.Entry<String, Object>) }
        Map<String, String> rightStrFields = rhsMap.findAll { isAssignableFrom(it.value, String) } as Map<String, String>
        Map<String, Boolean> rightBooleanFields = rhsMap.findAll { isAssignableFrom(it.value, Boolean) } as Map<String, Boolean>

        leftStrFields.each { println("$it.key, $it.value") }
        rightStrFields.each { println("$it.key, $it.value ") }

        String lhsId = lhs.id ?: "Left:Unsaved_${lhs.domainType}"
        String rhsId = rhs.id ?: "Right:Unsaved_${rhs.domainType}"
        Class<Model> diffClass = lhs.getClass()
        ObjectDiff baseDiff = new ObjectDiff(diffClass, lhsId, rhsId)
        baseDiff.label = lhsMap.find { it.key == LABEL_KEY }.value
        Map<String, Boolean> leftDateFinalised = lhsMap.find { it.key == DATE_FINALISED_KEY } as Map<String, Boolean>
        Map<String, Boolean> rightDateFinalised = rhsMap.find { it.key == DATE_FINALISED_KEY } as Map<String, Boolean>

        buildStrings(baseDiff, leftStrFields as Map<String, Object>, rightStrFields as Map<String, Object>)
        buildField(baseDiff, leftBooleanFields as Map<String, Object>, rightBooleanFields as Map<String, Object>)
        if ((leftDateFinalised || rightDateFinalised) && leftDateFinalised != rightDateFinalised) {
            buildField(baseDiff, dateFinalised(lhsMap) as Map<String, Object>, dateFinalised(rhsMap) as Map<String, Object>)
        }
        baseDiff
    }

    private static Map dateFinalised(Map map) {
        map.find { it.key == DATE_FINALISED_KEY } as Map
    }

    static ObjectDiff diff(Model lhs, Model rhs, CollectionDTO lhsCollectionDTO, CollectionDTO rhsCollectionDTO) {

        ObjectDiff baseDiff = buildBaseDiff(lhs, rhs)
        buildCollection(baseDiff as ObjectDiff<Diffable>, lhsCollectionDTO, rhsCollectionDTO)
    }

    static <T extends Diffable> ObjectDiff buildStrings(ObjectDiff<T> objectDiff, Map<String, Object> lhsMap, Map<String, Object> rhsMap) {
        lhsMap.each { k, v ->
            if (rhsMap[k] != v) {
                FieldDiff fieldDiff = new FieldDiff(k as String, clean(v as String), clean(rhsMap[k] as String))
                objectDiff.append(fieldDiff)
            }
        }
        objectDiff
    }

    static ObjectDiff buildField(ObjectDiff objectDiff, Map<String, Object> lhsMap, Map<String, Object> rhsMap) {
        lhsMap.each { k, v ->
            if (rhsMap[k] != v) {
                FieldDiff fieldDiff = new FieldDiff(k as String, v, rhsMap[k])
                objectDiff.append(fieldDiff)
            }
        }
        objectDiff
    }


    static ObjectDiff buildCollection(ObjectDiff<Diffable> objectDiff, CollectionDTO lhsCollectionDTO, CollectionDTO rhsCollectionDTO) {
        lhsCollectionDTO.fieldCollections.each {

            Collection<Object> rhsValue = getRhsCollection(it.key, rhsCollectionDTO)

            if (!it.value.isEmpty() || !rhsValue.isEmpty()) {
                println(" collection key: $it.key, value: $it.value, rhs: $rhsValue")
                String name = it.key
                Collection lhsValue = it.value
            //    objectDiff.appendCollection(it.key, it.value, rhsValue)
                objectDiff.appendCollection(name, lhsValue, rhsValue)
            }

        }
        objectDiff
    }


    static <F> Object setLeft(F lhs) {
        isAssignableFrom(lhs, String) ? clean(lhs as String) : lhs
        this
    }


    static <F> Object setRight(F rhs) {
        isAssignableFrom(rhs, String) ? clean(rhs as String) : rhs
        this
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
        null == it.key || null == it.value
    }

    static boolean isRequiredCollection(String key, List<String> requiredKeys) {
        requiredKeys.contains(key)
    }

    static Collection<Object> getRhsCollection(String name, CollectionDTO rhsCollection) {
        rhsCollection.fieldCollections.get(name) ?: []
    }
}

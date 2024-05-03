package uk.ac.ox.softeng.mauro.domain.diff

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import uk.ac.ox.softeng.mauro.domain.facet.Metadata
import uk.ac.ox.softeng.mauro.domain.facet.SummaryMetadata
import uk.ac.ox.softeng.mauro.domain.model.Item
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
    static final List<String> IGNORE_KEYS = [ID_KEY, DATE_CREATED_KEY, LAST_UPDATED_KEY, 'domainType', CLASS_KEY, FOLDER_KEY]

    static <K extends Diffable> ArrayDiff<K> arrayDiff(Class<Collection<K>> arrayClass) {
        new ArrayDiff<K>(arrayClass)
    }

    static CollectionDTO createCollectionDiff(Map<String, Object> modelProperties) {
        Map<String, Object> collectionMap = modelProperties.findAll {!isNullKey(it.key) && isCollection(it.value) }
        CollectionDTO collectionDTO = new CollectionDTO()
        collectionMap.each { k, v -> collectionDTO.addField(k as String, v as Collection<Object>) }
        collectionDTO
    }

    static ObjectDiff buildBaseDiff(Model lhs, Model rhs) {
        Map lhsMap = lhs.properties
        lhsMap.each { println("Map entry: ${it.key}, ${it.value}") }
        lhsMap.removeAll { excluded(it) }
        Map leftStrFields = lhsMap.findAll { isAssignableFrom(it.value, String) }
        Map leftBooleanFields = lhsMap.findAll { isAssignableFrom(it.value, Boolean) }

        Map rhsMap = rhs.properties
        rhsMap.removeAll { excluded(it) }
        Map<String, String> rightStrFields = rhsMap.findAll { isAssignableFrom(it.value, String) } as Map<String, String>
        Map<String, Boolean> rightBooleanFields = rhsMap.findAll { isAssignableFrom(it.value, Boolean) } as Map<String, Boolean>

        leftStrFields.each { println("$it.key, $it.value") }
        rightStrFields.each { println("$it.key, $it.value ") }

        String lhsId = lhs.id ?: "Left:Unsaved_${lhs.domainType}"
        String rhsId = rhs.id ?: "Right:Unsaved_${rhs.domainType}"
        Class<Model> diffClass = lhs.getClass()
        ObjectDiff baseDiff = new ObjectDiff(diffClass, lhsId, rhsId)
        baseDiff.label = lhsMap.find { it.key == LABEL_KEY }.value

        buildField(baseDiff, leftStrFields as Map<String, Object>, rightStrFields as Map<String, Object>)
        buildField(baseDiff, leftBooleanFields as Map<String, Object>, rightBooleanFields as Map<String, Object>)
    }

    static <T extends Model> ObjectDiff diff(Model lhs, Model rhs, CollectionDTO lhsCollectionDTO, CollectionDTO rhsCollectionDTO) {

        ObjectDiff baseDiff = buildBaseDiff(lhs, rhs)

        buildCollection(baseDiff, lhsCollectionDTO, rhsCollectionDTO)
    }

    static <K extends Diffable> CreationDiff<K> creationDiff(Class<K> objectClass) {
        new CreationDiff<K>(objectClass)
    }

    static <K extends Diffable> DeletionDiff<K> deletionDiff(Class<K> objectClass) {
        new DeletionDiff<K>(objectClass)
    }


    static boolean excluded(Map.Entry<Object, Object> it) {
        IGNORE_KEYS.contains(it.key) || isNull(it)
    }

    static boolean isNull(Map.Entry<Object, Object> it) {
        null == it.key || null == it.value
    }
    static boolean isNullKey(String key) {
        null == key
    }

    static boolean isAssignableFrom(Object value, Class aClass) {
        null != value && value.getClass().isAssignableFrom(aClass)
    }

    static <T extends Diffable> ObjectDiff buildField(ObjectDiff<T> objectDiff, Map<String, Object> lhsMap, Map<String, Object> rhsMap) {
        lhsMap.each { k, v ->
            if (rhsMap[k] != v) {
                FieldDiff fieldDiff = new FieldDiff(v.class, k as String, setLeft(v) as Diffable, setRight(rhsMap[k]) as Diffable)
                objectDiff.append(fieldDiff)
            }
        }
        objectDiff
    }

    static ObjectDiff buildCollection(ObjectDiff<Diffable> objectDiff, CollectionDTO lhsCollectionDTO, CollectionDTO rhsCollectionDTO) {
     //   params = params.collect {it instanceof Item ? it.id : it}
        lhsCollectionDTO.fieldCollections.each{
            println(" Each collection: it: $it.key, $it.value")
            Collection<Object> rhs = rhsCollectionDTO.fieldCollections.find {
                rhsIt ->
                    println("lhsKEY: $it.key , rhsIt.key: $rhsIt.key")
                    rhsIt.key == it.key
            }.value ?: []
            if (!it.value.isEmpty() || !rhs.isEmpty()) {
               // it.key.handle
              //  Class<Diffable> aClass = Class.forName(it.key) as Class<Diffable>
              //  println("Class name: $aClass")
                objectDiff.appendCollection(Metadata as Class<? extends Diffable>, it.key, it.value, rhs)
            }
            println("hi")
        }
        objectDiff
    }


    static <F> Object setLeft(F lhs) {
        isAssignableFrom(lhs, String) ? clean(lhs as String) : lhs
    }

    static <F> Object setRight(F rhs) {
        isAssignableFrom(rhs, String) ? clean(rhs as String) : rhs
    }

    // def void appendCollection(String s, List<SummaryMetadata> summaryMetadata1, List<SummaryMetadata> summaryMetadata2) {}


    private static String clean(String field) {
        field?.trim() ?: null
    }

    private static boolean isCollection(Object value) {
        if (value == null) {
            false
        } else {
            [Collection, Object[]].any {
                it.isAssignableFrom(value.getClass())
            }
        }
    }

}

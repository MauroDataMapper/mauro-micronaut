package uk.ac.ox.softeng.mauro.domain.diff

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import groovy.transform.CompileStatic
import io.micronaut.core.annotation.Introspected
import io.micronaut.core.annotation.Nullable
import uk.ac.ox.softeng.mauro.exception.MauroInternalException

@CompileStatic
@Introspected
class ObjectDiff<T extends DiffableItem> {
    @JsonIgnore
    Class<T> targetClass

    String leftId
    String rightId
    T left
    T right

    @Nullable
    String label

    @JsonProperty("diffs")
    List<FieldDiff> diffs = []

    @JsonIgnore
    boolean versionedDiff

    String namespace
    String key

    ObjectDiff(Class<T> targetClass) {
        this.targetClass = targetClass
    }

    ObjectDiff(Class<T> targetClass, String leftId, String rightId) {
        this.targetClass = targetClass
        this.leftId = leftId
        this.rightId = rightId
    }

    ObjectDiff<T> leftHandSide(String leftId, T lhs) {
        this.leftId = leftId
        this.left = lhs
        this
    }

    ObjectDiff<T> rightHandSide(String rightId, T rhs) {
        this.rightId = rightId
        this.right = rhs
        this
    }

    ObjectDiff<T> appendString(final String fieldName, final String lhs, final String rhs) throws MauroInternalException {
        FieldDiff fieldDiff = new FieldDiff(fieldName, DiffBuilder.clean(lhs), DiffBuilder.clean(rhs))
        append(fieldDiff)
    }

    def <K extends DiffableItem> ObjectDiff appendCollection(String name, Collection<DiffableItem> lhs, Collection<DiffableItem> rhs) {
        ArrayDiff diff = DiffBuilder.arrayDiff() as ArrayDiff
        diff.name = name
        if (!lhs) {
            return append(diff.createdObjects(rhs as Collection<K>))
        }

        // If no rhs then all lhs have been deleted/removed
        if (!rhs) {
            return append(diff.deletedObjects(lhs as Collection<K>) )
        }

        Collection<K> deleted = []
        Collection<ObjectDiff> modified = []
        // Assume all rhs have been created new
        List<K> created = new ArrayList<>(rhs as Collection<? extends K>)

        Map<String, K> lhsMap = lhs.collectEntries { [it.getDiffIdentifier(), it] }
        Map<String, K> rhsMap = rhs.collectEntries { [it.getDiffIdentifier(), it] }

        //toDo versionedDiff, childPaths when versionedDiff

        lhsMap.each { di, lObj ->
            K rObj = rhsMap[di]
            if (rObj) {
                // If robj then it exists and has not been created
                created.remove(rObj)
                // If we want to perform a diff on the actual elements themselves

                ObjectDiff od = lObj.diff(rObj)
                // If not equal then objects have been modified
                if (!od.objectsAreIdentical()) {
                    od.setLeft(null)
                    od.setRight(null)
                    modified.add(od)
                }

            } else {
                // If no robj then object has been deleted from lhs
                deleted.add(lObj)
            }
        }

        if (created || deleted || modified) {
            // return append(diff.deletedObjects(lhs as Collection<K>) as FieldDiff)
            append(diff.createdObjects(created as Collection)
                    .deletedObjects(deleted as Collection)
                    .modifiedObjects(modified as Collection) as ArrayDiff)
        }

        this
    }

    def <K> ObjectDiff<T> append(FieldDiff<K> fieldDiff) {
        diffs.add(fieldDiff)
        this

    }

    ObjectDiff<T> asVersionedDiff() {
        versionedDiff = true
        this
    }

    @JsonProperty('count')
    Integer getNumberOfDiffs() {
        diffs?.sum { it.getNumberOfDiffs() } as Integer ?: 0
    }

    private boolean objectsAreIdentical() {
        !getNumberOfDiffs()
    }

}



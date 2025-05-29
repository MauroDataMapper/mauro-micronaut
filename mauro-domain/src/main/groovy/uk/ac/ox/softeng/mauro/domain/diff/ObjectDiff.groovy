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
    @Nullable
    String namespace
    @Nullable
    String key

    @JsonProperty("diffs")
    List<? extends FieldDiff> diffs = []


    ObjectDiff(Class<T> targetClass) {
        this.targetClass = targetClass
    }

    ObjectDiff(Class<T> targetClass, String leftId, String rightId) {
        this.targetClass = targetClass
        this.leftId = leftId
        this.rightId = rightId
    }

    ObjectDiff<T> leftId(String leftId) {
        this.leftId = leftId
        this
    }

    ObjectDiff<T> rightId(String rightId) {
        this.rightId = rightId
        this
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

    ObjectDiff<T> appendString(final String fieldName, final String lhs, final String rhs, final DiffableItem lhsDiffableItem, final DiffableItem rhsDiffableItem) throws MauroInternalException {
        String lhsString = DiffBuilder.isNullOrEmpty(lhs) ? null : DiffBuilder.clean(lhs)
        String rhsString = DiffBuilder.isNullOrEmpty(rhs) ? null : DiffBuilder.clean(rhs)
        appendField( fieldName, lhsString, rhsString, lhsDiffableItem, rhsDiffableItem)
        this
    }

    <K> ObjectDiff<T> appendField(final String fieldName, K lhs, K rhs, final DiffableItem lhsDiffableItem, final DiffableItem rhsDiffableItem) throws MauroInternalException {
        if (lhs != rhs) {
            append(new FieldDiff(fieldName, lhs, rhs, lhsDiffableItem, rhsDiffableItem))
        }
        this
    }

    def <K extends DiffableItem> ObjectDiff appendCollection(String name, Collection<DiffableItem> lhs, Collection<DiffableItem> rhs) {
        ArrayDiff diff = DiffBuilder.arrayDiff() as ArrayDiff
        diff.name = name
        if (!lhs) {
            return append(diff.createdObjects(rhs.unique() as Collection<K>))
        }

        // If no rhs then all lhs have been deleted/removed
        if (!rhs) {
            return append(diff.deletedObjects(lhs.unique() as Collection<K>))
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
            append(diff.createdObjects(created as Collection)
                    .deletedObjects(deleted as Collection)
                    .modifiedObjects(modified as Collection) as ArrayDiff)
        }

        this
    }

    <K> ObjectDiff<T> append(FieldDiff<K> fieldDiff) {
        if (!diffs.contains(fieldDiff)) {
            diffs.add(fieldDiff)
        }
        this
    }

    @JsonProperty('count')
    Integer getNumberOfDiffs() {
        diffs?.sum { it.getNumberOfDiffs() } as Integer ?: 0
    }

    private boolean objectsAreIdentical() {
        !getNumberOfDiffs()
    }

    String toString()
    {
        return "ObjectDiff: class: "+targetClass.getCanonicalName()+" label"+label+" leftId: "+leftId+" rightId: "+rightId+" diffs: "+diffs
    }

}



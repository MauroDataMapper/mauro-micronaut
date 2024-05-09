package uk.ac.ox.softeng.mauro.domain.diff

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import groovy.transform.CompileStatic
import io.micronaut.core.annotation.Introspected
import io.micronaut.core.annotation.NonNull
import uk.ac.ox.softeng.mauro.domain.facet.Metadata
import uk.ac.ox.softeng.mauro.domain.facet.SummaryMetadata
import uk.ac.ox.softeng.mauro.domain.model.Model
import uk.ac.ox.softeng.mauro.domain.model.Path
import uk.ac.ox.softeng.mauro.exception.MauroInternalException

@CompileStatic
@Introspected
class ObjectDiff<T extends DiffableItem> {
    @JsonIgnore
    Class<T> targetClass

    String leftId
    String rightId
    String label

    @JsonProperty("diffs")
    List<FieldDiff> diffs = []

    boolean versionedDiff

    ObjectDiff(Class<T> targetClass, String leftId, String rightId) {
        this.targetClass = targetClass
        this.leftId = leftId
        this.rightId = rightId
    }

    def <K extends DiffableItem> ObjectDiff appendCollection(String name, Collection<DiffableItem> lhs, Collection<DiffableItem> rhs) {
        ArrayDiff diff = DiffBuilder.arrayDiff() as ArrayDiff
        diff.name = name
        if (!lhs) {
//            List<CollectionDiff> collectionDiffs = []
//            rhs.each {
//                DiffableItem rhsDiffableItem = (DiffableItem) (it)
//                CollectionDiff collectionDiff = rhsDiffableItem.fromItem()
//                collectionDiffs.add(collectionDiff)
//            }
            return append(diff.createdObjects(rhs))
         //   return append(diff.createdObjects(collectionDiffs) as FieldDiff<DiffableItem>)
        }

        // If no rhs then all lhs have been deleted/removed
        if (!rhs) {
//            List<CollectionDiff> collectionDiffs = []
//            lhs.each {
//                DiffableItem lhsDiffableItem = (DiffableItem) (it)
//                CollectionDiff collectionDiff = lhsDiffableItem.fromItem()
//                collectionDiffs.add(collectionDiff)
//            }
          //  return append(diff.deletedObjects(collectionDiffs) as FieldDiff<DiffableItem>)
            return append(diff.deletedObjects(lhs as Collection<K>) as FieldDiff)
        }

        Collection<K> deleted = []
        Collection<ObjectDiff> modified = []

        // Assume all rhs have been created new
        List<Object> created = new ArrayList<>(rhs)

        Map<String, K> lhsMap = lhs.collectEntries { [it.getDiffIdentifier(), it] }
        Map<String, K> rhsMap = rhs.collectEntries { [it.getDiffIdentifier(), it] }
        // This object diff is being performed on an object which has the concept of modelIdentifier, e.g branch name or version
        // If this is the case we want to make sure we ignore any versioning on sub contents as child versioning is controlled by the parent
        // This should only happen to models inside versioned folders, but we want to try and be more dynamic
//        if (isVersionedDiff()) {
//            Path childPath = Path.from((MdmDomain) lhs.first())
//            if (childPath.size() == 1 && childPath.first().modelIdentifier) {
//                // child collection has versioning
//                // recollect entries using the clean identifier rather than the full thing
//                lhsMap = lhs.collectEntries { [Path.from(it.pathPrefix, it.getDiffIdentifier(context)).first().identifier, it] }
//                rhsMap = rhs.collectEntries { [Path.from(it.pathPrefix, it.getDiffIdentifier(context)).first().identifier, it] }
//            }
//        }

        // Work through each lhs object and compare to rhs object
//        lhsMap.each { di, lObj ->
//            K rObj = rhsMap[di]
//            if (rObj) {
//                // If robj then it exists and has not been created
//                created.remove(rObj)
//                // If we want to perform a diff on the actual elements themselves
//
//                ObjectDiff od = lObj.diff(rObj)
//                // If not equal then objects have been modified
//                if (!od.objectsAreIdentical()) {
//                    modified.add(od)
//                }
//
//            } else {
//                // If no robj then object has been deleted from lhs
//                deleted.add(lObj)
//            }
//        }

        // if (created || deleted || modified || addIfEmpty) {
        if (created || deleted) {
            append(diff.createdObjects(created as FieldDiff as Collection)
                    .deletedObjects(deleted as FieldDiff as Collection))
            //    .withModifiedDiffs(modified))
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

//    @Override
//    Integer getNumberOfDiffs() {
//        diffs?.sum {it.getNumberOfDiffs()} as Integer ?: 0
//    }

}



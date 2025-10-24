package org.maurodata.domain.model


import com.fasterxml.jackson.annotation.JsonIgnore
import groovy.transform.CompileStatic
import jakarta.persistence.Transient

/**
 Any item in the model that holds references to other items
 should implement ItemReferencer
 */
@CompileStatic
trait ItemReferencer {
    /**
     * The list of ItemReference this holds references to
     */
    @Transient
    @JsonIgnore
    abstract List<ItemReference> retrieveItemReferences()

    /**
     * A map of Item to replace with new Item
     * returns a list of any references that were not replaced
     */
    @Transient
    @JsonIgnore
    abstract void replaceItemReferencesByIdentity(final IdentityHashMap<Item, Item> replacements, List<Item> notReplaced = [])

    /**
     * A very shallow copy of this ItemReferencer
     */
    @Transient
    @JsonIgnore
    abstract Item shallowCopy()

    /**
     * A simple map of all Items, each item pointing to a set of Items that refer it
     * E.g. for each item, what at the inbound references?
     */
    @Transient
    @JsonIgnore
    void predecessors(IdentityHashMap<Item, Set<Item>> predecessorMap = new IdentityHashMap<>(), IdentityHashMap<Item, Boolean> seen = new IdentityHashMap<>()) {

        Item me = (Item) this

        // Been here already
        if (seen.get(me) != null) {return}

        seen.put(me, true)

        List<ItemReference> itemReferences = me.retrieveItemReferences()
        if (itemReferences != null) {
            itemReferences.forEach {ItemReference beingReferenced ->
                Item successor = beingReferenced.theItem
                if (successor != null) {
                    // No Luke, I am your predecessor
                    Set successorReferencedBy = predecessorMap.get(successor)
                    if (successorReferencedBy == null) {
                        successorReferencedBy = Collections.newSetFromMap(new IdentityHashMap<>()) as Set<Item>
                        predecessorMap.put(successor, successorReferencedBy)
                    }
                    successorReferencedBy.add(me)
                }
            }
        }

        // Recurse successors

        if (itemReferences != null) {
            itemReferences.forEach {ItemReference beingReferenced ->
                Item successor = beingReferenced.theItem
                if (successor != null) {
                    ItemReferencer successorItemReferencer = successor
                    successorItemReferencer.predecessors(predecessorMap, seen)
                }
            }
        }

        // Do I have any predecessors? If not, create an empty set entry
        if (predecessorMap.get(me) == null) {
            predecessorMap.put(me, Collections.newSetFromMap(new IdentityHashMap<>()) as Set<Item>)
        }
    }

    /*
        Given an ItemReferencer (e.g. a DataModel / VersionedFolder / Folder etc)
        get a map of all id -> Item[]

        Useful for finding any Item by id without having to traverse
        but also for finding alternatively loaded items by id.
        Example use: DataType.referenceClass may point to a DataClass that only has the id hydrated
        use that id to find the DataClass in the model that has a label
         */
    @Transient
    @JsonIgnore
    Map<UUID, Set<Item>> itemLookupById() {
        IdentityHashMap<Item, Set<Item>> predecessorMap = new IdentityHashMap<>()
        this.predecessors(predecessorMap)

        Map<UUID, Set<Item>> allItemLookup = new HashMap<>(predecessorMap.size())

        predecessorMap.keySet().forEach {Item item ->

            final UUID id = item.id
            Set<Item> referencedItems = allItemLookup.get(id)
            if (referencedItems == null) {
                referencedItems = []
                allItemLookup.put(id, referencedItems)
            }
            referencedItems << item
        }

        return allItemLookup
    }

    /**
     A general deep clone does the following:

     Call the predecessors to walk the graph and get all references to Item
     Create an IdentityHashMap between Item and a shallow clone of the Item
     Replace all references to the originals with the shallow clones in the shallow clones to produce deep clones
     return the clone of this

     Look at stuff like modelResourceId
     */
    @Transient
    @JsonIgnore
    Item deepClone(IdentityHashMap<Item, Item> replacements = new IdentityHashMap<>(), List<Item> notReplaced = []) {
        IdentityHashMap<Item, Set<Item>> predecessorMap = new IdentityHashMap<>()
        IdentityHashMap<Item, Boolean> seen = new IdentityHashMap<>()
        this.predecessors(predecessorMap, seen)

        predecessorMap.keySet().forEach {Item toClone ->
            if (replacements.get(toClone) == null) {
                Item shallowCloned = (Item) toClone.shallowCopy()
                replacements.put(toClone, shallowCloned)
            }
        }

        replacements.values().forEach {Item shallowCloned ->
            ItemReferencer shallowClonedAsItemReferencer = (ItemReferencer) shallowCloned
            shallowClonedAsItemReferencer.replaceItemReferencesByIdentity(replacements, notReplaced)
        }

        return replacements.get(this)
    }
}

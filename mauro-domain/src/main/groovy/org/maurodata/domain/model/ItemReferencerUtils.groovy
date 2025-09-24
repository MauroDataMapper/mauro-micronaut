package org.maurodata.domain.model

import groovy.transform.CompileStatic

@CompileStatic
class ItemReferencerUtils {

    static <T extends Item> void addItems(List<T> items, List<ItemReference> beingReferenced) {
        if (items != null) {
            items.forEach {T t ->
                beingReferenced << ItemReference.from(t)
            }
        }
    }

    static <T extends Item> void addItems(Set<T> items, List<ItemReference> beingReferenced) {
        if (items != null) {
            items.forEach {T t ->
                beingReferenced << ItemReference.from(t)
            }
        }
    }

    static <T extends Item> void addItems(Collection<T> items, List<ItemReference> beingReferenced) {
        if (items != null) {
            items.forEach {T t ->
                beingReferenced << ItemReference.from(t)
            }
        }
    }

    static <T extends Item> void addItem(T item, List<ItemReference> beingReferenced) {
        if (item != null) {
            beingReferenced << ItemReference.from(item)
        }
    }

    static void addIdType(UUID id, String domainType, List<ItemReference> beingReferenced) {
        if (id != null) {
            beingReferenced << ItemReference.from(id, domainType)
        }
    }

    static <T extends Item> List<T> replaceItemsByIdentity(List<T> items, IdentityHashMap<Item, Item> replacements, List<Item> notReplaced = []) {
        if (items == null) {
            return null
        }
        final List<T> replacement = []
        items.forEach {T t ->
            Item replacementItem = replacements.get(t)
            if (replacementItem != null) {
                replacement.add((T) replacementItem)
            } else {
                replacement.add(t)
                notReplaced.add(t)
            }
        }

        return replacement
    }

    static <T extends Item> Set<T> replaceItemsByIdentity(Set<T> items, IdentityHashMap<Item, Item> replacements, List<Item> notReplaced = []) {
        if (items == null) {
            return null
        }
        final Set<T> replacement = []
        items.forEach {T t ->
            Item replacementItem = replacements.get(t)
            if (replacementItem != null) {
                replacement.add((T) replacementItem)
            } else {
                replacement.add(t)
                notReplaced.add(t)
            }
        }

        return replacement
    }

    static <T extends Item> Collection<T> replaceItemsByIdentity(Collection<T> items, IdentityHashMap<Item, Item> replacements, List<Item> notReplaced = []) {
        if (items == null) {
            return null
        }
        final Collection<T> replacement = []
        items.forEach {T t ->
            Item replacementItem = replacements.get(t)
            if (replacementItem != null) {
                replacement.add((T) replacementItem)
            } else {
                replacement.add(t)
                notReplaced.add(t)
            }
        }

        return replacement
    }

    static <T extends Item> T replaceItemByIdentity(T item, IdentityHashMap<Item, Item> replacements, List<Item> notReplaced = []) {
        if (item == null) {
            return null
        }

        Item replacementItem = replacements.get(item)

        if (replacementItem != null) {
            return (T) replacementItem
        }

        notReplaced.add(item)

        return item
    }

    /*
    Helper / debug
     */

    static void printItemReference(ItemReference itemReference) {
        if (itemReference != null) {
            Item item = itemReference.theItem
            if (item != null) {
                System.out.println(item.id.toString() + " -> " + item.dump())
            }
        }
    }

    static void printPredecessors(IdentityHashMap<Item, Set<Item>> predecessors) {

        predecessors.keySet().forEach {Item item ->

            Set myPredecessors = predecessors.get(item)

            System.out.println(item.dump())
            System.out.println('\t->')
            System.out.println('\t{')

            myPredecessors.forEach {Item predecessor ->
                System.out.println('\t\t' + predecessor.dump())
            }

            System.out.println('\t}')
        }
    }
}
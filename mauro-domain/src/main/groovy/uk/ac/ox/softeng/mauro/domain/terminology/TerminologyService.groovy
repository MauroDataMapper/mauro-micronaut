package uk.ac.ox.softeng.mauro.domain.terminology

import groovy.transform.CompileStatic
import uk.ac.ox.softeng.mauro.domain.model.ModelService
import uk.ac.ox.softeng.mauro.domain.tree.TreeItem

import jakarta.inject.Singleton

/**
 * The TerminologyService class provides utility functions for manipulating Terminology objects
 */
@CompileStatic
@Singleton
class TerminologyService extends ModelService<Terminology> {

    Boolean handles(Class clazz) {
        clazz == Terminology
    }

    Boolean handles(String domainType) {
        return domainType != null && domainType.toLowerCase() in ['terminology', 'terminologies']
    }

    /*
    List<Term> childTermsByParent(Terminology fullTerminology, UUID id) {
        Map<UUID, Term> terms = fullTerminology.terms.collectEntries {[it.id, it]}
        Map<UUID, TermRelationshipType> termRelationshipTypes =
                fullTerminology.termRelationshipTypes.collectEntries {[it.id, it]}
        Map<UUID, TermRelationshipType> parentTermRelationshipTypes =
                termRelationshipTypes.findAll {it.value.parentalRelationship}
        Map<UUID, TermRelationshipType> childTermRelationshipTypes =
                termRelationshipTypes.findAll {it.value.childRelationship}
        Map<UUID, TermRelationship> parentChildTermRelationships =
                fullTerminology.termRelationships.findAll {
                    parentTermRelationshipTypes.containsKey(
                        it.relationshipType.id) ||
                        childTermRelationshipTypes.containsKey(it.relationshipType.id)
                }

        if (id) {
            fullTerminology.terms.collect {Term term ->

            }
        } else {

        }

    }
    */

    List<TreeItem> buildTree(Terminology fullTerminology, Term root, Integer depth = null) {
        Map<UUID, Term> terms = fullTerminology.terms.collectEntries {[it.id, it]}
        Map<UUID, TermRelationshipType> termRelationshipTypes =
            fullTerminology.termRelationshipTypes.collectEntries {[it.id, it]}

        Map<UUID, List<Term>> childTerms = [:]

        fullTerminology.termRelationships.forEach {
            Term child, parent
            if (termRelationshipTypes[it.relationshipType.id].childRelationship) {
                child = terms[it.sourceTerm.id]
                parent = terms[it.targetTerm.id]
                if (childTerms.containsKey(parent.id)) {
                    childTerms[parent.id].add(child)
                } else {
                    childTerms[parent.id] = [child]
                }
            }
            if (termRelationshipTypes[it.relationshipType.id].parentalRelationship) {
                child = terms[it.targetTerm.id]
                parent = terms[it.sourceTerm.id]
                if (parent.id in childTerms) {
                    childTerms[parent.id].add(child)
                } else {
                    childTerms[parent.id] = [child]
                }
            }
        }

        List<TreeItem> stack = []
        List<TreeItem> results = []
        if (root) {
            TreeItem tree = new TreeItem(id: root.id, label: root.definition, domainType: root.domainType)
            stack = [tree]
            results = tree.children
        } else {
            Set<UUID> childIds = childTerms.
                collectMany {it.value.collect {it.id}} as Set
            Collection<Term> rootTerms = terms.values().findAll {!childIds.contains(it.id)}
            stack = rootTerms.
                collect {new TreeItem(id: it.id, label: it.definition, domainType: it.domainType)}
            results.addAll(stack)
        }
        Integer currentDepth = 0
        while (!stack.empty && (!depth || currentDepth < depth)) {
            TreeItem treeItem = stack.pop()
            treeItem.children = childTerms[treeItem.id].
                collect {new TreeItem(id: it.id, label: it.definition, domainType: it.domainType)}
            treeItem.children.each {stack.push(it)}
            currentDepth += 1
        }

        results
    }
}

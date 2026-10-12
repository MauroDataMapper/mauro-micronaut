package org.maurodata.plugin.datamodel

import groovy.transform.CompileStatic
import jakarta.inject.Singleton
import org.maurodata.domain.comparison.ComparisonConclusion
import org.maurodata.domain.comparison.ComparisonContext
import org.maurodata.domain.comparison.ComparisonResult
import org.maurodata.domain.datamodel.DataElement

@CompileStatic
@Singleton
class SyntacticDataElementComparisonProvider implements DataElementComparisonProviderPlugin {

    String name = 'SyntacticDataElementComparisonProvider'

    String displayName = 'Syntactic DataElement Comparison Provider'

    String description = 'Compares DataElements using only explicitly declared syntactic structure'

    String version = '1.0.0'

    @Override
    List<ComparisonResult> compare(DataElement left, DataElement right, ComparisonContext context) {
        [
            compareLabel(left.label, right.label),
            compareLabelTokens(left.label, right.label),
            compareMultiplicity('minMultiplicity', left.minMultiplicity, right.minMultiplicity),
            compareMultiplicity('maxMultiplicity', left.maxMultiplicity, right.maxMultiplicity),
            compareCardinality(left, right)
        ].findAll {it != null} as List<ComparisonResult>
    }

    private ComparisonResult compareLabel(String left, String right) {
        new ComparisonResult(
            provider: name,
            comparisonType: 'label',
            comparedProperty: 'label',
            conclusion: left == right ? ComparisonConclusion.STRUCTURALLY_IDENTICAL : ComparisonConclusion.STRUCTURALLY_DIFFERENT,
            left: left,
            right: right,
            interpretation: 'Compares DataElement labels exactly.'
        )
    }

    private ComparisonResult compareLabelTokens(String left, String right) {
        Set<String> leftTokens = tokenise(left)
        Set<String> rightTokens = tokenise(right)
        Set<String> sharedTokens = leftTokens.intersect(rightTokens) as Set<String>

        new ComparisonResult(
            provider: name,
            comparisonType: 'labelTokens',
            comparedProperty: 'label',
            conclusion: concludeSets(leftTokens, rightTokens, sharedTokens),
            left: leftTokens.toSorted(),
            right: rightTokens.toSorted(),
            interpretation: 'Compares DataElement labels as case-insensitive word sets.',
            metadata: setMetrics(leftTokens, rightTokens, sharedTokens)
        )
    }

    private ComparisonResult compareMultiplicity(String property, Integer left, Integer right) {
        new ComparisonResult(
            provider: name,
            comparisonType: property,
            comparedProperty: property,
            conclusion: left == right ? ComparisonConclusion.STRUCTURALLY_IDENTICAL : ComparisonConclusion.STRUCTURALLY_DIFFERENT,
            left: left,
            right: right,
            interpretation: 'Compares declared multiplicity values exactly. Null means unspecified.'
        )
    }

    private ComparisonResult compareCardinality(DataElement left, DataElement right) {
        if (left.minMultiplicity == null || left.maxMultiplicity == null || right.minMultiplicity == null || right.maxMultiplicity == null) {
            return new ComparisonResult(
                provider: name,
                comparisonType: 'cardinality',
                comparedProperty: 'minMultiplicity,maxMultiplicity',
                conclusion: ComparisonConclusion.NOT_COMPARABLE_BY_THIS_PROVIDER,
                left: cardinality(left),
                right: cardinality(right),
                interpretation: 'Cardinality interval comparison requires both minMultiplicity and maxMultiplicity to be specified on both DataElements.',
                metadata: [
                    reason: 'unspecifiedMultiplicity'
                ] as Map<String, Object>
            )
        }

        if (left.minMultiplicity > left.maxMultiplicity || right.minMultiplicity > right.maxMultiplicity) {
            return new ComparisonResult(
                provider: name,
                comparisonType: 'cardinality',
                comparedProperty: 'minMultiplicity,maxMultiplicity',
                conclusion: ComparisonConclusion.STRUCTURALLY_INCOMPATIBLE,
                left: cardinality(left),
                right: cardinality(right),
                interpretation: 'At least one DataElement declares a minimum multiplicity greater than its maximum multiplicity.'
            )
        }

        new ComparisonResult(
            provider: name,
            comparisonType: 'cardinality',
            comparedProperty: 'minMultiplicity,maxMultiplicity',
            conclusion: concludeCardinality(left, right),
            left: cardinality(left),
            right: cardinality(right),
            interpretation: 'Compares declared cardinality intervals derived from minMultiplicity and maxMultiplicity.'
        )
    }

    private ComparisonConclusion concludeCardinality(DataElement left, DataElement right) {
        if (left.minMultiplicity == right.minMultiplicity && left.maxMultiplicity == right.maxMultiplicity) {
            return ComparisonConclusion.STRUCTURALLY_IDENTICAL
        }

        if (left.minMultiplicity >= right.minMultiplicity && left.maxMultiplicity <= right.maxMultiplicity) {
            return ComparisonConclusion.LEFT_STRUCTURALLY_NARROWS_RIGHT
        }

        if (right.minMultiplicity >= left.minMultiplicity && right.maxMultiplicity <= left.maxMultiplicity) {
            return ComparisonConclusion.RIGHT_STRUCTURALLY_NARROWS_LEFT
        }

        if (left.minMultiplicity <= right.maxMultiplicity && right.minMultiplicity <= left.maxMultiplicity) {
            return ComparisonConclusion.STRUCTURALLY_OVERLAPPING
        }

        ComparisonConclusion.STRUCTURALLY_DISJOINT
    }

    private ComparisonConclusion concludeSets(Set<String> leftValues, Set<String> rightValues, Set<String> sharedValues) {
        if (leftValues == rightValues) {
            return ComparisonConclusion.SETS_EQUAL
        }
        if (!sharedValues) {
            return ComparisonConclusion.SETS_DISJOINT
        }
        if (rightValues.containsAll(leftValues)) {
            return ComparisonConclusion.LEFT_IS_SUBSET_OF_RIGHT
        }
        if (leftValues.containsAll(rightValues)) {
            return ComparisonConclusion.RIGHT_IS_SUBSET_OF_LEFT
        }
        ComparisonConclusion.SETS_OVERLAP
    }

    private Set<String> tokenise(String value) {
        if (!value) {
            return [] as Set<String>
        }
        value.toLowerCase(Locale.UK)
            .split(/[^a-z0-9]+/)
            .findAll {it}
            .toSet() as Set<String>
    }

    private Map<String, Object> setMetrics(Set<String> leftValues, Set<String> rightValues, Set<String> sharedValues) {
        Set<String> unionValues = (leftValues + rightValues) as Set<String>
        [
            leftCount        : leftValues.size(),
            rightCount       : rightValues.size(),
            sharedCount      : sharedValues.size(),
            leftOnlyCount    : (leftValues - rightValues).size(),
            rightOnlyCount   : (rightValues - leftValues).size(),
            shared           : sharedValues.toSorted(),
            leftOnly         : (leftValues - rightValues).toSorted(),
            rightOnly        : (rightValues - leftValues).toSorted(),
            jaccardSimilarity: unionValues ? sharedValues.size() / unionValues.size() : 1G
        ] as Map<String, Object>
    }

    private Map<String, Integer> cardinality(DataElement dataElement) {
        [
            minMultiplicity: dataElement.minMultiplicity,
            maxMultiplicity: dataElement.maxMultiplicity
        ] as Map<String, Integer>
    }
}

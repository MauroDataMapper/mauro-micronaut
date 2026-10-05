package org.maurodata.plugin.datatype

import groovy.transform.CompileStatic
import jakarta.inject.Singleton
import org.maurodata.domain.datamodel.DataType
import org.maurodata.domain.datamodel.EnumerationValue
import org.maurodata.domain.comparison.ComparisonConclusion
import org.maurodata.domain.comparison.ComparisonContext
import org.maurodata.domain.comparison.ComparisonResult
import org.maurodata.domain.model.Model
import org.maurodata.domain.terminology.CodeSet
import org.maurodata.domain.terminology.Term
import org.maurodata.domain.terminology.Terminology

@CompileStatic
@Singleton
class SyntacticDataTypeComparisonProvider implements DataTypeComparisonProviderPlugin {

    String name = 'SyntacticDataTypeComparisonProvider'

    String displayName = 'Syntactic DataType Comparison Provider'

    String description = 'Compares DataTypes using only explicitly declared syntactic structure'

    String version = '1.0.0'

    @Override
    List<ComparisonResult> compare(DataType left, DataType right, ComparisonContext context) {
        List<ComparisonResult> results = []

        results.add(compareScalar('label', 'label', left.label, right.label, 'Compares DataType labels exactly.'))
        results.add(compareLabelTokens(left.label, right.label))
        results.add(compareDataTypeKind(left, right))

        if (left.units || right.units) {
            results.add(compareUnits(left.units, right.units))
        }

        compareDeclaredValueSets(left, right, context).tap {
            results.addAll(it)
        }

        // referenceClass comparison has been considered but is intentionally not implemented in this syntactic provider.

        results
    }

    private ComparisonResult compareScalar(String comparisonType, String comparedProperty, Object left, Object right, String interpretation) {
        new ComparisonResult(
            provider: name,
            comparisonType: comparisonType,
            comparedProperty: comparedProperty,
            conclusion: left == right ? ComparisonConclusion.STRUCTURALLY_IDENTICAL : ComparisonConclusion.STRUCTURALLY_DIFFERENT,
            left: left,
            right: right,
            interpretation: interpretation
        )
    }

    private ComparisonResult compareDataTypeKind(DataType left, DataType right) {
        Object leftKind = left.dataTypeKind?.stringValue
        Object rightKind = right.dataTypeKind?.stringValue
        new ComparisonResult(
            provider: name,
            comparisonType: 'dataTypeKind',
            comparedProperty: 'dataTypeKind',
            conclusion: leftKind == rightKind ? ComparisonConclusion.STRUCTURALLY_IDENTICAL :
                        ComparisonConclusion.STRUCTURALLY_INCOMPATIBLE,
            left: leftKind,
            right: rightKind,
            interpretation: 'Compares the declared DataType kind.'
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
            interpretation: 'Compares DataType labels as case-insensitive word sets.',
            metadata: setMetrics(leftTokens, rightTokens, sharedTokens)
        )
    }

    private ComparisonResult compareUnits(String left, String right) {
        UnitDefinition leftUnit = unitDefinition(left)
        UnitDefinition rightUnit = unitDefinition(right)

        Map<String, Object> metadata = [
            leftCanonical : leftUnit?.canonical,
            rightCanonical: rightUnit?.canonical,
            leftDimension : leftUnit?.dimension,
            rightDimension: rightUnit?.dimension,
            compatible    : false,
            convertible   : false
        ] as Map<String, Object>

        ComparisonConclusion conclusion
        String interpretation

        if (left == right) {
            conclusion = ComparisonConclusion.STRUCTURALLY_IDENTICAL
            metadata.compatible = true
            interpretation = 'Compares declared units exactly. The declared unit strings are identical.'
        } else if (!leftUnit || !rightUnit) {
            conclusion = ComparisonConclusion.NOT_COMPARABLE_BY_THIS_PROVIDER
            metadata.reason = !leftUnit && !rightUnit ? 'unknownUnits' : (!leftUnit ? 'unknownLeftUnit' : 'unknownRightUnit')
            interpretation = 'Compares declared units using a built-in SI and imperial unit registry. At least one unit is not recognised.'
        } else if (leftUnit.dimension != rightUnit.dimension) {
            conclusion = ComparisonConclusion.STRUCTURALLY_INCOMPATIBLE
            interpretation = 'Compares declared units using physical dimensions. The units measure different dimensions.'
        } else if (leftUnit.canonical == rightUnit.canonical) {
            conclusion = ComparisonConclusion.STRUCTURALLY_IDENTICAL
            metadata.compatible = true
            interpretation = 'Compares declared units using normalised SI and imperial aliases. The declared units are equivalent.'
        } else {
            conclusion = ComparisonConclusion.STRUCTURALLY_OVERLAPPING
            metadata.compatible = true
            metadata.convertible = true
            metadata.conversion = conversionFormula(leftUnit, rightUnit)
            interpretation = 'Compares declared units using physical dimensions. The units are compatible and convertible.'
        }

        new ComparisonResult(
            provider: name,
            comparisonType: 'units',
            comparedProperty: 'units',
            conclusion: conclusion,
            left: left,
            right: right,
            interpretation: interpretation,
            metadata: metadata
        )
    }

    private List<ComparisonResult> compareDeclaredValueSets(DataType left, DataType right, ComparisonContext context) {
        DeclaredValueSet leftValues = declaredValueSet(left, context.leftModelResource, 'left')
        DeclaredValueSet rightValues = declaredValueSet(right, context.rightModelResource, 'right')

        if (!leftValues.available && !rightValues.available) {
            return []
        }

        Set<String> leftCodes = leftValues.values.keySet()
        Set<String> rightCodes = rightValues.values.keySet()
        Set<String> sharedCodes = leftCodes.intersect(rightCodes) as Set<String>
        Set<String> leftOnlyCodes = leftCodes - rightCodes
        Set<String> rightOnlyCodes = rightCodes - leftCodes
        Map<String, Object> declaredValueSetMetadata = [
            leftSource : leftValues.source,
            rightSource: rightValues.source
        ] as Map<String, Object>
        declaredValueSetMetadata.putAll(setMetrics(leftCodes, rightCodes, sharedCodes))

        ComparisonConclusion conclusion = concludeSets(leftCodes, rightCodes, sharedCodes)
        List<ComparisonResult> results = [
            new ComparisonResult(
                provider: name,
                comparisonType: 'declaredValueSet',
                comparedProperty: 'codes',
                conclusion: conclusion,
                left: leftCodes.toSorted(),
                right: rightCodes.toSorted(),
                interpretation: 'Compares literal declared values using EnumerationValue.key and Term.code.',
                metadata: declaredValueSetMetadata
            )
        ]

        results.add(compareDeclaredValueValues(leftValues, rightValues, sharedCodes))

        results
    }

    private ComparisonResult compareDeclaredValueValues(DeclaredValueSet leftValues, DeclaredValueSet rightValues, Set<String> sharedCodes) {
        List<String> sortedSharedCodes = sharedCodes.toSorted()

        Map<String, String> leftSharedValues = sortedSharedCodes.collectEntries {String code ->
            [(code): leftValues.values[code]['value']]
        } as Map<String, String>
        Map<String, String> rightSharedValues = sortedSharedCodes.collectEntries {String code ->
            [(code): rightValues.values[code]['value']]
        } as Map<String, String>

        List<Map<String, Object>> changedValues = sortedSharedCodes.collect {String code ->
            Map<String, String> leftValue = leftValues.values[code]
            Map<String, String> rightValue = rightValues.values[code]
            String leftText = leftValue['value']
            String rightText = rightValue['value']
            if (leftText == rightText) {
                return null
            }
            Map<String, Object> changedValue = [:]
            changedValue['code'] = code
            changedValue['left'] = leftText
            changedValue['right'] = rightText
            changedValue
        }.findAll {it != null} as List<Map<String, Object>>

        Set<String> leftValueSet = leftValues.values.values().collect {Map<String, String> value -> value['value']}.findAll {it != null}.toSet() as Set<String>
        Set<String> rightValueSet = rightValues.values.values().collect {Map<String, String> value -> value['value']}.findAll {it != null}.toSet() as Set<String>
        Set<String> sharedValueSet = leftValueSet.intersect(rightValueSet) as Set<String>

        ComparisonConclusion conclusion
        String interpretation
        Map<String, Object> metadata = [
            comparedCodesCount: sortedSharedCodes.size(),
            changedCount      : changedValues.size(),
            unchangedCount    : sortedSharedCodes.size() - changedValues.size(),
            changed           : changedValues,
            valueSetMetrics   : setMetrics(leftValueSet, rightValueSet, sharedValueSet)
        ] as Map<String, Object>

        if (!sharedCodes) {
            conclusion = ComparisonConclusion.NOT_COMPARABLE_BY_THIS_PROVIDER
            metadata.reason = 'noSharedCodes'
            interpretation = 'Compares EnumerationValue.value and Term.definition for shared EnumerationValue.key or Term.code values. There are no shared codes to compare.'
        } else if (changedValues) {
            conclusion = ComparisonConclusion.STRUCTURALLY_DIFFERENT
            interpretation =
                'Compares EnumerationValue.value and Term.definition for shared EnumerationValue.key or Term.code values. At least one shared code has a different declared ' +
                'value.'
        } else {
            conclusion = ComparisonConclusion.STRUCTURALLY_IDENTICAL
            interpretation =
                'Compares EnumerationValue.value and Term.definition for shared EnumerationValue.key or Term.code values. All shared codes have identical declared values.'
        }

        new ComparisonResult(
            provider: name,
            comparisonType: 'declaredValueValues',
            comparedProperty: 'value,definition',
            conclusion: conclusion,
            left: leftSharedValues,
            right: rightSharedValues,
            interpretation: interpretation,
            metadata: metadata
        )
    }

    private ComparisonConclusion concludeSets(Set<String> leftCodes, Set<String> rightCodes, Set<String> sharedCodes) {
        if (leftCodes == rightCodes) {
            return ComparisonConclusion.SETS_EQUAL
        }
        if (!sharedCodes) {
            return ComparisonConclusion.SETS_DISJOINT
        }
        if (rightCodes.containsAll(leftCodes)) {
            return ComparisonConclusion.LEFT_IS_SUBSET_OF_RIGHT
        }
        if (leftCodes.containsAll(rightCodes)) {
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

    private DeclaredValueSet declaredValueSet(DataType dataType, Model modelResource, String side) {
        if (dataType.dataTypeKind == DataType.DataTypeKind.ENUMERATION_TYPE) {
            return new DeclaredValueSet(source: "${side}:EnumerationType", available: true, values: valuesFromEnumeration(dataType.enumerationValues))
        }

        if (modelResource instanceof Terminology) {
            return new DeclaredValueSet(source: "${side}:Terminology", available: true, values: valuesFromTerms(((Terminology) modelResource).terms))
        }

        if (modelResource instanceof CodeSet) {
            return new DeclaredValueSet(source: "${side}:CodeSet", available: true, values: valuesFromTerms(((CodeSet) modelResource).terms as Collection<Term>))
        }

        new DeclaredValueSet(source: "${side}:none", available: false, values: [:] as Map<String, Map<String, String>>)
    }

    private Map<String, Map<String, String>> valuesFromEnumeration(Collection<EnumerationValue> enumerationValues) {
        Map<String, Map<String, String>> values = [:]
        (enumerationValues ?: Collections.<EnumerationValue> emptyList()).each {EnumerationValue enumerationValue ->
            if (enumerationValue.key) {
                Map<String, String> value = [:]
                value['value'] = enumerationValue.value
                value['category'] = enumerationValue.category
                value['description'] = enumerationValue.description ?: enumerationValue.value
                values[enumerationValue.key] = value
            }
        }
        values
    }

    private Map<String, Map<String, String>> valuesFromTerms(Collection<Term> terms) {
        Map<String, Map<String, String>> values = [:]
        (terms ?: Collections.<Term> emptyList()).each {Term term ->
            if (term.code) {
                Map<String, String> value = [:]
                value['value'] = term.definition
                value['category'] = null
                value['description'] = term.description ?: term.definition
                values[term.code] = value
            }
        }
        values
    }

    private UnitDefinition unitDefinition(String unit) {
        if (!unit) {
            return null
        }
        UNIT_DEFINITIONS[normaliseUnit(unit)]
    }

    private String normaliseUnit(String unit) {
        unit.trim().toLowerCase(Locale.UK).replaceAll(/[._-]+/, ' ').replaceAll(/\s+/, ' ')
    }

    private String conversionFormula(UnitDefinition left, UnitDefinition right) {
        BigDecimal multiplier = right.toBase / left.toBase
        if (right.offsetToBase || left.offsetToBase) {
            BigDecimal offset = (right.offsetToBase - left.offsetToBase) / left.toBase
            return "${left.canonical} = ${right.canonical} * ${formatDecimal(multiplier)} + ${formatDecimal(offset)}"
        }
        "${left.canonical} = ${right.canonical} * ${formatDecimal(multiplier)}"
    }

    private String formatDecimal(BigDecimal value) {
        value.stripTrailingZeros().toPlainString()
    }

    private static UnitDefinition unit(String canonical, String dimension, Number toBase, String... aliases) {
        UnitDefinition definition = new UnitDefinition(canonical: canonical, dimension: dimension, toBase: new BigDecimal(toBase.toString()), offsetToBase: 0G)
        ([canonical] + aliases.toList()).each {String alias ->
            UNIT_DEFINITIONS[alias.trim().toLowerCase(Locale.UK).replaceAll(/[._-]+/, ' ').replaceAll(/\s+/, ' ')] = definition
        }
        definition
    }

    private static class DeclaredValueSet {
        String source
        boolean available
        Map<String, Map<String, String>> values = [:]
    }

    private static class UnitDefinition {
        String canonical
        String dimension
        BigDecimal toBase
        BigDecimal offsetToBase
    }

    private static final Map<String, UnitDefinition> UNIT_DEFINITIONS = [:] as Map<String, UnitDefinition>

    static {
        unit('m', 'length', 1G, 'meter', 'meters', 'metre', 'metres')
        unit('km', 'length', 1000G, 'kilometer', 'kilometers', 'kilometre', 'kilometres')
        unit('cm', 'length', 0.01G, 'centimeter', 'centimeters', 'centimetre', 'centimetres')
        unit('mm', 'length', 0.001G, 'millimeter', 'millimeters', 'millimetre', 'millimetres')
        unit('um', 'length', 0.000001G, 'micrometer', 'micrometers', 'micrometre', 'micrometres', 'micron', 'microns')
        unit('nm', 'length', 0.000000001G, 'nanometer', 'nanometers', 'nanometre', 'nanometres')
        unit('in', 'length', 0.0254G, 'inch', 'inches')
        unit('ft', 'length', 0.3048G, 'foot', 'feet')
        unit('yd', 'length', 0.9144G, 'yard', 'yards')
        unit('mi', 'length', 1609.344G, 'mile', 'miles')
        unit('nmi', 'length', 1852G, 'nautical mile', 'nautical miles')

        unit('kg', 'mass', 1G, 'kilogram', 'kilograms')
        unit('g', 'mass', 0.001G, 'gram', 'grams')
        unit('mg', 'mass', 0.000001G, 'milligram', 'milligrams')
        unit('ug', 'mass', 0.000000001G, 'microgram', 'micrograms')
        unit('tonne', 'mass', 1000G, 't', 'metric ton', 'metric tons', 'metric tonne', 'metric tonnes')
        unit('oz', 'mass', 0.028349523125G, 'ounce', 'ounces')
        unit('lb', 'mass', 0.45359237G, 'lbs', 'pound', 'pounds')
        unit('st', 'mass', 6.35029318G, 'stone', 'stones')
        unit('ton', 'mass', 907.18474G, 'short ton', 'short tons')

        unit('s', 'time', 1G, 'sec', 'second', 'seconds')
        unit('ms', 'time', 0.001G, 'millisecond', 'milliseconds')
        unit('min', 'time', 60G, 'minute', 'minutes')
        unit('h', 'time', 3600G, 'hr', 'hour', 'hours')
        unit('d', 'time', 86400G, 'day', 'days')

        unit('A', 'electric_current', 1G, 'amp', 'amps', 'ampere', 'amperes')
        unit('K', 'temperature', 1G, 'kelvin', 'kelvins')
        unit('mol', 'amount_of_substance', 1G, 'mole', 'moles')
        unit('cd', 'luminous_intensity', 1G, 'candela', 'candelas')

        unit('rad', 'angle', 1G, 'radian', 'radians')
        unit('sr', 'solid_angle', 1G, 'steradian', 'steradians')
        unit('Hz', 'frequency', 1G, 'hertz')
        unit('N', 'force', 1G, 'newton', 'newtons')
        unit('Pa', 'pressure', 1G, 'pascal', 'pascals')
        unit('J', 'energy', 1G, 'joule', 'joules')
        unit('W', 'power', 1G, 'watt', 'watts')
        unit('C', 'electric_charge', 1G, 'coulomb', 'coulombs')
        unit('V', 'electric_potential', 1G, 'volt', 'volts')
        unit('F', 'capacitance', 1G, 'farad', 'farads')
        unit('ohm', 'electric_resistance', 1G)
        unit('S', 'electric_conductance', 1G, 'siemens')
        unit('Wb', 'magnetic_flux', 1G, 'weber', 'webers')
        unit('T', 'magnetic_flux_density', 1G, 'tesla', 'teslas')
        unit('H', 'inductance', 1G, 'henry', 'henrys')
        unit('lm', 'luminous_flux', 1G, 'lumen', 'lumens')
        unit('lx', 'illuminance', 1G, 'lux')
        unit('Bq', 'radioactivity', 1G, 'becquerel', 'becquerels')
        unit('Gy', 'absorbed_dose', 1G, 'gray', 'grays')
        unit('Sv', 'dose_equivalent', 1G, 'sievert', 'sieverts')
        unit('kat', 'catalytic_activity', 1G, 'katal', 'katals')

        unit('L', 'volume', 0.001G, 'l', 'litre', 'litres', 'liter', 'liters')
        unit('mL', 'volume', 0.000001G, 'ml', 'millilitre', 'millilitres', 'milliliter', 'milliliters')
        unit('gal', 'volume', 0.003785411784G, 'gallon', 'gallons', 'us gallon', 'us gallons')
        unit('qt', 'volume', 0.000946352946G, 'quart', 'quarts')
        unit('pt', 'volume', 0.000473176473G, 'pint', 'pints')
        unit('fl oz', 'volume', 0.0000295735295625G, 'fluid ounce', 'fluid ounces')
        unit('imp gal', 'volume', 0.00454609G, 'imperial gallon', 'imperial gallons')
        unit('imp pt', 'volume', 0.00056826125G, 'imperial pint', 'imperial pints')
    }
}

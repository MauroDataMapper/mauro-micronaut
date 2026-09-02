/* Keep this file aligned with .groovylintrc.json
 */

ruleset {
    description "Project-specific CodeNarc rules with the repo's preferred overrides."

    LineLength(length: 200)
    SpaceAfterOpeningBrace(enabled: false)
    SpaceBeforeOpeningBrace(enabled: false)
    SpaceBeforeClosingBrace(enabled: false)
    SpaceAfterClosingBrace(enabled: false)
    Indentation(enabled: false)
    TrailingWhitespace(enabled: false)
    ClassStartsWithBlankLine(enabled: false)
    ClassEndsWithBlankLine(enabled: false)
    SpaceAfterIf(enabled: false)
    BlockEndsWithBlankLine(enabled: false)
    BlockStartsWithBlankLine(enabled: false)
    MissingBlankLineBeforeAnnotatedField(enabled: false)
    SpaceAfterComma(enabled: false)
    ConsecutiveBlankLines(enabled: false)
    SpaceAroundOperator(enabled: false)
    SpaceInsideParentheses(enabled: false)
    SpaceAfterMethodCallName(enabled: false)

    CompileStatic(enabled: false)

    PublicMethodsBeforeNonPublicMethods(enabled: false)
    StaticMethodsBeforeInstanceMethods(enabled: false)
    StaticFieldsBeforeInstanceFields(enabled: false)

    NestedBlockDepth(enabled: false)
    ParameterCount(enabled: false)
    UnnecessaryGString(enabled: false)
    DuplicateStringLiteral(enabled: false)
    DuplicateMapLiteral(enabled: false)
    DuplicateNumberLiteral(enabled: false)
    UnnecessaryToString(enabled: false)

    UnnecessaryGetter(enabled: false)
    UnnecessarySetter(enabled: false)
    UnnecessaryReturnKeyword(enabled: false)
    UnnecessaryObjectReferences(enabled: false)

    ConfusingMethodName(enabled: false)
    GetterMethodCouldBeProperty(enabled: false)

    ClassJavadoc(enabled: false)
    JavadocEmptyReturnTag(enabled: false)

    ImplicitReturnStatement(enabled: false)
    ImplicitClosureParameter(enabled: false)

    UnusedMethodParameter(enabled: false)
    FactoryMethodName(enabled: false)
    MethodName(enabled: false)
    UnnecessaryElseStatement(enabled: false)
    UnnecessaryOverridingMethod(enabled: false)
    ParameterReassignment(enabled: false)
    Instanceof(enabled: false)

    JUnitPublicProperty(enabled: false)
    JUnitPublicNonTestMethod(enabled: false)
    JUnitTestMethodWithoutAssert(enabled: false)

    ImplementationAsType(enabled: false)
}

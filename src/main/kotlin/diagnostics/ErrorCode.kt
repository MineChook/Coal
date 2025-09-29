package diagnostics

/**
 * Enum class representing various error codes and their corresponding message templates
 */
enum class ErrorCode(val code: String, val template: String) {
    // Lexical
    UnexpectedChar("E0001", "Unexpected character: '${0}'"),
    UnterminatedString("E0002", "Unterminated string literal"),
    UnterminatedChar("E0003", "Unterminated character literal"),
    UnknownEscapeSequence("E0004", "Unknown escape sequence: '\\${0}'"),
    EmptyCharLiteral("E0005", "Empty character literal"),

    // Parsing
    ExpectedToken("E0101", "Expected token: '${0}', but found: '${1}'"),
    ExpectedExpr("E0102", "Expected an expression"),

    // Typing / semantics
    AssignToConst("E0103", "Cannot assign to constant variable: '${0}'"),
    ConstNeedsInit("E0104", "Constant variable '${0}' must be initialized"),
    VarNeedsType("E0105", "Variable '${0}' must have an explicit type if not initialized"),
    CompareTypeMismatch("E0106", "Cannot compare values of type '${0}' and '${1}'"),
    RelopTypeInvalid("E0107", "Relational operator '${0}' cannot be applied to type '${1}'"),
    LogicNeedsBool("E0108", "Logical operator '${0}' requires boolean operands"),
    NotConditionBool("E0109", "Logical NOT operator requires a boolean operand, found '${0}'"),
    UnknownMethod("E0110", "Unknown method: '${0}'"),
    CannotInferType("E0111", "Cannot infer type for variable '${0}'"),
    OnlyEqOnStrings("E0112", "Only equality operators can be used with strings"),
    StringsOnlyAdd("E0113", "Only the '+' operator can be used with strings"),
    UnsupportedBinary("E0114", "Unsupported binary operation: '${0}' between '${1}' and '${2}'"),
    UnsupportedPrintType("E0115", "Unsupported type for print: '${0}'"),
    UnsupportedConversion("E0116", "Unsupported conversion from '${0}' to '${1}'"),
    InvalidType("E0117", "Invalid type: '${0}'"),

    // Extra semantic coverage
    UndefinedVariable("E0120", "Use of undefined variable: '${0}'"),
    RedeclaredVariable("E0121", "Redeclaration of variable: '${0}'"),
    UnknownFunction("E0122", "Unknown function: '${0}'"),
    ArityMismatch("E0123", "Function '${0}' called with ${1} argument(s), but ${2} expected"),
    NonBoolCondition("E0124", "Condition must be boolean, found '${0}'"),
    TypeMismatch("E0125", "Type mismatch: expected '${0}', found '${1}'"),

    // Codegen
    Internal("E1001", "Internal compiler error: ${0}"),
}
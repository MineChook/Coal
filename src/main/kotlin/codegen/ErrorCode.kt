package codegen

enum class ErrorCode(val code: String, val template: String) {
    // Lexical Errors
    UnexpectedChar("E0001", "Unexpected character: '${0}'"),
    UnterminatedString("E0002", "Unterminated string literal"),
    UnterminatedChar("E0003", "Unterminated character literal"),
    UnknownEscapeSequence("E0004", "Unknown escape sequence: '\\${0}'"),
    EmptyCharLiteral("E0005", "Empty character literal"),

    // Parsing/typing Errors
    ExpectedToken("E0101", "Expected token: '${0}', but found: '${1}'"),
    ExpectedExpr("E0102", "Expected an expression"),
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

    // Codegen
    Internal("E1001", "Internal compiler error: ${0}"),
}
package community.flock.wirespec.compiler.core.emit

import community.flock.wirespec.compiler.core.emit.common.DEFAULT_PACKAGE_NAME
import community.flock.wirespec.compiler.core.emit.common.AbstractEmitter
import community.flock.wirespec.compiler.core.parse.AST
import community.flock.wirespec.compiler.core.parse.nodes.Endpoint
import community.flock.wirespec.compiler.core.parse.nodes.Enum
import community.flock.wirespec.compiler.core.parse.nodes.Refined
import community.flock.wirespec.compiler.core.parse.nodes.Type
import community.flock.wirespec.compiler.core.parse.nodes.Type.Shape.Field.Reference
import community.flock.wirespec.compiler.utils.Logger
import community.flock.wirespec.compiler.utils.noLogger

class ScalaEmitter(
    private val packageName: String = DEFAULT_PACKAGE_NAME,
    logger: Logger = noLogger
) : AbstractEmitter(logger) {

    override val shared = ""

    override fun emit(ast: AST): List<Pair<String, String>> = super.emit(ast)
        .map { (name, result) -> name to if (packageName.isBlank()) "" else "package $packageName\n\n$result" }

    override fun Type.emit() = withLogging(logger) {
        """case class $name(
            |${shape.emit()}
            |)
            |
            |""".trimMargin()
    }

    override fun Type.Shape.emit() = withLogging(logger) {
        value.joinToString("\n") { it.emit() }.dropLast(1)
    }

    override fun Type.Shape.Field.emit() = withLogging(logger) {
        "${SPACER}val ${identifier.emit()}: ${if (isNullable) "Option[${reference.emit()}]" else reference.emit()},"
    }

    override fun Type.Shape.Field.Identifier.emit() = withLogging(logger) {
        if (preservedKeywords.contains(value)) "`$value`" else value
    }

    override fun Reference.emit() = withLogging(logger) {
        when (this) {
            is Reference.Unit -> "Unit"
            is Reference.Any -> "Any"
            is Reference.Custom -> value
            is Reference.Primitive -> when (type) {
                Reference.Primitive.Type.String -> "String"
                Reference.Primitive.Type.Integer -> "Int"
                Reference.Primitive.Type.Number -> "Double"
                Reference.Primitive.Type.Boolean -> "Boolean"
            }
        }.let { if (isIterable) "List[$it]" else it }
    }

    override fun Enum.emit() = withLogging(logger) {
        fun String.sanitize() = replace("-", "_").let { if(it.first().isDigit()) "_$it" else it }
        """
        |sealed abstract class $name(val label: String)
        |object $name {
        |${entries.joinToString("\n") { """${SPACER}final case object ${it.sanitize().uppercase()} extends $name(label = "$it")""" }}
        |}
        |""".trimMargin()
    }

    override fun Refined.emit() = withLogging(logger) {
        """case class $name(val value: String) {
            |${SPACER}implicit class ${name}Ops(val that: $name) {
            |${validator.emit()}
            |${SPACER}}
            |}
            |
            |""".trimMargin()
    }

    override fun Refined.Validator.emit() = withLogging(logger) {
        """${SPACER}${SPACER}val regex = new scala.util.matching.Regex($value)
            |${SPACER}${SPACER}regex.findFirstIn(that.value)""".trimMargin()
    }

    override fun Endpoint.emit() = withLogging(logger) {
        """// TODO("Not yet implemented")
            |
        """.trimMargin()
    }

    companion object {
        private val preservedKeywords = listOf(
            "abstract",
            "case",
            "catch",
            "class",
            "def",
            "do",
            "else",
            "extends",
            "false",
            "final",
            "finally",
            "for",
            "forSome",
            "if",
            "implicit",
            "import",
            "lazy",
            "match",
            "new",
            "null",
            "object",
            "override",
            "package",
            "private",
            "protected",
            "return",
            "sealed",
            "super",
            "this",
            "throw",
            "trait",
            "true",
            "try",
            "type",
            "val",
            "var",
            "while",
            "with",
            "yield",
        )
    }

}

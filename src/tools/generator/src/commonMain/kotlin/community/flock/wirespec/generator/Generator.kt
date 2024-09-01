package community.flock.wirespec.generator

import community.flock.kotlinx.rgxgen.RgxGen
import community.flock.wirespec.compiler.core.parse.AST
import community.flock.wirespec.compiler.core.parse.Channel
import community.flock.wirespec.compiler.core.parse.Definition
import community.flock.wirespec.compiler.core.parse.Endpoint
import community.flock.wirespec.compiler.core.parse.Enum
import community.flock.wirespec.compiler.core.parse.Field
import community.flock.wirespec.compiler.core.parse.Reference
import community.flock.wirespec.compiler.core.parse.Refined
import community.flock.wirespec.compiler.core.parse.Type
import community.flock.wirespec.compiler.core.parse.Union
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlin.random.Random

fun AST.generate(type: String, random: Random = Random.Default): JsonElement = Reference.Custom(
    value = type.removeSuffix("[]"),
    isIterable = type.endsWith("[]"),
    isDictionary = false
).let { generate(it, random) }

fun AST.generate(type: Reference, random: Random = Random.Default): JsonElement =
    generateReference(type, random)

private fun AST.resolveReference(type: Reference) = filterIsInstance<Definition>()
    .find { it.identifier.value == type.value }
    ?: error("Definition not found in AST: $type")

private fun AST.generateIterator(def: Definition, random: Random): JsonElement = (0..random.nextInt(10))
    .map { generateObject(def, random) }
    .let(::JsonArray)

private fun AST.generateReference(ref: Reference, random: Random) = when (ref) {
    is Reference.Primitive -> when (ref.type) {
        Reference.Primitive.Type.String -> RgxGen.parse("\\w{1,50}").generate(random).let(::JsonPrimitive)
        Reference.Primitive.Type.Integer -> random.nextInt().let(::JsonPrimitive)
        Reference.Primitive.Type.Number -> random.nextDouble().let(::JsonPrimitive)
        Reference.Primitive.Type.Boolean -> random.nextBoolean().let(::JsonPrimitive)
    }

    is Reference.Custom -> resolveReference(ref)
        .let { if (ref.isIterable) generateIterator(it, random) else generateObject(it, random) }

    is Reference.Unit -> JsonNull
    is Reference.Any -> throw NotImplementedError("Cannot generate Any")
}

private fun AST.generateType(def: Type, random: Random): JsonObject = random.nextInt().let { typeSeed ->
    def.shape.value
        .fold<Field, Map<String, JsonElement>>(emptyMap()) { acc, cur ->
            cur.identifier.value.let { value ->
                val fieldSeed = typeSeed + value.sumOf { it.code }
                val fieldRandom = Random(fieldSeed)
                acc.plus(value to generateReference(cur.reference, fieldRandom))
            }
        }
        .let(::JsonObject)
}

private fun randomRegex(regex: String, random: Random) = RgxGen
    .parse(regex.substring(1, regex.length - 2))
    .generate(random)

private fun generateRefined(def: Refined, random: Random) = randomRegex(def.validator.value, random)
    .let(::JsonPrimitive)

private fun generateEnum(def: Enum, random: Random) = random
    .nextInt(def.entries.size)
    .let(def.entries.toList()::get)
    .let(::JsonPrimitive)

private fun AST.generateUnion(def: Union, random: Random) = random
    .nextInt(def.entries.size)
    .let(def.entries.toList()::get)
    .let { generate(it, random) }

private fun AST.generateObject(def: Definition, random: Random) = when (def) {
    is Type -> generateType(def, random)
    is Refined -> generateRefined(def, random)
    is Enum -> generateEnum(def, random)
    is Union -> generateUnion(def, random)
    is Endpoint -> throw NotImplementedError("Endpoint cannot be generated")
    is Channel -> throw NotImplementedError("Channel cannot be generated")
}

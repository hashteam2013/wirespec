package community.flock.wirespec.compiler.core.parse

import community.flock.wirespec.compiler.core.WirespecSpec
import community.flock.wirespec.compiler.core.tokenize.tokenize
import community.flock.wirespec.compiler.utils.noLogger
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class ParserReferenceTest {

    private fun parser() = Parser(noLogger)
    private fun compile(source: String) = WirespecSpec.tokenize(source)
        .let(parser()::parse)

    @Test
    fun shouldHaveSelfRef() {
        val source = """
            |type Self {
            |  self: Self
            |}

        """.trimMargin()

        compile(source)
            .shouldBeRight()
    }

    @Test
    fun shouldNotFindReferenceInType() {
        val source = """
            |type Foo {
            |  bar: Bar
            |}
        """.trimMargin()

        compile(source)
            .shouldBeLeft()
            .also { it.size shouldBe 1 }
            .first()
            .run {
                message shouldBe "Cannot find reference: Bar"
                coordinates.line shouldBe 2
                coordinates.position shouldBe 11
                coordinates.idxAndLength.idx shouldBe 21
                coordinates.idxAndLength.length shouldBe 3
            }
    }

    @Test
    fun shouldNotFindReferenceInEndpointRequest() {
        val source = """
            |endpoint FooPoint POST Foo /foo -> {
            |  200 -> Bar
            |}
        """.trimMargin()

        compile(source)
            .shouldBeLeft()
            .apply { size shouldBe 1 }
            .apply {
                first().message shouldBe "Cannot find reference: Foo"
                first().coordinates.line shouldBe 1
                first().coordinates.position shouldBe 27
                first().coordinates.idxAndLength.idx shouldBe 26
                first().coordinates.idxAndLength.length shouldBe 3
            }
    }

    @Test
    fun shouldNotFindReferenceInEndpointResponse() {
        val source = """
            |type Foo { str:String }
            |endpoint FooPoint POST Foo /foo -> {
            |  200 -> Bar
            |}
        """.trimMargin()

        compile(source)
            .shouldBeLeft()
            .apply { size shouldBe 1 }
            .apply {
                first().message shouldBe "Cannot find reference: Bar"
                first().coordinates.line shouldBe 3
                first().coordinates.position shouldBe 13
                first().coordinates.idxAndLength.idx shouldBe 73
                first().coordinates.idxAndLength.length shouldBe 3
            }
    }

    @Test
    fun shouldNotFindReferenceInEnum() {
        val source = """
            |type Foo = Bar
        """.trimMargin()

        compile(source)
            .shouldBeLeft()
            .apply { size shouldBe 1 }
            .apply {
                first().message shouldBe "Cannot find reference: Bar"
                first().coordinates.line shouldBe 1
                first().coordinates.position shouldBe 15
                first().coordinates.idxAndLength.idx shouldBe 14
                first().coordinates.idxAndLength.length shouldBe 3
            }
    }

    @Test
    fun shouldNotFindReferenceInEnumSecond() {
        val source = """
            |type Bar { str:String }
            |type Foo = Bar | Baz
        """.trimMargin()

        compile(source)
            .shouldBeLeft()
            .apply { size shouldBe 1 }
            .apply {
                first().message shouldBe "Cannot find reference: Baz"
                first().coordinates.line shouldBe 2
                first().coordinates.position shouldBe 21
                first().coordinates.idxAndLength.idx shouldBe 44
                first().coordinates.idxAndLength.length shouldBe 3
            }
    }
}

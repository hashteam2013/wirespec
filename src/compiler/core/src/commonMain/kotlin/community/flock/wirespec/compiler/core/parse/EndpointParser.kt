package community.flock.wirespec.compiler.core.parse

import arrow.core.Either
import arrow.core.raise.either
import community.flock.wirespec.compiler.core.exceptions.WirespecException
import community.flock.wirespec.compiler.core.exceptions.WirespecException.CompilerException.ParserException.WrongTokenException
import community.flock.wirespec.compiler.core.parse.nodes.Endpoint
import community.flock.wirespec.compiler.core.parse.nodes.Type
import community.flock.wirespec.compiler.core.parse.nodes.Type.Shape.Field.Reference.Custom
import community.flock.wirespec.compiler.core.parse.nodes.Type.Shape.Field.Reference.Primitive
import community.flock.wirespec.compiler.core.tokenize.types.Arrow
import community.flock.wirespec.compiler.core.tokenize.types.Brackets
import community.flock.wirespec.compiler.core.tokenize.types.Colon
import community.flock.wirespec.compiler.core.tokenize.types.CustomType
import community.flock.wirespec.compiler.core.tokenize.types.CustomValue
import community.flock.wirespec.compiler.core.tokenize.types.ForwardSlash
import community.flock.wirespec.compiler.core.tokenize.types.LeftCurly
import community.flock.wirespec.compiler.core.tokenize.types.Method
import community.flock.wirespec.compiler.core.tokenize.types.Path
import community.flock.wirespec.compiler.core.tokenize.types.RightCurly
import community.flock.wirespec.compiler.core.tokenize.types.StatusCode
import community.flock.wirespec.compiler.core.tokenize.types.WirespecType
import community.flock.wirespec.compiler.core.tokenize.types.WsBoolean
import community.flock.wirespec.compiler.core.tokenize.types.WsInteger
import community.flock.wirespec.compiler.core.tokenize.types.WsNumber
import community.flock.wirespec.compiler.core.tokenize.types.WsString
import community.flock.wirespec.compiler.utils.Logger

class EndpointParser(logger: Logger) : AbstractParser(logger) {

    fun TokenProvider.parseEndpoint(): Either<WirespecException, Endpoint> = either {
        eatToken().bind()
        token.log()
        when (token.type) {
            is CustomType -> parseEndpointDefinition(token.value).bind()
            else -> raise(WrongTokenException<CustomType>(token).also { eatToken().bind() })
        }
    }

    private fun TokenProvider.parseEndpointDefinition(name: String) = either {
        eatToken().bind()
        token.log()
        val method = when (token.type) {
            is Method -> Endpoint.Method.valueOf(token.value)
            else -> raise(WrongTokenException<Method>(token))
        }.also { eatToken().bind() }

        val requests = listOfNotNull(
            when (token.type) {
                is WirespecType -> parseReference(token.type as WirespecType, token.value).bind()
                else -> null
            }
        ).map {
            Endpoint.Request(
                content = Endpoint.Content(
                    type = "application/json",
                    reference = it,
                    isNullable = false
                )
            )
        }

        val segments = mutableListOf<Endpoint.Segment>().apply {
            while (token.type !is Arrow) {
                add(parseEndpointSegments().bind())
            }
        }.also { eatToken().bind() }

        when (token.type) {
            is LeftCurly -> Unit
            else -> raise(WrongTokenException<LeftCurly>(token))
        }.also { eatToken().bind() }

        val responses = parseEndpointResponses().bind()

        Endpoint(
            name = name,
            method = method,
            path = segments,
            query = emptyList(),
            headers = emptyList(),
            cookies = emptyList(),
            requests = requests,
            responses = responses,
        )
    }

    private fun TokenProvider.parseEndpointSegments() = either {
        token.log()
        when (token.type) {
            is Path -> Endpoint.Segment.Literal(token.value.drop(1)).also { eatToken().bind() }
            is ForwardSlash -> parseEndpointSegmentParam().bind()
            else -> raise(WrongTokenException<Path>(token))
        }
    }

    private fun TokenProvider.parseEndpointSegmentParam() = either {
        eatToken().bind()
        token.log()
        when (token.type) {
            is LeftCurly -> eatToken().bind()
            else -> raise(WrongTokenException<LeftCurly>(token))
        }
        val identifier = when (token.type) {
            is CustomValue -> Type.Shape.Field.Identifier(token.value).also { eatToken().bind() }
            else -> raise(WrongTokenException<CustomValue>(token))
        }
        when (token.type) {
            is Colon -> eatToken().bind()
            else -> raise(WrongTokenException<Colon>(token))
        }
        val reference = when (token.type) {
            is WirespecType -> parseReference(token.type as WirespecType, token.value).bind()
            else -> raise(WrongTokenException<WirespecType>(token))
        }
        when (token.type) {
            is RightCurly -> eatToken().bind()
            else -> raise(WrongTokenException<RightCurly>(token))
        }
        Endpoint.Segment.Param(
            identifier = identifier,
            reference = reference
        )
    }

    private fun TokenProvider.parseEndpointResponses() = either {
        token.log()
        val responses = mutableListOf<Endpoint.Response>()
        while (token.type !is RightCurly) {
            when (token.type) {
                is StatusCode -> responses.add(parseEndpointResponse(token.value).bind())
                else -> raise(WrongTokenException<StatusCode>(token))
            }
        }
        when (token.type) {
            is RightCurly -> Unit
            else -> raise(WrongTokenException<RightCurly>(token))
        }.also { eatToken().bind() }
        responses.toList()
    }

    private fun TokenProvider.parseEndpointResponse(statusCode: String) = either {
        eatToken().bind()
        token.log()
        when (token.type) {
            is Arrow -> Unit
            else -> raise(WrongTokenException<Arrow>(token))
        }
        eatToken().bind()
        token.log()
        val content = when (token.type) {
            is WirespecType -> parseContent(token.type as WirespecType, token.value).bind()
            else -> raise(WrongTokenException<WirespecType>(token))
        }
        Endpoint.Response(status = statusCode, content = content)
    }

    private fun TokenProvider.parseContent(wsType: WirespecType, value: String) = either {
        token.log()
        Endpoint.Content(
            type = "application/json",
            reference = parseReference(wsType, value).bind(),
        )
    }

    private fun TokenProvider.parseReference(wsType: WirespecType, value: String) = either {
        eatToken().bind()
        token.log()
        val isIterable = (token.type is Brackets).also { if (it) eatToken().bind() }
        when (wsType) {
            is WsString -> Primitive(Primitive.Type.String, isIterable)

            is WsInteger -> Primitive(Primitive.Type.Integer, isIterable)
            is WsNumber -> Primitive(Primitive.Type.Number, isIterable)

            is WsBoolean -> Primitive(Primitive.Type.Boolean, isIterable)

            is CustomType -> Custom(value, isIterable)
        }
    }
}

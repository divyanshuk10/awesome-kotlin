package link.kotlin.server.handlers

import at.favre.lib.crypto.bcrypt.BCrypt
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.undertow.server.HttpServerExchange
import io.undertow.util.AttachmentKey
import io.undertow.util.Headers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import link.kotlin.server.utils.logger
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.util.*
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class LoginHandler(
    private val bcryptVerifier: BCrypt.Verifyer,
) : CoroutinesHttpHandler {
    data class LoginBody(
        val username: String,
        val password: CharArray
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as LoginBody

            if (username != other.username) return false
            if (!password.contentEquals(other.password)) return false

            return true
        }

        override fun hashCode(): Int {
            var result = username.hashCode()
            result = 31 * result + password.contentHashCode()
            return result
        }
    }

    override suspend fun handleRequest(exchange: HttpServerExchange) {
        val params = exchange.getBody<LoginBody>()

        val result = withContext(Dispatchers.IO) {
            bcryptVerifier.verify(params.password, "$2a$10\$jlOnyAVW2TitTrUI05ru1Og4FFub.5M5p.heDLQp.p.gIIdvioFEW".toCharArray())
        }

        Arrays.fill(params.password, '0')

        exchange.respondJson(result.verified)
    }
}

val REQUEST_OBJECT_MAPPER: AttachmentKey<ObjectMapper> = AttachmentKey.create(ObjectMapper::class.java)

suspend inline fun <reified T : Any> HttpServerExchange.getBody(): T {
    val objectMapper = getAttachment(REQUEST_OBJECT_MAPPER) ?: throw InternalServerError()

    val obj: T = try {
        objectMapper.readValue(readBody())
    } catch (e: Exception) {
        throw BadRequest()
    }

    // Validate body is applicable
    (obj as? Validatable)?.validate()

    return obj
}

interface Validatable {
    /**
     * @throws [ValidationException]
     */
    fun validate()
}

class BadRequest : RuntimeException()
class InternalServerError : RuntimeException()
class ValidationException : RuntimeException()

suspend inline fun HttpServerExchange.readBody(): String {
    return suspendCoroutine { continuation: Continuation<String> ->
        this.requestReceiver.receiveFullString(
            { _, message -> continuation.resume(message) },
            { _, e -> continuation.resumeWithException(e) },
            StandardCharsets.UTF_8
        )
    }
}

fun HttpServerExchange.respondJson(obj: Any) {
    val objectMapper = getAttachment(REQUEST_OBJECT_MAPPER) ?: throw InternalServerError()

    if (isResponseChannelAvailable) {
        try {
            val json = objectMapper.writeValueAsBytes(obj)
            responseHeaders.add(Headers.CONTENT_TYPE, "application/json")
            responseSender.send(ByteBuffer.wrap(json))
        } catch (e: Exception) {
            LOGGER.error("Can't send json. Class: ${obj::class.simpleName}.", e)
            throw e
        }
    } else {
        LOGGER.error(
            "Response channel not available request path: {}.",
            requestPath
        )
    }
}

private val LOGGER = logger<HttpServerExchange>()


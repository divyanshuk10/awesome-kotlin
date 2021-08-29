package link.kotlin.server.handlers

import io.undertow.server.HttpHandler
import io.undertow.server.HttpServerExchange
import io.undertow.util.SameThreadExecutor
import kotlinx.coroutines.*
import link.kotlin.server.utils.REVIEW
import link.kotlin.server.utils.logger
import java.lang.Runnable
import kotlin.coroutines.CoroutineContext

interface CoroutinesHttpHandler {
    suspend fun handleRequest(exchange: HttpServerExchange)
}

class CoroutinesHttpHandlerAdapter(
    private val handler: CoroutinesHttpHandler,
    private val context: CoroutineContext,
) : HttpHandler {
    override fun handleRequest(exchange: HttpServerExchange) {
        if (exchange.isDispatched) {
            LOGGER.info(REVIEW, "Exchange already dispatched.")
            CoroutineScope(context).launch {
                handler.handleRequest(exchange)
            }
        } else {
            exchange.dispatch(SameThreadExecutor.INSTANCE, Runnable {
                CoroutineScope(context).launch {
                    handler.handleRequest(exchange)
                }
            })
        }
    }

    private companion object {
        private val LOGGER = logger<CoroutinesHttpHandlerAdapter>()
    }
}

fun CoroutinesHttpHandler.toUndertow(
    context: CoroutineContext = Dispatchers.Default,
): HttpHandler {
    return CoroutinesHttpHandlerAdapter(
        context = context,
        handler = this
    )
}

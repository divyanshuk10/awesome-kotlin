package link.kotlin.server.handlers

import io.undertow.server.HandlerWrapper
import io.undertow.server.HttpHandler
import io.undertow.server.handlers.AttachmentHandler
import io.undertow.util.AttachmentKey

class AttachmentHandlerWrapper<T>(
    private val key: AttachmentKey<T>,
    private val instance: T,
) : HandlerWrapper {
    override fun wrap(handler: HttpHandler): HttpHandler {
        return AttachmentHandler(
            key,
            handler,
            instance
        )
    }
}

/*
 * Copyright 2014-2019 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.websocket

import io.ktor.application.*
import io.ktor.http.content.*
import io.ktor.http.*
import io.ktor.http.cio.websocket.*
import io.ktor.http.websocket.*
import io.ktor.request.*
import kotlinx.coroutines.*
import io.ktor.utils.io.*
import kotlin.coroutines.*

/**
 * An [OutgoingContent] response object that could be used to `respond()`: it will cause application engine to
 * perform HTTP upgrade and start websocket RAW session.
 *
 * Please note that you generally shouldn't use this object directly but use [WebSockets] feature with routing builders
 * [webSocket] instead.
 *
 * [handle] function is applied to a session and as far as it is a RAW session, you should handle all low-level
 * frames yourself and deal with ping/pongs, timeouts, close frames, frame fragmentation and so on.
 *
 * @param call that is starting web socket session
 * @param protocol web socket negotiated protocol name (optional)
 * @param handle function that is started once HTTP upgrade complete and the session will end once this function exit
 */
public class WebSocketUpgrade(
    public val call: ApplicationCall,
    public val protocol: String? = null,
    private val installExtensions: Boolean = false,
    public val handle: suspend WebSocketSession.() -> Unit
) : OutgoingContent.ProtocolUpgrade() {

    public constructor(
        call: ApplicationCall,
        protocol: String? = null,
        handle: suspend WebSocketSession.() -> Unit
    ) : this(call, protocol, installExtensions = false, handle)

    private val key = call.request.header(HttpHeaders.SecWebSocketKey)
    private val feature = call.application.feature(WebSockets)

    public val extensions: List<WebSocketExtension<*>> = if (installExtensions) {
        feature.extensions.map { it() }
    } else {
        emptyList()
    }

    override val headers: Headers
        get() = Headers.build {
            append(HttpHeaders.Upgrade, "websocket")
            append(HttpHeaders.Connection, "Upgrade")
            if (key != null) {
                append(HttpHeaders.SecWebSocketAccept, websocketServerAccept(key))
            }
            if (protocol != null) {
                append(HttpHeaders.SecWebSocketProtocol, protocol)
            }

            writeExtensions()
        }

    init {
        call.attributes.put(WebSockets.EXTENSIONS_KEY, extensions)
    }

    override suspend fun upgrade(
        input: ByteReadChannel,
        output: ByteWriteChannel,
        engineContext: CoroutineContext,
        userContext: CoroutineContext
    ): Job {
        val webSocket = RawWebSocket(
            input, output,
            feature.maxFrameSize, feature.masking,
            coroutineContext = engineContext + (coroutineContext[Job] ?: EmptyCoroutineContext)
        )

        webSocket.launch(WebSocketHandlerCoroutineName) {
            try {
                webSocket.start(handle)
            } catch (cause: Throwable) {
            }
        }

        return webSocket.coroutineContext[Job]!!
    }

    private fun HeadersBuilder.writeExtensions() {
        if (!installExtensions) return

        val requestedExtensions = call.request.header(HttpHeaders.SecWebSocketExtensions)
            ?.let { parseWebSocketExtensions(it) } ?: emptyList()

        val extensions: List<WebSocketExtensionHeader> = feature.extensions.flatMap {
            it().serverNegotiation(requestedExtensions)
        }

        if (extensions.isNotEmpty()) {
            append(HttpHeaders.SecWebSocketExtensions, extensions.joinToString(";"))
        }
    }

    public companion object {
        private val WebSocketHandlerCoroutineName = CoroutineName("raw-ws-handler")
    }
}

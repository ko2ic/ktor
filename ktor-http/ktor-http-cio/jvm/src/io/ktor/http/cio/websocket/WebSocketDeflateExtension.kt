/*
 * Copyright 2014-2020 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.http.cio.websocket

import io.ktor.util.*
import kotlinx.coroutines.*

private const val SERVER_MAX_WINDOW_BITS: String = "server_max_window_bits"
private const val CLIENT_NO_CONTEXT_TAKEOVER = "client_no_context_takeover"
private const val SERVER_NO_CONTEXT_TAKEOVER = "server_no_context_takeover"
private const val CLIENT_MAX_WINDOW_BITS = "client_max_window_bits"
private const val PERMESSAGE_DEFLATE = "permessage-deflate"
private const val MAX_WINDOW_BITS = 15
private const val MIN_WINDOW_BITS = 15

public class WebSocketDeflateExtension(
    private val config: Config
) : WebSocketExtension<WebSocketDeflateExtension.Config> {
    override val factory: WebSocketExtensionFactory<Config, out WebSocketExtension<Config>> = WebSocketDeflateExtension

    override val protocols: List<String> = config.build()

    private var outgoingContextTakeover: Boolean = true
    private var incomingContextTakeoverHint: Boolean = true

    private var incomingMaxWindowBits: Int = 0
    private var outgoingMaxWindowBits: Int = config.serverMaxWindowBits ?: MAX_WINDOW_BITS

    override fun clientNegotiation(negotiatedProtocols: List<WebSocketExtensionHeader>): Boolean {
        val protocol = negotiatedProtocols.find { it.name == PERMESSAGE_DEFLATE } ?: return false
        TODO()
    }

    override fun serverNegotiation(requestedProtocols: List<WebSocketExtensionHeader>): List<WebSocketExtensionHeader> {
        val protocol = requestedProtocols.find { it.name == PERMESSAGE_DEFLATE } ?: return emptyList()

        val parameters = mutableListOf<String>()
        for ((key, value) in protocol.parseParameters()) {
            when (key.toLowerCase()) {
                SERVER_MAX_WINDOW_BITS -> {
                    val bitsValue = value.toInt().also { check(it in MIN_WINDOW_BITS..MAX_WINDOW_BITS) }
                    outgoingMaxWindowBits = bitsValue.coerceAtMost(outgoingMaxWindowBits)
                }
                CLIENT_MAX_WINDOW_BITS -> TODO()
                SERVER_NO_CONTEXT_TAKEOVER -> {
                    check(value.isBlank())

                    outgoingContextTakeover = false
                    parameters.add(SERVER_NO_CONTEXT_TAKEOVER)
                    /* Client prevents the peer server from using context takeover */
                }
                CLIENT_NO_CONTEXT_TAKEOVER -> {
                    check(value.isBlank())

                    incomingContextTakeoverHint = false
                    parameters.add(CLIENT_NO_CONTEXT_TAKEOVER)
                    /* Hint that client will not use context takeover */
                }
                else -> error("Unsupported extension parameter: ($key, $value)")
            }
        }

        parameters.add("$SERVER_MAX_WINDOW_BITS=$outgoingContextTakeover")

        return listOf(WebSocketExtensionHeader(PERMESSAGE_DEFLATE, parameters))
    }

    override fun processOutgoingFrame(frame: Frame): Frame {
        TODO("Not yet implemented")
    }

    override fun processIncomingFrame(frame: Frame): Frame {
        TODO("Not yet implemented")
    }

    public class Config {
        public var clientNoContextTakeOver: Boolean = false

        public var serverNoContextTakeOver: Boolean = false

        public var clientMaxWindowBits: Int? = null
            set(value) {
                require(value == null || value in MIN_WINDOW_BITS..MAX_WINDOW_BITS) {
                    "Client max window bits should be in $MIN_WINDOW_BITS..$MAX_WINDOW_BITS. Current value: $value"
                }

                field = value
            }

        public var serverMaxWindowBits: Int? = null
            set(value) {
                require(value == null || value in MIN_WINDOW_BITS..MAX_WINDOW_BITS) {
                    "Client max window bits should be in $MIN_WINDOW_BITS..$MAX_WINDOW_BITS. Current value: $value"
                }

                field = value
            }

        internal var manualConfig: (MutableList<String>) -> Unit = {}

        public fun configureProtocols(block: (protocols: MutableList<String>) -> Unit) {
            manualConfig = {
                manualConfig(it)
                block(it)
            }
        }

        internal fun build(): List<String> {
            val result = mutableListOf<String>()

            result += "per-message-deflate;"

            if (clientNoContextTakeOver) {
                result += "$CLIENT_NO_CONTEXT_TAKEOVER;"
            }

            if (serverNoContextTakeOver) {
                result += "$SERVER_NO_CONTEXT_TAKEOVER;"
            }

            result += if (clientMaxWindowBits != null) {
                "$CLIENT_MAX_WINDOW_BITS=${clientMaxWindowBits};"
            } else {
                "$CLIENT_MAX_WINDOW_BITS;"
            }

            result += if (serverMaxWindowBits != null) {
                "$SERVER_MAX_WINDOW_BITS=${serverMaxWindowBits};"
            } else {
                "$SERVER_MAX_WINDOW_BITS;"
            }

            manualConfig(result)

            return result
        }
    }

    public companion object : WebSocketExtensionFactory<Config, WebSocketDeflateExtension> {
        override val key: AttributeKey<WebSocketDeflateExtension>  = AttributeKey("WebsocketDeflateExtension")
        override val rcv1: Boolean = true
        override val rcv2: Boolean = false
        override val rcv3: Boolean = false

        override fun install(config: Config.() -> Unit): WebSocketDeflateExtension =
            WebSocketDeflateExtension(Config().apply(config))
    }
}

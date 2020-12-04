/*
 * Copyright 2014-2020 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.http.cio.websocket

import io.ktor.util.*

private const val SERVER_MAX_WINDOW_BITS: String = "server_max_window_bits"
private const val CLIENT_NO_CONTEXT_TAKEOVER = "client_no_context_takeover"
private const val SERVER_NO_CONTEXT_TAKEOVER = "server_no_context_takeover"
private const val CLIENT_MAX_WINDOW_BITS = "client_max_window_bits"

public class WebSocketDeflateExtension(
    private val config: Config
) : WebSocketExtension<WebSocketDeflateExtension.Config> {
    override val factory: WebSocketExtensionFactory<Config, out WebSocketExtension<Config>> = WebSocketDeflateExtension

    override val protocols: List<String> = config.build()

    override fun clientNegotiation(negotiatedProtocols: List<String>): Boolean {
        /**
         * 1. Find per-message-deflate, read parameters until `,`
         * 2. Parse parameters and check if they match the config
         */
        TODO()
    }

    override fun serverNegotiation(requestedProtocols: List<String>): List<String> {
        /**
         * 1. Find `per-message-deflate`, read parameters until `,`
         */
        TODO("Not yet implemented")
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
                require(value == null || value in 8..15) {
                    "Client max window bits should be in 8..15. Current value: $value"
                }

                field = value
            }

        public var serverMaxWindowBits: Int? = null
            set(value) {
                require(value == null || value in 8..15) {
                    "Client max window bits should be in 8..15. Current value: $value"
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

/*
 * Copyright 2014-2020 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.client.tests.utils

import io.ktor.client.features.logging.*
import io.ktor.http.cio.websocket.*
import io.ktor.util.*

class FrameLogger(val logger: Logger) : WebSocketExtension<FrameLogger.Config> {
    override val factory: WebSocketExtensionFactory<Config, FrameLogger> = FrameLogger

    override val protocols: List<WebSocketExtensionProtocol> = emptyList()

    override fun clientNegotiation(negotiatedProtocols: List<WebSocketExtensionProtocol>): Boolean {
        logger.log("Client negotiation")
        return true
    }

    override fun serverNegotiation(requestedProtocols: List<WebSocketExtensionProtocol>): List<WebSocketExtensionProtocol> {
        logger.log("Server negotiation")
        return emptyList()
    }

    override fun processOutgoingFrame(frame: Frame): Frame {
        logger.log("Process outgoing frame: $frame")
        return frame
    }

    override fun processIncomingFrame(frame: Frame): Frame {
        logger.log("Process incoming frame: $frame")
        return frame
    }

    class Config {
        lateinit var logger: Logger
    }

    companion object : WebSocketExtensionFactory<Config, FrameLogger> {
        override val key: AttributeKey<FrameLogger> = AttributeKey("frame-logger")

        override val rcv1: Boolean = false
        override val rcv2: Boolean = false
        override val rcv3: Boolean = false

        override fun install(config: Config.() -> Unit): FrameLogger {
            return FrameLogger(Config().apply(config).logger)
        }
    }
}

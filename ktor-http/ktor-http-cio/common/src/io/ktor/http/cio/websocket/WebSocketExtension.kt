/*
 * Copyright 2014-2020 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.http.cio.websocket

import io.ktor.util.*

public typealias ExtensionInstaller = () -> WebSocketExtension<*>

public interface WebSocketExtensionFactory<ConfigType: Any, ExtensionType: WebSocketExtension<ConfigType>> {
    public val key: AttributeKey<ExtensionType>

    public val rcv1: Boolean

    public val rcv2: Boolean

    public val rcv3: Boolean

    public fun install(config: ConfigType.() -> Unit): ExtensionType
}

public interface WebSocketExtension<ConfigType : Any> {
    public val factory: WebSocketExtensionFactory<ConfigType, out WebSocketExtension<ConfigType>>

    public val protocols: List<String>

    public fun clientNegotiation(negotiatedProtocols: List<String>): Boolean

    public fun serverNegotiation(
        requestedProtocols: List<String>
    ): List<String>

    public fun processOutgoingFrame(frame: Frame): Frame

    public fun processIncomingFrame(frame: Frame): Frame
}

public class ExtensionsConfig {
    public val installers: MutableList<ExtensionInstaller> = mutableListOf()
    private val rcv: Array<Boolean> = arrayOf(false, false, false)

    public fun <ConfigType : Any> install(
        extension: WebSocketExtensionFactory<ConfigType, *>,
        config: ConfigType.() -> Unit
    ) {
        checkConflicts(extension)
        installers += { extension.install(config) }
    }

    private fun checkConflicts(extensionFactory: WebSocketExtensionFactory<*, *>) {
        var hasConflict = extensionFactory.rcv1 && rcv[1]
        hasConflict = hasConflict || extensionFactory.rcv2 && rcv[2]
        hasConflict = hasConflict || extensionFactory.rcv3 && rcv[3]

        check(!hasConflict) { "Failed to install extension. Please check configured extensions for conflicts." }
    }
}

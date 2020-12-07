/*
 * Copyright 2014-2020 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.http.cio.websocket

public class WebSocketExtensionHeader(public val name: String, public val parameters: List<String>) {
    public fun parseParameters(): Iterable<Pair<String, String>> {
        TODO()
    }
}

public fun parseWebSocketExtensions(value: String): List<WebSocketExtensionHeader> {
    TODO()
}

public fun List<WebSocketExtensionHeader>.writeHeaderValue(): String {
    return joinToString(";")
}

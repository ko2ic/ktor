/*
 * Copyright 2014-2020 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.server.servlet

import io.ktor.application.*
import io.ktor.server.engine.*
import io.ktor.util.*
import javax.servlet.*

/**
 * Provides javax.servlet request attributes or fail it the underlying engine is not
 * servlet-backed.
 */
public val ApplicationCall.servletRequestAttributes: Map<String, Any>
    get() = attributes[servletRequestAttributesKey]

/**
 * A key for call attribute containing java.servlet attributes.
 */
internal val servletRequestAttributesKey: AttributeKey<Map<String, Any>> = AttributeKey("ServletRequestAttributes")

@EngineAPI
public fun ServletRequest.putServletAttributesTo(call: ApplicationCall) {
    val servletAttributes = attributeNames?.asSequence()?.associateWith { attributeName ->
        getAttribute(attributeName)
    }?.filterValues { it != null } ?: emptyMap()

    call.attributes.put(servletRequestAttributesKey, servletAttributes)
}

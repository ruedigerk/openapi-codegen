package io.github.ruedigerk.contractfirst.generator.client.internal

import java.lang.reflect.Type

/**
 * Represents the definition of a response to an API operation. Every combination of status code and content type for an API operation is represented by a
 * different ResponseDefinition.
 */
data class ResponseDefinition(val statusCode: StatusCode, val contentType: String?, val javaType: Type) {

  fun hasNoContent(): Boolean {
    return contentType == null
  }
}

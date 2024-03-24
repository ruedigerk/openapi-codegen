package io.github.ruedigerk.contractfirst.generator.client.internal

/**
 * Represents the definition of the request body of an API operation, and the entity that is transferred in it.
 */
data class OperationRequestBody(val contentType: String?, val isRequired: Boolean, val entity: Any?)

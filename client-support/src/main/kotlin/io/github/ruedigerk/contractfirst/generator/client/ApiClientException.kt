package io.github.ruedigerk.contractfirst.generator.client

/**
 * Abstract superclass of all exceptions that are thrown by the API client.
 */
abstract class ApiClientException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)

package io.github.ruedigerk.contractfirst.generator.client

/**
 * Thrown by the API client when a required request parameter or a required request body is missing.
 */
class ApiClientValidationException(message: String) : ApiClientException(message)

package io.github.ruedigerk.contractfirst.generator.client.internal

/**
 * Represents the definition of a request body that is either a multipart body, or an application/x-www-form-urlencoded body.
 */
internal data class MultipartRequestBody(val bodyParts: List<BodyPart>)

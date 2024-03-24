package io.github.ruedigerk.contractfirst.generator.client.internal

import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import java.lang.reflect.Type

/**
 * Represents the definition of an API operation, and the data that is transferred within it.
 */
class Operation private constructor(builder: Builder) {

  val path: String = builder.path
  val method: String = builder.method
  val parameters: List<Parameter> = builder.parameters.toList()
  val requestBody: OperationRequestBody = builder.requestBody

  val pathParameters: Map<String, Parameter>
    get() = extractParameters(ParameterLocation.PATH, parameters)

  val queryParameters: Map<String, Parameter>
    get() = extractParameters(ParameterLocation.QUERY, parameters)

  val headerParameters: Map<String, Parameter>
    get() = extractParameters(ParameterLocation.HEADER, parameters)

  private val responseDefinitions: Map<StatusCode, List<ResponseDefinition>> = builder.responseDefinitions.groupBy { it.statusCode }

  /**
   * Determines the accept-header value for this operation. All JSON-compatible mime types are sent with a q-factor of 1 and all other mime types with a
   * q-factor of 0.5.
   *
   * See: https://developer.mozilla.org/en-US/docs/Glossary/Quality_values
   * See: https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Accept
   * See: https://developer.mozilla.org/en-US/docs/Web/HTTP/Content_negotiation
   */
  fun determineAcceptHeaderValue(): String {
    val (jsonMediaTypes, nonJsonMediaTypes) = allAcceptedMediaTypes.partition(MediaTypes::isJsonMediaType)
    return (jsonMediaTypes + nonJsonMediaTypes.map { "$it; q=0.5" }).joinToString(", ")
  }

  private val allAcceptedMediaTypes: List<String>
    get() = responseDefinitions.values
        .flatMap { responseDefinitions -> responseDefinitions.map { it.contentType } }
        .filterNotNull()
        .distinct()

  /**
   * Returns the Java type of the response definition that matches the servers returned status code and content type. If no response definition is matching,
   * null is returned. If the matching response is defined to have no content, type [Void.TYPE] is returned.
   */
  fun determineMatchingResponseType(statusCode: Int, contentType: String?): Type? {
    val responseDefinitions = selectResponseDefinitionsForStatusCode(statusCode)

    // The status code is not defined in the contract
    if (responseDefinitions.isEmpty()) {
      return null
    }

    if (responseDefinitions.all { it.hasNoContent() }) {
      return Void.TYPE
    }

    if (contentType == null) {
      return if (responseDefinitions.any { it.hasNoContent() }) {
        Void.TYPE
      } else {
        null
      }
    }

    val mediaType = contentType.toMediaTypeOrNull()

    for (definition in responseDefinitions) {
      if (definition.contentType != null && isCompatibleMediaType(mediaType, definition.contentType.toMediaTypeOrNull())) {
        return definition.javaType
      }
    }

    // No matching definition found. As a special case, if the server sends a JSON content type and there is only a single response definition for this status
    // code in the contract, try to deserialize the response as a JSON entity. This quirk is added, because there seem to be a lot of contracts in the wild that
    // erroneously declare some none-JSON content type in the contract but actually send JSON encoded response entities.
    if (MediaTypes.isJsonMediaType(mediaType) && responseDefinitions.size == 1) {
      return responseDefinitions.first().javaType
    }

    return null
  }

  private fun selectResponseDefinitionsForStatusCode(statusCode: Int): List<ResponseDefinition> {
    val responses = responseDefinitions[StatusCode.of(statusCode)]
    return responses ?: responseDefinitions[StatusCode.DEFAULT] ?: emptyList()
  }

  private fun isCompatibleMediaType(testedMediaType: MediaType?, mediaTypeToMatchAgainst: MediaType?): Boolean {
    if (mediaTypeToMatchAgainst == null) {
      return false
    }
    if (mediaTypeToMatchAgainst.type == "*") {
      return true
    }
    if (testedMediaType == null) {
      return false
    }

    val sameType = mediaTypeToMatchAgainst.type == testedMediaType.type
    return sameType && (mediaTypeToMatchAgainst.subtype == "*" || mediaTypeToMatchAgainst.subtype == testedMediaType.subtype)
  }

  private fun extractParameters(location: ParameterLocation, parameters: List<Parameter>): Map<String, Parameter> {
    return parameters
        .filter { it.location == location }
        .associateBy { it.name }
  }

  /**
   * Builder for instances of class Operation.
   */
  class Builder(val path: String, val method: String) {

    val parameters: MutableList<Parameter> = ArrayList()
    val responseDefinitions: MutableList<ResponseDefinition> = ArrayList()

    private val bodyParts: MutableList<BodyPart> = ArrayList()

    // Default is "no body", indicated by contentType null
    var requestBody: OperationRequestBody = OperationRequestBody(null, false, null)

    /**
     * Adds a parameter definition to this operation.
     */
    fun parameter(name: String, location: ParameterLocation, required: Boolean, value: Any?) {
      parameters.add(Parameter(name, location, required, value))
    }

    /**
     * Defines the request body for this operation.
     */
    fun requestBody(contentType: String?, required: Boolean, entity: Any?) {
      requestBody = OperationRequestBody(contentType, required, entity)
    }

    /**
     * Defines a form field of an x-www-form-urlencoded request body, or a part of a multipart request body.
     *
     * BACKWARDS_COMPATIBILITY(1.7): This method exists only for backwards compatibility with version 1.7 of the generator.
     */
    fun requestBodyPart(name: String, value: Any) {
      bodyParts.add(BodyPart(null, name, value))
    }

    /**
     * Defines a form field of an x-www-form-urlencoded request body, or a part of a multipart request body.
     */
    fun requestBodyPart(type: BodyPart.Type?, name: String, value: Any) {
      bodyParts.add(BodyPart(type, name, value))
    }

    /**
     * Defines the request body as a multipart, or an application/x-www-form-urlencoded body.
     */
    fun multipartRequestBody(contentType: String?) {
      requestBody = OperationRequestBody(contentType, false, MultipartRequestBody(bodyParts))
    }

    /**
     * Adds a response definition without content/body.
     */
    fun response(statusCode: StatusCode) {
      responseDefinitions.add(ResponseDefinition(statusCode, null, Void.TYPE))
    }

    /**
     * Adds a response definition with content of the specified content type and Java type.
     */
    fun response(statusCode: StatusCode, contentType: String?, javaType: Type) {
      responseDefinitions.add(ResponseDefinition(statusCode, contentType, javaType))
    }

    /**
     * Build an operation from this builder.
     */
    fun build(): Operation {
      return Operation(this)
    }
  }
}

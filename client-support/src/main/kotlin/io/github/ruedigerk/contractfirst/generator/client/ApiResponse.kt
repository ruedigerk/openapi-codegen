package io.github.ruedigerk.contractfirst.generator.client

import java.lang.reflect.Type

/**
 * Represents a response that is conforming to the API specification of the operation called, regardless of the response being successful.
 */
data class ApiResponse(
    
    /**
     * The request for this response.
     */
    val request: ApiRequest,
    
    /**
     * The HTTP staus code of this response.
     */
    val statusCode: Int,
    
    /**
     * The HTTP status message of the response sent by the server.
     */
    val httpStatusMessage: String,
    
    /**
     * The Content-Type header of the response or null, if none was sent.
     */
    val contentType: String?,
    
    /**
     * The headers of the response.
     */
    val headers: List<Header>,
    
    /**
     * The parsed response entity or null, when the response body is empty.
     */
    val entity: Any?,
    
    /**
     * The Java type of the response entity, or `Void.TYPE` when the response contains no entity. This is usually the class of the entity, 
     * e.g., BigDecimal.class, but can also be a TypeToken, when the type is generic, e.g., `new TypeToken<List<BigDecimal>>(){}.getType()`.
     */
    val entityType: Type
) {

  /**
   * Whether the response has a status code in the range 200 to 299.
   */
  val isSuccessful: Boolean
    get() = statusCode in 200..299

  override fun toString(): String {
    return "ApiResponse(" +
        "request=" + request +
        ",status=" + statusCode + " " + httpStatusMessage +
        ",contentType='" + contentType + '\'' +
        ",headers=" + headers +
        ",javaType=" + entityType +
        ",entity=" + entity +
        ')'
  }
}

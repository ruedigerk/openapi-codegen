package io.github.ruedigerk.contractfirst.generator.client

/**
 * Represents a response that is not conforming to the API specification of the operation called. Also used for responses that the API client can not process,
 * e.g., when the server returns an entity in an unsupported format, like XML.
 */
data class IncompatibleResponse(

    /**
     * Returns the request for this response.
     */
    val request: ApiRequest,

    /**
     * Return the HTTP staus code of this response.
     */
    val statusCode: Int,

    /**
     * Returns the HTTP status message of the response sent by the server.
     */
    val httpStatusMessage: String,

    /**
     * Returns the Content-Type header of the response or null, if none was sent.
     */
    val contentType: String?,

    /**
     * Returns the headers of the response.
     */
    val headers: List<Header>,

    /**
     * The content of the response as a String.
     */
    val body: String
) {

  /**
   * Returns whether the response has a status code in the range 200 to 299.
   */
  val isSuccessful: Boolean
    get() = statusCode in 200..299

  override fun toString(): String {
    return "IncompatibleResponse(" +
        "request=" + request +
        ",status=" + statusCode + " " + httpStatusMessage +
        ",contentType='" + contentType + '\'' +
        ",headers=" + headers +
        ",body='" + body + '\'' +
        ')'
  }
}

package io.github.ruedigerk.contractfirst.generator.client

/**
 * Represents an incomplete response, where an IOException occurred reading the response body.
 */
data class IncompleteResponse(
    
    /**
     * Returns a description of the request for this response.
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
    val headers: List<Header>
) {

  override fun toString(): String {
    return "IncompleteResponse(" +
        "request=" + request +
        ",status=" + statusCode + " " + httpStatusMessage +
        ",contentType='" + contentType + '\'' +
        ",headers=" + headers +
        ')'
  }
}

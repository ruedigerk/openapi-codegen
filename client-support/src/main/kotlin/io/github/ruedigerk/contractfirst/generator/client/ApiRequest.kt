package io.github.ruedigerk.contractfirst.generator.client

/**
 * Represents a request sent by the API client. Does not contain the request body.
 */
data class ApiRequest(
    
    /**
     * The URL of the request.
     */
    val url: String,
    
    /**
     * The HTTP method of the request, e.g., "GET".
     */
    val method: String?,
    
    /**
     * The headers of the HTTP request.
     */
    val headers: List<Header>
) {

  override fun toString(): String {
    return "ApiRequest($method $url,headers=$headers)"
  }
}

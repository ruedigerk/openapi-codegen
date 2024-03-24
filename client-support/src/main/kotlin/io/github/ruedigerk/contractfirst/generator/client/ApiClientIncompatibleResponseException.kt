package io.github.ruedigerk.contractfirst.generator.client

/**
 * Thrown by the API client when the server sends a response that is not conforming to the API specification of the operation called.
 */
class ApiClientIncompatibleResponseException(
    
    msg: String,

    /**
     * The response sent by the server that was not conforming to the API specification.
     */
    val response: IncompatibleResponse,

    cause: Throwable? = null
) : ApiClientException(toMessage(msg, response), cause) {

  companion object {

    private fun toMessage(msg: String, response: IncompatibleResponse): String {
      val request = response.request
      return msg + ", for " + request.method + " " + request.url +
          ", status=" + response.statusCode + " " + response.httpStatusMessage +
          ", content-type=" + response.contentType +
          ", body=" + response.body
    }
  }
}

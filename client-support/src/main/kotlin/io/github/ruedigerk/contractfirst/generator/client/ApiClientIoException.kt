package io.github.ruedigerk.contractfirst.generator.client

import java.io.IOException

/**
 * Thrown when an IOException occurs during a request of the API client.
 */
class ApiClientIoException : ApiClientException {

  /**
   * The description of the request that lead to the IOException.
   */
  val request: ApiRequest?
  
  /**
   * The incomplete response from the server, if available.
   */
  @Suppress("MemberVisibilityCanBePrivate")
  val incompleteResponse: IncompleteResponse?

  constructor(message: String, request: ApiRequest, cause: IOException?) : super(toMessage(message, request), cause) {
    this.request = request
    incompleteResponse = null
  }

  constructor(message: String, incompleteResponse: IncompleteResponse, cause: IOException?) : super(toMessage(message, incompleteResponse), cause) {
    request = incompleteResponse.request
    this.incompleteResponse = incompleteResponse
  }

  private companion object {

    private fun toMessage(message: String, request: ApiRequest): String {
      return message + ", for: " + request.method + " " + request.url
    }

    private fun toMessage(message: String, incompleteResponse: IncompleteResponse): String {
      val request = incompleteResponse.request
      return message +
          ", for " + request.method + " " + request.url +
          ", status=" + incompleteResponse.statusCode + " " + incompleteResponse.httpStatusMessage
    }
  }
}

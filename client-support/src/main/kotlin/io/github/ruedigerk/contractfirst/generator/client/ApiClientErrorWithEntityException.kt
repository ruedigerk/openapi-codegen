package io.github.ruedigerk.contractfirst.generator.client

/**
 * Abstract superclass of all exceptions thrown by the API client that indicate an error defined in the contract. Assumes that the error is completely described
 * by the status code and the entity returned by the server.
 */
abstract class ApiClientErrorWithEntityException protected constructor(
    
    /**
     * The response sent by the server.
     */
    val response: ApiResponse
) : ApiClientException(toMessage(response)) {

  /**
   * Returns the request for this response.
   */
  val request: ApiRequest
    get() = response.request

  /**
   * The HTTP status code sent by the server.
   */
  val statusCode: Int
    get() = response.statusCode

  /**
   * The entity sent by the server.
   */
  open val entity: Any?
    get() = response.entity

  private companion object {

    private fun toMessage(response: ApiResponse): String {
      val request = response.request
      return "Error with entity for " + request.method + " " + request.url +
          ", status=" + response.statusCode + " " + response.httpStatusMessage +
          ", entity=" + response.entity
    }
  }
}

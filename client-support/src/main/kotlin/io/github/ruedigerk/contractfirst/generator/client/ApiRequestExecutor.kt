package io.github.ruedigerk.contractfirst.generator.client

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonParseException
import io.github.ruedigerk.contractfirst.generator.client.internal.*
import io.github.ruedigerk.contractfirst.generator.support.gson.LocalDateGsonTypeAdapter
import io.github.ruedigerk.contractfirst.generator.support.gson.OffsetDateTimeGsonTypeAdapter
import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.internal.http.HttpMethod.requiresRequestBody
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.lang.reflect.Type
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.*

/**
 * Performs HTTP requests as defined by generated client code. Instances are thread-safe and can be shared across multiple instances of generated ApiClients.
 *
 * @param httpClient the OkHttp-Client instance to use for sending HTTP requests.
 * @param baseUrl    the base URL to send requests to.
 */
class ApiRequestExecutor(httpClient: OkHttpClient, baseUrl: String) {

  /**
   * Returns the Gson instance used to serialize and deserialize JSON entities.
   */
  val gson: Gson

  /**
   * Returns the OkHttpClient instance used to send requests.
   */
  @Suppress("MemberVisibilityCanBePrivate")
  val httpClient: OkHttpClient

  /**
   * Returns the base URL used to send requests.
   */
  @Suppress("MemberVisibilityCanBePrivate")
  val baseUrl: String

  init {
    this.httpClient = addInternalInterceptors(httpClient)
    this.baseUrl = removeTrailingSlash(baseUrl)

    gson = createGson()
  }

  private fun addInternalInterceptors(httpClient: OkHttpClient): OkHttpClient {
    return httpClient.newBuilder()
        .addNetworkInterceptor(RequestAccessInterceptor())
        .build()
  }

  private fun removeTrailingSlash(baseUrl: String): String {
    return if (baseUrl.endsWith("/")) baseUrl.substring(0, baseUrl.length - 1) else baseUrl
  }

  private fun createGson(): Gson {
    return GsonBuilder()
        .registerTypeAdapter(LocalDate::class.java, LocalDateGsonTypeAdapter())
        .registerTypeAdapter(OffsetDateTime::class.java, OffsetDateTimeGsonTypeAdapter())
        .create()
  }

  @Throws(ApiClientIoException::class, ApiClientValidationException::class, ApiClientIncompatibleResponseException::class)
  fun executeRequest(operation: Operation): ApiResponse {
    validateOperation(operation)

    val request = createRequest(operation)
    val requestAndResponse = executeHttpRequest(request)

    return interpretResponse(requestAndResponse, operation)
  }

  @Throws(ApiClientValidationException::class)
  private fun validateOperation(operation: Operation) {
    if (operation.requestBody.isRequired && operation.requestBody.entity == null) {
      throw ApiClientValidationException("Request body is required but missing")
    }

    for (parameter in operation.parameters) {
      if (parameter.isRequired && parameter.value == null) {
        throw ApiClientValidationException("Parameter $parameter is required but missing")
      }
    }
  }

  private fun executeHttpRequest(request: Request): RequestAndResponse {
    try {
      val response = httpClient.newCall(request).execute()
      // RequestAccessInterceptor.getLastRequest() is used instead of response.request() because the latter is missing all headers that OkHttp is adding
      // late in the request processing, like Content-Type, Content-Size, etc.
      // In case the call is executed successfully, RequestAccessInterceptor.lastRequest cannot be null.
      return RequestAndResponse(RequestAccessInterceptor.lastRequest!!, response)
    } catch (e: IOException) {
      // Special case for extremely short request timeouts, where the request times out even before the RequestAccessInterceptor was called. In this case
      // RequestAccessInterceptor.lastRequest can be null.
      val detailedRequest = RequestAccessInterceptor.lastRequest ?: request
      val apiRequest = toApiRequest(detailedRequest)
      throw ApiClientIoException("Error executing request: $e", apiRequest, e)
    } finally {
      RequestAccessInterceptor.clearThreadLocal()
    }
  }

  private fun toApiRequest(request: Request): ApiRequest {
    return ApiRequest(request.url.toString(), request.method, request.headers.toList())
  }

  @Throws(ApiClientIoException::class)
  fun createRequest(operation: Operation): Request {
    val url = determineRequestUrl(operation)
    val headers = determineRequestHeaders(operation)

    try {
      val requestBody = serializeRequestBody(operation)

      return Request.Builder()
          .url(url)
          .method(operation.method, requestBody)
          .headers(headers)
          .build()
    } catch (e: IOException) {
      val apiRequest = ApiRequest(url.toString(), operation.method, headers.toList())
      throw ApiClientIoException("Error serializing request body: $e", apiRequest, e)
    }
  }

  private fun determineRequestUrl(operation: Operation): HttpUrl {
    val urlBuilder = baseUrl.toHttpUrl().newBuilder()

    addRequestPath(urlBuilder, operation)
    addQueryParameters(urlBuilder, operation.queryParameters)

    return urlBuilder.build()
  }

  private fun addRequestPath(urlBuilder: HttpUrl.Builder, operation: Operation) {
    val path = operation.path
    val pathSegments = path.split("/".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()

    for (segment in pathSegments) {
      var resolvedSegment = segment

      if (segment.startsWith("{") && segment.endsWith("}")) {
        val parameterName = segment.substring(1, segment.length - 1)
        val parameter = operation.pathParameters[parameterName]

        if (parameter != null) {
          resolvedSegment = ParameterSerialization.serializeSimpleStyleParameter(parameter.value)
        }
      }

      urlBuilder.addPathSegment(resolvedSegment)
    }
  }

  private fun addQueryParameters(urlBuilder: HttpUrl.Builder, queryParameters: Map<String, Parameter>) {
    ParameterSerialization.serializeFormStyleParameters(queryParameters) { name, value -> urlBuilder.addQueryParameter(name, value) }
  }

  // This method does not need to set a Content-Type header, as that is done by OkHttp when we set the media-type on the request body.
  private fun determineRequestHeaders(operation: Operation): Headers {
    val builder = Headers.Builder()

    // Split array-valued parameters to separate headers. This is not by the OpenAPI spec but works with JAX-RS out of the box.
    ParameterSerialization.serializeFormStyleParameters(operation.headerParameters) { name, value -> builder.add(name, value) }

    val acceptHeaderValue = operation.determineAcceptHeaderValue()
    if (acceptHeaderValue.isNotEmpty()) {
      builder.add("Accept", acceptHeaderValue)
    }

    return builder.build()
  }

  @Throws(IOException::class)
  private fun serializeRequestBody(operation: Operation): RequestBody? {
    val requestBody = operation.requestBody

    return when (requestBody.entity) {
      null -> if (requiresRequestBody(operation.method)) createEmptyRequestBody() else null

      is MultipartRequestBody -> {
        val bodyParts = requestBody.entity.bodyParts

        if (requestBody.contentType == "application/x-www-form-urlencoded") {
          createFormRequestBody(bodyParts)
        } else {
          createMultipartRequestBody(bodyParts)
        }
      }

      else -> createEntityRequestBody(requestBody)
    }
  }

  private fun createEmptyRequestBody(): RequestBody {
    return ByteArray(0).toRequestBody()
  }

  /**
   * Support for x-www-form-urlencoded request bodies is limited. Array-valued and complex form fields are not serialized as per the specification. See:
   * https://spec.openapis.org/oas/v3.0.3#support-for-x-www-form-urlencoded-request-bodies See: https://www.rfc-editor.org/rfc/rfc1866#section-8.2.1
   */
  private fun createFormRequestBody(bodyParts: List<BodyPart>): RequestBody {
    val builder = FormBody.Builder()

    for (part in bodyParts) {
      builder.add(part.name, ParameterSerialization.serializePrimitiveParameterValue(part.value))
    }

    return builder.build()
  }

  /**
   * See: https://spec.openapis.org/oas/v3.0.3#special-considerations-for-multipart-content
   */
  @Throws(IOException::class)
  private fun createMultipartRequestBody(bodyParts: List<BodyPart>): RequestBody {
    val builder = MultipartBody.Builder().setType(MultipartBody.FORM)

    for (part in bodyParts) {
      when (part.type) {
        // BACKWARDS_COMPATIBILITY(1.7) START
        null -> addLegacyBodyPart(builder, part)
        // BACKWARDS_COMPATIBILITY(1.7) END

        BodyPart.Type.ATTACHMENT -> addAttachmentBodyParts(builder, part)

        BodyPart.Type.COMPLEX -> {
          val content = gson.toJson(part.value)
          val body: RequestBody = content.toRequestBody("application/json".toMediaType())
          builder.addFormDataPart(part.name, null, body)
        }

        BodyPart.Type.PRIMITIVE -> builder.addFormDataPart(part.name, ParameterSerialization.serializePrimitiveParameterValue(part.value))
      }
    }

    return createOptionallyEmptyRequestBody(builder)
  }

  /**
   * As it's not easy to check if all body parts are to be omitted, always try to build the multipart body and fall back to an empty body, if the builder
   * throws an exception, indicating no body parts were added.
   */
  private fun createOptionallyEmptyRequestBody(builder: MultipartBody.Builder): RequestBody {
    return try {
      builder.build()
    } catch (ignored: IllegalStateException) {
      createEmptyRequestBody()
    }
  }

  /**
   * BACKWARDS_COMPATIBILITY(1.7): This method exists only for backwards compatibility with version 1.7 of the generator.
   */
  @Throws(IOException::class)
  private fun addLegacyBodyPart(builder: MultipartBody.Builder, part: BodyPart) {
    val headers: Headers = Headers.Builder().add("Content-Disposition", "form-data; name=\"" + part.name + "\"").build()

    when (part.value) {
      is ByteArray -> builder.addPart(headers, part.value.toRequestBody())
      is InputStream -> builder.addPart(headers, part.value.use { it.readBytes() }.toRequestBody())
      else -> builder.addPart(headers, ParameterSerialization.serializePrimitiveParameterValue(part.value).toRequestBody())
    }
  }

  @Throws(IOException::class)
  private fun addAttachmentBodyParts(builder: MultipartBody.Builder, part: BodyPart) {
    when (part.value) {
      is Attachment -> addAttachmentBodyPart(builder, part.name, part.value)

      is Collection<*> -> {
        @Suppress("UNCHECKED_CAST")
        val attachments = part.value as Collection<Attachment>
        for (attachment in attachments) {
          addAttachmentBodyPart(builder, part.name, attachment)
        }
      }

      else -> error("Body part value not of type Attachment or List<Attachment>: ${part.value.javaClass.name}")
    }
  }

  @Throws(IOException::class)
  private fun addAttachmentBodyPart(builder: MultipartBody.Builder, partName: String, attachment: Attachment) {
    val contentType: MediaType = attachment.mediaType.toMediaType()

    val body = when (attachment.content) {
      is ByteArray -> attachment.content.toRequestBody(contentType)

      is InputStream -> {
        val bytes = attachment.content.use { it.readBytes() }
        bytes.toRequestBody(contentType)
      }

      is File -> attachment.content.asRequestBody(contentType)

      else -> error("Unsupported attachment content type: ${attachment.content.javaClass.name}")
    }

    builder.addFormDataPart(partName, attachment.fileName, body)
  }

  @Throws(IOException::class)
  private fun createEntityRequestBody(requestBody: OperationRequestBody): RequestBody {
    val entity = requestBody.entity
    val mediaType = requestBody.contentType?.toMediaTypeOrNull()

    when {
      MediaTypes.isJsonMediaType(mediaType) -> {
        val content = gson.toJson(entity)
        return content.toRequestBody(mediaType)
      }

      entity is ByteArray -> {
        return entity.toRequestBody(mediaType)
      }

      entity is InputStream -> {
        val bytes = entity.use { it.readBytes() }
        return bytes.toRequestBody(mediaType)
      }

      else -> {
        // Not a JSON media type, so we assume it is text.
        val content = requestBody.toString()
        return content.toRequestBody(mediaType)
      }
    }
  }

  /**
   * Interprets the response from the server and returns the appropriate Response or throws an ApiClientIoException in case of an IOException.
   *
   * Note: this method closes the response body unless it returns an InputStream of the body!
   *
   * @throws ApiClientIoException                   when an IOException occurs reading the response.
   * @throws ApiClientIncompatibleResponseException when the response is not conforming to the specification of the API.
   */
  @Throws(ApiClientIoException::class, ApiClientIncompatibleResponseException::class)
  private fun interpretResponse(requestAndResponse: RequestAndResponse, operation: Operation): ApiResponse {
    val request = requestAndResponse.request
    val response = requestAndResponse.response
    val responseBody = checkNotNull(response.body)

    val statusCode = response.code
    val mediaType = response.header(CONTENT_TYPE_HEADER)

    val apiRequest = toApiRequest(request)
    val responseBuilder = ResponseBuilder(apiRequest, statusCode, response.message, mediaType, response.headers)

    try {
      val javaType = operation.determineMatchingResponseType(statusCode, mediaType)

      when {
        javaType == null -> {
          // The response is not described in the contract, return an unexpected response.
          val bodyContent = responseBody.string()
          response.close()
          val incompatibleResponse = responseBuilder.incompatibleResponse(bodyContent)
          throw ApiClientIncompatibleResponseException("The combination of the response's status code and content type is unknown", incompatibleResponse)
        }

        javaType == Void.TYPE -> {
          // The response should be empty according to the contract. Ignore body if it exists.
          response.close()
          return responseBuilder.apiResponse(javaType, null)
        }

        javaType == InputStream::class.java -> {
          // The contract says to return the body unprocessed, i.e., as type InputStream.
          // In this case, the application is responsible for closing the InputStream!
          return responseBuilder.apiResponse(javaType, responseBody.byteStream())
        }

        MediaTypes.isJsonMediaType(mediaType) -> {
          // The contract defines the response to be a JSON entity. Deserialize and return it.
          // Note: deserializeFromJson closes the response body.
          return deserializeFromJson(responseBuilder, responseBody, javaType)
        }

        javaType == String::class.java -> {
          // The contract defines the response to be some string content, e.g., text/plain, so just return it as a string.
          return responseBuilder.apiResponse(javaType, responseBody.string())
        }

        else -> {
          // In this case, the contract defines a schema for the response, but the server sends it in an unsupported format, i.e., a non-JSON format.
          val bodyContent = responseBody.string()
          val incompatibleResponse = responseBuilder.incompatibleResponse(bodyContent)
          throw ApiClientIncompatibleResponseException("Content-Type not supported by API client: $mediaType", incompatibleResponse)
        }
      }
    } catch (e: IOException) {
      // The body could not be read
      response.close()
      val incompleteResponse = responseBuilder.incompleteResponse()
      throw ApiClientIoException("Error reading response body: $e", incompleteResponse, e)
    }
  }

  /**
   * Deserializes the response entity and closes the response body.
   */
  @Throws(IOException::class, ApiClientIncompatibleResponseException::class)
  private fun deserializeFromJson(responseBuilder: ResponseBuilder, responseBody: ResponseBody, expectedType: Type): ApiResponse {
    val stringContent = responseBody.string()

    try {
      val entity = gson.fromJson<Any>(stringContent, expectedType)
      return responseBuilder.apiResponse(expectedType, entity)
    } catch (e: JsonParseException) {
      val incompatibleResponse = responseBuilder.incompatibleResponse(stringContent)
      throw ApiClientIncompatibleResponseException("JSON response from server cannot be parsed to $expectedType: $e", incompatibleResponse, e)
    }
  }

  private data class RequestAndResponse(val request: Request, val response: Response)

  /**
   * Builder for constructing Responses, either defined or undefined.
   */
  private class ResponseBuilder(
      private val request: ApiRequest,
      private val statusCode: Int,
      private val httpStatusMessage: String,
      private val contentType: String?,
      headers: Headers
  ) {

    private val headers = headers.toList()

    fun apiResponse(javaType: Type, entity: Any?): ApiResponse {
      return ApiResponse(request, statusCode, httpStatusMessage, contentType, headers, entity, javaType)
    }

    fun incompleteResponse(): IncompleteResponse {
      return IncompleteResponse(request, statusCode, httpStatusMessage, contentType, headers)
    }

    fun incompatibleResponse(responseBody: String): IncompatibleResponse {
      return IncompatibleResponse(request, statusCode, httpStatusMessage, contentType, headers, responseBody)
    }
  }

  /**
   * OkHttp interceptor for accessing the final request. This is necessary, because the application can use interceptors that add or modify headers, and we want
   * to report the final set of headers used.
   */
  private class RequestAccessInterceptor : Interceptor {

    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
      val request: Request = chain.request()
      THREAD_LOCAL.set(request)

      return chain.proceed(request)
    }

    companion object {

      private val THREAD_LOCAL = ThreadLocal<Request?>()

      val lastRequest: Request?
        get() = THREAD_LOCAL.get()

      fun clearThreadLocal() {
        THREAD_LOCAL.remove()
      }
    }
  }

  private companion object {

    private const val CONTENT_TYPE_HEADER = "Content-Type"

    private fun Headers.toList(): List<Header> = this.map { (name, value) -> Header(name, value) }
  }
}

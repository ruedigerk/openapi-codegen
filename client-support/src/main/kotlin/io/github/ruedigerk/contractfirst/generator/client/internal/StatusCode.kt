package io.github.ruedigerk.contractfirst.generator.client.internal

import java.util.*

/**
 * Represents the status code of a response of an API operation. The status code can either be a real numeric HTTP status code or the "default" status
 * code, a placeholder for not otherwise specified status codes.
 */
class StatusCode private constructor(private val code: Int?) {

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false
    
    val that = other as StatusCode
    
    return code == that.code
  }

  override fun hashCode(): Int {
    return Objects.hash(code)
  }

  override fun toString(): String {
    return code?.toString() ?: "default"
  }

  companion object {

    @JvmField
    val DEFAULT: StatusCode = StatusCode(null)

    @JvmStatic
    fun of(statusCode: Int): StatusCode {
      return StatusCode(statusCode)
    }
  }
}

package io.github.ruedigerk.contractfirst.generator.client

import java.io.File
import java.io.InputStream
import java.util.*

/**
 * Represents a file/binary body part of a multipart request body. It contains the content, file name and media type of the body part. It is instantiated
 * using static factory methods.
 */
class Attachment private constructor(

    /**
     * Content can be either of type java.io.File, java.io.InputStream or byte[].
     */
    val content: Any,

    val fileName: String,
    val mediaType: String
) {

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as Attachment

    if (content != other.content) return false
    if (fileName != other.fileName) return false
    if (mediaType != other.mediaType) return false

    return true
  }

  override fun hashCode(): Int {
    return Objects.hash(content, fileName, mediaType)
  }

  companion object {

    /**
     * Creates an attachment from the specified file and the specified media type.
     */
    @JvmStatic
    fun of(file: File, mediaType: String): Attachment {
      return Attachment(file, file.name, mediaType)
    }

    /**
     * Creates an attachment from the specified file and the specified media type. Uses the specified file name instead of the name of the specified file.
     */
    @JvmStatic
    fun of(file: File, fileName: String, mediaType: String): Attachment {
      return Attachment(file, fileName, mediaType)
    }

    /**
     * Creates an attachment by using the specified InputStream as content, the specified file name and the specified media type.
     */
    @JvmStatic
    fun of(content: InputStream, fileName: String, mediaType: String): Attachment {
      return Attachment(content, fileName, mediaType)
    }

    /**
     * Creates an attachment by using the specified byte array as content, the specified file name and the specified media type.
     */
    @JvmStatic
    fun of(content: ByteArray, fileName: String, mediaType: String): Attachment {
      return Attachment(content, fileName, mediaType)
    }
  }
}

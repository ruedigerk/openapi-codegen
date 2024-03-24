package io.github.ruedigerk.contractfirst.generator.client.internal

import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull

/**
 * Helper methods for working with media types.
 */
internal object MediaTypes {

  @JvmStatic
  fun isJsonMediaType(mediaType: String?): Boolean {
    return isJsonMediaType(mediaType?.toMediaTypeOrNull())
  }

  @JvmStatic
  fun isJsonMediaType(mediaType: MediaType?): Boolean {
    if (mediaType == null) {
      return false
    }

    val subtype = mediaType.subtype
    return mediaType.type == "application" && (subtype == "json" || subtype.startsWith("vnd.") && subtype.endsWith("+json"))
  }
}

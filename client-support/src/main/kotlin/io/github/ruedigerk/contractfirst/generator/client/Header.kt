package io.github.ruedigerk.contractfirst.generator.client

/**
 * Represents an HTTP header.
 */
data class Header(
    
    /**
     * The name of the header, e.g., "Content-Type".
     */
    val name: String,
    
    /**
     * The value of the header, e.g., "application/json".
     */
    val value: String
) {

  override fun toString(): String = "$name=$value"
}

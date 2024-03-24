package io.github.ruedigerk.contractfirst.generator.client.internal

/**
 * Represents the definition of a parameter of an API operation and the value that is transferred in it. The combination of name and location must be
 * unique for all parameters of an API operation.
 */
data class Parameter(val name: String, val location: ParameterLocation, val isRequired: Boolean, val value: Any?) {

  override fun toString(): String {
    return "$name ($location)"
  }
}

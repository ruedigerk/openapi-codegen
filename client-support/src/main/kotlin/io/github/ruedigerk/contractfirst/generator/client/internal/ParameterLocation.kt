package io.github.ruedigerk.contractfirst.generator.client.internal

/**
 * Represents the possible locations a parameter of an API operation can have.
 */
enum class ParameterLocation {

  QUERY,
  HEADER,
  PATH,
  COOKIE
}

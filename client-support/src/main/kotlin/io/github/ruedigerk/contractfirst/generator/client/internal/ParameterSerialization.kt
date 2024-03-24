package io.github.ruedigerk.contractfirst.generator.client.internal

import java.util.function.BiConsumer

/**
 * Performs HTTP requests as defined by generated client code.
 */
internal object ParameterSerialization {

  fun serializeFormStyleParameters(parameters: Map<String, Parameter>, parameterConsumer: BiConsumer<String, String>) {
    parameters.forEach { (name, parameter) -> serializeFormStyleParameter(name, parameter.value, parameterConsumer) }
  }

  /**
   * Serializes a "form" style parameter, like a query parameter. Splits Collection-typed values into separate calls to the consumer allowing output like:
   * color=blue&color=black&color=brown
   *
   * Serializes values as specified for style="form" and explode="true" parameters, which is the default for query parameters and x-www-form-urlencoded request
   * bodies.
   *
   * See: https://spec.openapis.org/oas/v3.0.3#style-examples
   */
  fun serializeFormStyleParameter(name: String, value: Any?, parameterConsumer: BiConsumer<String, String>) {
    if (value is Collection<*>) {
      value.filterNotNull().forEach { element -> serializeSingleFormStyleParameter(name, element, parameterConsumer) }
    } else {
      serializeSingleFormStyleParameter(name, value, parameterConsumer)
    }
  }

  private fun serializeSingleFormStyleParameter(name: String, value: Any?, parameterConsumer: BiConsumer<String, String>) {
    if (value != null) {
      val serializedValue = serializePrimitiveParameterValue(value)
      parameterConsumer.accept(name, serializedValue)
    }
  }

  /**
   * Serializes a "simple" style parameter, like a path parameter. Creates output like: blue,black,brown
   *
   * Serializes values as specified for style="simple" and explode="false" parameters, which is the default for path parameters.
   *
   * See: https://spec.openapis.org/oas/v3.0.3#style-examples
   */
  fun serializeSimpleStyleParameter(value: Any?): String {
    return if (value is Collection<*>) {
      value.filterNotNull().joinToString(",", transform = ::serializePrimitiveParameterValue)
    } else {
      serializePrimitiveParameterValue(value)
    }
  }

  /**
   * Serializes a single primitive parameter value, i.e. the value should not be the equivalent of a JSON-schema array or object.
   */
  fun serializePrimitiveParameterValue(value: Any?): String {
    // LocalDate and OffsetDateTime already return the desired format in their toString methods.
    return value?.toString() ?: ""
  }
}


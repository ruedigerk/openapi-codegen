package io.github.ruedigerk.contractfirst.generator.client.internal;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

/**
 * Performs HTTP requests as defined by generated client code.
 */
public class ParameterSerialization {

  public static void serializeFormStyleParameters(Map<String, Parameter> parameters, BiConsumer<String, String> parameterConsumer) {
    parameters.forEach((name, parameter) -> serializeFormStyleParameter(name, parameter.getValue(), parameterConsumer));
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
  public static void serializeFormStyleParameter(String name, Object value, BiConsumer<String, String> parameterConsumer) {
    if (value instanceof Collection<?>) {
      ((Collection<?>) value).stream()
          .filter(Objects::nonNull)
          .forEach(element -> serializeSingleFormStyleParameter(name, element, parameterConsumer));
    } else {
      serializeSingleFormStyleParameter(name, value, parameterConsumer);
    }
  }

  private static void serializeSingleFormStyleParameter(String name, Object value, BiConsumer<String, String> parameterConsumer) {
    if (value != null) {
      String serializedValue = serializePrimitiveParameterValue(value);
      parameterConsumer.accept(name, serializedValue);
    }
  }
  
  /**
   * Serializes a "simple" style parameter, like a path parameter. Creates output like: blue,black,brown
   *
   * Serializes values as specified for style="simple" and explode="false" parameters, which is the default for path parameters.
   *
   * See: https://spec.openapis.org/oas/v3.0.3#style-examples
   */
  public static String serializeSimpleStyleParameter(Object value) {
    if (value instanceof Collection) {
      return ((Collection<?>) value).stream()
          .filter(Objects::nonNull)
          .map(ParameterSerialization::serializePrimitiveParameterValue)
          .collect(Collectors.joining(","));
    } else {
      return serializePrimitiveParameterValue(value);
    }
  }

  /**
   * Serializes a single primitive parameter value, i.e. the value should not be the equivalent of a JSON-schema array or object.
   */
  public static String serializePrimitiveParameterValue(Object value) {
    if (value == null) {
      return "";
    } else {
      // LocalDate and OffsetDateTime return the desired format in their toString methods.
      return value.toString();
    }
  }
}


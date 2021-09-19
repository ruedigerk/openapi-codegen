package org.contractfirst.generator.parser

import io.swagger.v3.oas.models.media.ArraySchema
import io.swagger.v3.oas.models.media.Schema
import org.contractfirst.generator.NotSupportedException
import org.contractfirst.generator.logging.Log
import org.contractfirst.generator.model.*
import org.contractfirst.generator.parser.ParserHelper.normalize
import org.contractfirst.generator.parser.ParserHelper.nullToEmpty

class SchemaParser(private val log: Log) {

  fun parseTopLevelSchemas(schemas: Map<String, Schema<*>>): Map<MSchemaRef, MSchemaNonRef> = schemas
      .mapValues { toTopLevelSchema(it.value, NameHint(it.key)) }
      .mapKeys { MSchemaRef("#/components/schemas/${it.key}") }

  private fun toTopLevelSchema(schema: Schema<*>, location: NameHint): MSchemaNonRef =
      (parseSchema(schema, location) as? MSchemaNonRef) ?: throw NotSupportedException("Unsupported schema reference in #/components/schemas: $schema")

  fun parseSchema(schema: Schema<*>, location: NameHint): MSchema {
    log.debug { "Parsing schema $location of ${schema.javaClass.simpleName}" }

    if (schema.`$ref` != null) {
      return MSchemaRef(schema.`$ref`)
    }
    if (schema.enum != null && schema.enum.isNotEmpty()) {
      return toEnumSchema(schema, location)
    }

    return when (schema.type) {
      "array" -> toArraySchema(schema as ArraySchema, location)
      "boolean", "integer", "number", "string" -> toPrimitiveSchema(MPrimitiveType.valueOf(schema.type.uppercase()), schema, location)
      "object", null -> toObjectOrMapSchema(schema, location)
      else -> throw NotSupportedException("Schema type '${schema.type}' is not supported: $schema")
    }
  }

  private fun toEnumSchema(schema: Schema<*>, location: NameHint): MSchemaEnum {
    if (schema.type != "string") {
      throw NotSupportedException("Currently only enum schemas of type 'string' are supported, type is '${schema.type}'")
    }

    return MSchemaEnum(schema.title.normalize(), schema.description.normalize(), schema.enum.map { it.toString() }, location)
  }

  @Suppress("UNCHECKED_CAST")
  private fun toArraySchema(schema: ArraySchema, location: NameHint): MSchemaArray {
    val itemsSchema = parseSchema(schema.items as Schema<*>, location)

    return MSchemaArray(
        schema.title.normalize(),
        schema.description.normalize(),
        itemsSchema,
        schema.uniqueItems ?: false,
        schema.minItems,
        schema.maxItems,
        location
    ).also { itemsSchema.embedIn(it) }
  }

  private fun toPrimitiveSchema(primitiveType: MPrimitiveType, schema: Schema<*>, location: NameHint): MSchemaPrimitive {
    return MSchemaPrimitive(
        schema.title.normalize(),
        schema.description.normalize(),
        primitiveType,
        schema.format.normalize(),
        schema.minimum,
        schema.maximum,
        schema.exclusiveMinimum ?: false,
        schema.exclusiveMaximum ?: false,
        schema.minLength,
        schema.maxLength,
        schema.pattern,
        location
    )
  }

  private fun toObjectOrMapSchema(schema: Schema<*>, location: NameHint): MSchemaNonRef {
    val properties = schema.properties.nullToEmpty()
    val additionalProperties = schema.additionalProperties as? Schema<*>
    // TODO: additionalProperties of type Boolean instead of Schema are ignored.

    return when {
      properties.isNotEmpty() && additionalProperties != null -> {
        throw NotSupportedException("Object schemas having both properties and additionalProperties are not supported, just either or: $schema")
      }
      additionalProperties != null -> toMapSchema(schema, parseSchema(additionalProperties, location / "additionalProperties"), location)
      else -> toObjectSchema(schema, location)
    }
  }

  private fun toMapSchema(schema: Schema<*>, valuesSchema: MSchema, location: NameHint): MSchemaMap =
      MSchemaMap(schema.title.normalize(), schema.description.normalize(), valuesSchema, schema.minItems, schema.maxItems, location)
          .also { valuesSchema.embedIn(it) }

  private fun toObjectSchema(schema: Schema<*>, location: NameHint): MSchemaObject {
    val requiredProperties = schema.required.nullToEmpty().toSet()
    val properties = schema.properties.nullToEmpty().map { (name, schema) ->
      MSchemaProperty(name, requiredProperties.contains(name), parseSchema(schema, location / name))
    }

    val schemaObject = MSchemaObject(schema.title.normalize(), schema.description.normalize(), properties, location)

    properties.forEach { it.schema.embedIn(schemaObject) }

    return schemaObject
  }

  private fun MSchema.embedIn(parent: MSchemaNonRef) {
    // Only inline schemas are treated as embedded in another schema, referencing a schema is not treated as embedding. 
    if (this is MSchemaNonRef) {
      this.embeddedIn = parent
    }
  }
}
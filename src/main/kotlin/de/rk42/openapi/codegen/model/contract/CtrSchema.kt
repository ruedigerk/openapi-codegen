package de.rk42.openapi.codegen.model.contract

sealed interface CtrSchema

data class CtrSchemaRef(
    val reference: String
) : CtrSchema {

  fun referencedName(): String {
    val lastSlashIndex = reference.lastIndexOf('/')
    return reference.substring(lastSlashIndex + 1)
  }
}

sealed interface CtrSchemaNonRef : CtrSchema {
  
  val title: String?
  
  val description: String?
  
  /** A reference this schema is referenced by (optional) */
  var referencedBy: CtrSchemaRef?
}

data class CtrSchemaObject(
    override val title: String?,
    override val description: String?,
    val properties: List<CtrSchemaProperty>,
    override var referencedBy: CtrSchemaRef? = null
) : CtrSchemaNonRef

data class CtrSchemaProperty(
    val name: String,
    val required: Boolean,
    var schema: CtrSchema
)

data class CtrSchemaArray(
    override val title: String?,
    override val description: String?,
    var itemSchema: CtrSchema,
    override var referencedBy: CtrSchemaRef? = null
) : CtrSchemaNonRef

data class CtrSchemaMap(
    override val title: String?,
    override val description: String?,
    var valuesSchema: CtrSchema,
    override var referencedBy: CtrSchemaRef? = null
) : CtrSchemaNonRef

/**
 * Currently Enums are always assumed to habe type "string".
 */
data class CtrSchemaEnum(
    override val title: String?,
    override val description: String?,
    val values: List<String>,
    override var referencedBy: CtrSchemaRef? = null
) : CtrSchemaNonRef

data class CtrSchemaPrimitive(
    override val title: String?,
    override val description: String?,
    val type: CtrPrimitiveType,
    val format: String?,
    override var referencedBy: CtrSchemaRef? = null
) : CtrSchemaNonRef

/**
 * Type "null" is currently not supported.
 */
enum class CtrPrimitiveType {

  BOOLEAN,
  INTEGER,
  NUMBER,
  STRING
}
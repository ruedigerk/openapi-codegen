package de.rk42.openapi.codegen.model.java

sealed interface JavaType

data class JavaClass(
    val className: String,
    val title: String,
    val properties: List<JavaProperty>,
) : JavaType

data class JavaProperty(
    val javaIdentifier: String,
    val name: String,
    val required: Boolean,
    var type: JavaReference
)

data class JavaEnum(
    val className: String,
    val title: String,
    val values: List<EnumConstant>
) : JavaType

data class EnumConstant(
    val originalName: String,
    val javaIdentifier: String
)

data class JavaBuiltIn(
    val typeName: String
) : JavaType

data class JavaReference(
    val typeName: String,
    val packageName: String,
    val isClass: Boolean,
    val typeParameter: JavaReference? = null,
)
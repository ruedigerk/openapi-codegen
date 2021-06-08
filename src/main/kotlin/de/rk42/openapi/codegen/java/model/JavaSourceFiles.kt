package de.rk42.openapi.codegen.java.model

sealed interface JavaSourceFile

data class JavaClassFile(
    val className: String,
    val javadoc: String?,
    val properties: List<JavaProperty>,
) : JavaSourceFile

data class JavaProperty(
    val javaName: String,
    val javadoc: String?,
    val originalName: String,
    val required: Boolean,
    var type: JavaAnyType
)

data class JavaEnumFile(
    val className: String,
    val javadoc: String?,
    val values: List<EnumConstant>
) : JavaSourceFile

data class EnumConstant(
    val javaName: String,
    val originalName: String
)

package io.github.ruedigerk.contractfirst.generator.java.generator

import com.squareup.javapoet.*
import io.github.ruedigerk.contractfirst.generator.Configuration
import io.github.ruedigerk.contractfirst.generator.java.Identifiers.capitalize
import io.github.ruedigerk.contractfirst.generator.java.generator.GeneratorCommon.NOT_NULL_ANNOTATION
import io.github.ruedigerk.contractfirst.generator.java.generator.GeneratorCommon.toAnnotation
import io.github.ruedigerk.contractfirst.generator.java.generator.GeneratorCommon.toTypeName
import io.github.ruedigerk.contractfirst.generator.java.generator.JavapoetExtensions.doIf
import io.github.ruedigerk.contractfirst.generator.java.generator.JavapoetExtensions.doIfNotNull
import io.github.ruedigerk.contractfirst.generator.java.model.*
import java.io.File
import javax.lang.model.element.Modifier.PRIVATE
import javax.lang.model.element.Modifier.PUBLIC

/**
 * Generates the code for the model classes.
 */
class ModelGenerator(configuration: Configuration) {

  private val outputDir = File(configuration.outputDir)
  private val modelPackage = "${configuration.outputJavaBasePackage}.model"

  fun generateCode(specification: JavaSpecification) {
    specification.modelFiles.asSequence()
        .map(::toJavaFile)
        .forEach(::writeFile)
  }

  private fun writeFile(javaFile: JavaFile) {
    javaFile.writeTo(outputDir)
  }

  private fun toJavaFile(sourceFile: JavaSourceFile): JavaFile {
    val typeSpec = when (sourceFile) {
      is JavaClassFile -> toJavaClass(sourceFile)
      is JavaEnumFile -> toJavaEnum(sourceFile)
    }

    return JavaFile.builder(modelPackage, typeSpec)
        .skipJavaLangImports(true)
        .build()
  }

  private fun toJavaClass(classFile: JavaClassFile): TypeSpec {
    val fields = classFile.properties.map(::toField)
    val accessors = classFile.properties.flatMap { generateAccessorMethods(it, classFile.className) }
    val equalsHashCodeAndToString = MethodsFromObject.generateEqualsHashCodeAndToString(classFile.className.toTypeName(), fields)

    return TypeSpec.classBuilder(classFile.className)
        .doIfNotNull(classFile.javadoc) { addJavadoc("\$L", it) }
        .addModifiers(PUBLIC)
        .addFields(fields)
        .addMethods(accessors)
        .addMethods(equalsHashCodeAndToString)
        .build()
  }

  private fun generateAccessorMethods(property: JavaProperty, className: String): List<MethodSpec> {
    val propertyTypeName = property.type.toTypeName()

    // The getter is annotated with BeanValidation annotations.
    val getter = generateGetter(property, propertyTypeName)
    val setter = generateSetter(property, propertyTypeName)
    val builder = generateBuilderSetter(property, className, propertyTypeName)

    return listOf(builder, getter, setter)
  }

  private fun generateSetter(property: JavaProperty, propertyTypeName: TypeName): MethodSpec =
      MethodSpec.methodBuilder("set${property.javaName.capitalize()}")
          .addModifiers(PUBLIC)
          .addParameter(propertyTypeName, property.javaName)
          .addStatement("this.\$1N = \$1N", property.javaName)
          .build()

  private fun generateBuilderSetter(property: JavaProperty, className: String, propertyTypeName: TypeName): MethodSpec =
      MethodSpec.methodBuilder(property.javaName)
          .addModifiers(PUBLIC)
          .returns(className.toTypeName())
          .addParameter(propertyTypeName, property.javaName)
          .addStatement("this.\$1N = \$1N", property.javaName)
          .addStatement("return this", property.javaName)
          .build()

  private fun generateGetter(property: JavaProperty, propertyTypeName: TypeName): MethodSpec {
    return MethodSpec.methodBuilder("get${property.javaName.capitalize()}")
        .addModifiers(PUBLIC)
        .returns(propertyTypeName)
        .addStatement("return \$N", property.javaName)
        .build()
  }

  private fun toField(property: JavaProperty): FieldSpec {
    val typeValidationAnnotations = property.type.validations.map(GeneratorCommon::toAnnotation)

    return FieldSpec.builder(property.type.toTypeName(), property.javaName, PRIVATE)
        .doIfNotNull(property.javadoc) { addJavadoc("\$L", it) }
        .doIf(property.required) { addAnnotation(NOT_NULL_ANNOTATION) }
        .doIf(property.javaName != property.originalName) { addAnnotation(serializedNameAnnotation(property.originalName)) }
        .addAnnotations(typeValidationAnnotations)
        .doIfNotNull(property.initializerType) { initializer("new \$T<>()", it.toTypeName()) }
        .build()
  }

  private fun toJavaEnum(enumFile: JavaEnumFile): TypeSpec {
    val builder = TypeSpec.enumBuilder(enumFile.className)
        .doIfNotNull(enumFile.javadoc) { addJavadoc("\$L", it) }
        .addModifiers(PUBLIC)

    enumFile.constants.forEach { enumConstant ->
      val constant = TypeSpec.anonymousClassBuilder("")
          .doIf(enumConstant.javaName != enumConstant.originalName) { addAnnotation(serializedNameAnnotation(enumConstant.originalName)) }
          .build()

      builder.addEnumConstant(enumConstant.javaName, constant)
    }

    return builder.build()
  }

  private fun serializedNameAnnotation(originalName: String) = toAnnotation("com.google.gson.annotations.SerializedName", originalName)
}
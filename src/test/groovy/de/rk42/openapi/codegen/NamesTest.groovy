package de.rk42.openapi.codegen

import spock.lang.Specification

class NamesTest extends Specification {

  def "toJavaIdentifier"() {
    expect:
    Names.toJavaIdentifier(input) == expected

    where:
    input                    | expected
    "1Abcd"                  | "_1abcd"
    "Abcd"                   | "abcd"
    "abcd"                   | "abcd"
    " ab cd ef "             | "abCdEf"
    "some-value-here"        | "someValueHere"
    "some_value_here"        | "someValueHere"
    "some2value/for.testing" | "some2ValueForTesting"
    "\$type"                 | "\$type"
    "123"                    | "_123"
    "\$123"                  | "\$123"
  }

  def "toJavaTypeIdentifier"() {
    expect:
    Names.toJavaTypeIdentifier(input) == expected

    where:
    input                    | expected
    "1Abcd"                  | "_1Abcd"
    "Abcd"                   | "Abcd"
    "abcd"                   | "Abcd"
    " ab cd ef "             | "AbCdEf"
    "some-value-here"        | "SomeValueHere"
    "some_value_here"        | "SomeValueHere"
    "some2value/for.testing" | "Some2ValueForTesting"
    "\$type"                 | "\$Type"
    "123"                    | "_123"
    "\$123"                  | "\$123"
  }

  def "toJavaConstant"() {
    expect:
    Names.toJavaConstant(input) == expected

    where:
    input                    | expected
    "Abcd"                   | "ABCD"
    "abcd"                   | "ABCD"
    "HTTPUrl"                | "HTTP_URL"
    "AbCdEf"                 | "AB_CD_EF"
    "abCdEf"                 | "AB_CD_EF"
    " ab cd ef "             | "_AB_CD_EF"
    "some-value-here"        | "SOME_VALUE_HERE"
    "some_value_here"        | "SOME_VALUE_HERE"
    "some2value/for.testing" | "SOME2_VALUE_FOR_TESTING"
    "\$type"                 | "\$_TYPE"
    "123"                    | "_123"
    "\$123"                  | "\$123"
  }

  def "mediaTypeToJavaIdentifier"() {
    expect:
    Names.mediaTypeToJavaIdentifier(input) == expected

    where:
    input                             | expected
    "application/json"                | "ApplicationJson"
    "application/xml"                 | "ApplicationXml"
    "application/*"                   | "ApplicationStar"
    "*/*"                             | "StarStar"
    "application/json; charset=UTF-8" | "ApplicationJsonCharsetUtf8"
  }

  def "capitalize"() {
    expect:
    Names.capitalize(input) == expected

    where:
    input   | expected
    "test"  | "Test"
    " test" | " test"
    ""      | ""
    "Test"  | "Test"
  }

  def "camelize"() {
    expect:
    Names.toCamelCase(input, uppercaseFirstLetter) == expected

    where:
    input                    | uppercaseFirstLetter | expected
    "Abcd"                   | true                 | "Abcd"
    "abcd"                   | true                 | "Abcd"
    "HTTPUrl"                | true                 | "HttpUrl"
    "AbCdEf"                 | true                 | "AbCdEf"
    "abCdEf"                 | true                 | "AbCdEf"
    " ab cd ef "             | true                 | "AbCdEf"
    "some-value-here"        | true                 | "SomeValueHere"
    "some_value_here"        | true                 | "SomeValueHere"
    "some2value/for.testing" | true                 | "Some2ValueForTesting"
    "\$type"                 | true                 | "\$Type"
    "123"                    | true                 | "123"
    "\$123"                  | true                 | "\$123"

    "Abcd"                   | false                | "abcd"
    "abcd"                   | false                | "abcd"
    "HTTPUrl"                | false                | "httpUrl"
    "AbCdEf"                 | false                | "abCdEf"
    "abCdEf"                 | false                | "abCdEf"
    " ab cd ef "             | false                | "abCdEf"
    "some-value-here"        | false                | "someValueHere"
    "some_value_here"        | false                | "someValueHere"
    "some2value/for.testing" | false                | "some2ValueForTesting"
    "\$type"                 | false                | "\$type"
    "123"                    | false                | "123"
    "\$123"                  | false                | "\$123"
  }
}

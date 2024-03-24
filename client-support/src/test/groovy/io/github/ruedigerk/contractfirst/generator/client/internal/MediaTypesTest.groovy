package io.github.ruedigerk.contractfirst.generator.client.internal

import okhttp3.MediaType
import spock.lang.Specification
import spock.lang.Unroll

class MediaTypesTest extends Specification {

  @Unroll
  def "isJsonMediaType (String) #contentType"() {
    when:
    def result = MediaTypes.isJsonMediaType((String) contentType)

    then:
    result == expected

    where:
    contentType                                                                     | expected
    "application/json"                                                              | true
    "application/json; charset=UTF-8"                                               | true
    "application/json;Charset=UTF-8; whatever=\"some value\""                       | true
    "application/vnd.custom-media-type+json"                                        | true
    "application/vnd.custom-media-type+json;Charset=UTF-8; whatever=\"some value\"" | true
    "application/pdf"                                                               | false
    "text/plain"                                                                    | false
    "nonsense"                                                                      | false
    null                                                                            | false
  }

  @Unroll
  def "isJsonMediaType (MediaType) #contentType"() {
    when:
    def result = MediaTypes.isJsonMediaType((MediaType) contentType)

    then:
    result == expected

    where:
    contentType                                                                                    | expected
    MediaType.get("application/json")                                                              | true
    MediaType.get("application/json; charset=UTF-8")                                               | true
    MediaType.get("application/json;Charset=UTF-8; whatever=\"some value\"")                       | true
    MediaType.get("application/vnd.custom-media-type+json")                                        | true
    MediaType.get("application/vnd.custom-media-type+json;Charset=UTF-8; whatever=\"some value\"") | true
    MediaType.get("application/pdf")                                                               | false
    MediaType.get("text/plain")                                                                    | false
    null                                                                                           | false
  }
}

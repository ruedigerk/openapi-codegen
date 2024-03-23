package io.github.ruedigerk.contractfirst.generator.integrationtest

import io.github.ruedigerk.contractfirst.generator.client.Attachment
import io.github.ruedigerk.contractfirst.generator.integrationtest.generated.client.api.MultipartRequestBodyApiClient
import io.github.ruedigerk.contractfirst.generator.integrationtest.generated.client.model.CFormEncodedRequestBodyRequestBodyApplicationXWwwFormUrlencodedEnumProperty
import io.github.ruedigerk.contractfirst.generator.integrationtest.generated.client.model.CMultipartRequestBodyRequestBodyMultipartFormDataObjectProperty
import io.github.ruedigerk.contractfirst.generator.integrationtest.generated.server.model.SMultipartRequestBodyRequestBodyMultipartFormDataObjectProperty
import io.github.ruedigerk.contractfirst.generator.integrationtest.spec.EmbeddedJaxRsServerSpecification
import org.glassfish.jersey.media.multipart.FormDataBodyPart
import org.glassfish.jersey.media.multipart.FormDataParam
import spock.lang.Subject

import javax.ws.rs.*
import javax.ws.rs.core.MediaType

/**
 * Tests serialization of form encoded request bodies.
 */
class MultipartRequestBodyClientTest extends EmbeddedJaxRsServerSpecification {

  @Subject
  MultipartRequestBodyApiClient apiClient = new MultipartRequestBodyApiClient(apiClientSupport)

  @Override
  Class<?> getTestResource() {
    EmbeddedServerResource
  }

  def "Test form encoded request body"() {
    when:
    def result = apiClient.returningResult().formEncodedRequestBody(
        "a&1",
        42L,
        CFormEncodedRequestBodyRequestBodyApplicationXWwwFormUrlencodedEnumProperty.SECOND_VALUE
    )

    then:
    result.isStatus204WithoutEntity()
  }

  def "Test multipart form data request body"() {
    when:
    def result = apiClient.returningResult().multipartRequestBody(
        "allParametersPresent",
        "a&1",
        42L,
        new CMultipartRequestBodyRequestBodyMultipartFormDataObjectProperty(a: "string", b: 23L),
        Attachment.of(new File("src/test/resources/sample.pdf"), "application/pdf"),
        [
            Attachment.of(getSamplePdfAsInputStream().bytes, "sample-bytes.pdf", "application/pdf"),
            Attachment.of(getSamplePdfAsInputStream(), "sample-is.pdf", "application/pdf"),
        ]
    )

    then:
    result.isStatus204WithoutEntity()
  }

//  def "Test multipart form data request body with optional parameters not supplied"() {
//    when:
//    def result = apiClient.returningResult().multipartRequestBody(
//        "optionalParametersMissing",
//        null,
//        null,
//        null,
//        null,
//        []
//    )
//
//    then:
//    result.isStatus204WithoutEntity()
//  }

  static private InputStream getSamplePdfAsInputStream() {
    MultipartRequestBodyClientTest.getResourceAsStream("/sample.pdf")
  }

  /**
   * JAX-RS resource implementation used in this test.
   */
  @Path("")
  static class EmbeddedServerResource {

    @POST
    @Path("/formEncodedRequestBody")
    @Consumes("application/x-www-form-urlencoded")
    void formEncodedRequestBody(
        @FormParam("stringProperty") String stringProperty,
        @FormParam("integerProperty") String integerProperty,
        @FormParam("enumProperty") String enumProperty
    ) {
      assert stringProperty == "a&1"
      assert integerProperty == "42"
      assert enumProperty == "second%value"
    }

    @POST
    @Path("/multipartRequestBody")
    @Consumes("multipart/form-data")
    void multipartRequestBody(
        @QueryParam("testSelector") String testSelector,
        @FormDataParam("stringProperty") String stringProperty,
        @FormDataParam("integerProperty") Long integerProperty,
        @FormDataParam("objectProperty") SMultipartRequestBodyRequestBodyMultipartFormDataObjectProperty objectProperty,
        @FormDataParam("firstBinary") FormDataBodyPart firstBinary,
        @FormDataParam("additionalBinaries") List<FormDataBodyPart> additionalBinaries
    ) {
      if (testSelector == "optionalParametersMissing") {
        assert stringProperty == null
        assert integerProperty == null
        assert objectProperty == null
//        assert firstBinary == null
        assert additionalBinaries == null
      } else {
        assert stringProperty == "a&1"
        assert integerProperty == 42L
        assert objectProperty == new SMultipartRequestBodyRequestBodyMultipartFormDataObjectProperty(a: "string", b: 23L)

        verifyBodyPartIsPdf(firstBinary, "firstBinary", "sample.pdf")

        assert additionalBinaries.size() == 2

        verifyBodyPartIsPdf(additionalBinaries[0], "additionalBinaries", "sample-bytes.pdf")
        verifyBodyPartIsPdf(additionalBinaries[1], "additionalBinaries", "sample-is.pdf")
      }
    }

    private static void verifyBodyPartIsPdf(FormDataBodyPart bodyPart, String name, String fileName) {
      assert bodyPart.name == name
      assert bodyPart.contentDisposition.fileName == fileName
      assert bodyPart.mediaType == new MediaType("application", "pdf")
      assert bodyPart.getValueAs(byte[]) == getSamplePdfAsInputStream().bytes
    }
  }
}

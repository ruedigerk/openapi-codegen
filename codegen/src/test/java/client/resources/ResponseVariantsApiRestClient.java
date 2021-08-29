package client.resources;

import client.model.Error;
import client.model.Pet;
import de.rk42.openapi.codegen.client.DefinedResponse;
import de.rk42.openapi.codegen.client.GenericResponse;
import de.rk42.openapi.codegen.client.RestClientIoException;
import de.rk42.openapi.codegen.client.RestClientSupport;
import de.rk42.openapi.codegen.client.RestClientUndefinedResponseException;
import de.rk42.openapi.codegen.client.RestClientValidationException;
import de.rk42.openapi.codegen.client.internal.Operation;
import de.rk42.openapi.codegen.client.internal.ParameterLocation;
import de.rk42.openapi.codegen.client.internal.StatusCode;

public class ResponseVariantsApiRestClient {
  private final RestClientSupport support;

  public ResponseVariantsApiRestClient(RestClientSupport support) {
    this.support = support;
  }

  /**
   * Test for the various parameter locations and for serializing request and response body entities.
   *
   * @param petStoreId ID of pet store
   * @param dryRun Do a dry run?
   * @param customerId Optional customer ID
   * @param testCaseSelector Used to select the desired behaviour of the server in the test.
   */
  public Pet createPet(String petStoreId, Boolean dryRun, Long customerId, String testCaseSelector,
      Pet requestBody) throws RestClientIoException, RestClientValidationException,
      RestClientUndefinedResponseException {

    GenericResponse genericResponse = createPetWithResponse(petStoreId, dryRun, customerId, testCaseSelector, requestBody);
    DefinedResponse response = genericResponse.asDefinedResponse();

    if (!response.isSuccessful()) {
      throw new RestClientErrorEntityException(response.getStatusCode(), (Error) response.getEntity());
    }

    return (Pet) response.getEntity();
  }

  /**
   * Test for the various parameter locations and for serializing request and response body entities.
   *
   * @param petStoreId ID of pet store
   * @param dryRun Do a dry run?
   * @param customerId Optional customer ID
   * @param testCaseSelector Used to select the desired behaviour of the server in the test.
   */
  public GenericResponse createPetWithResponse(String petStoreId, Boolean dryRun, Long customerId,
      String testCaseSelector, Pet requestBody) throws RestClientIoException,
      RestClientValidationException {

    Operation.Builder builder = new Operation.Builder("/{petStoreId}/pets", "POST");

    builder.parameter("petStoreId", ParameterLocation.PATH, true, petStoreId);
    builder.parameter("dryRun", ParameterLocation.QUERY, false, dryRun);
    builder.parameter("customerId", ParameterLocation.HEADER, false, customerId);
    builder.parameter("testCaseSelector", ParameterLocation.HEADER, false, testCaseSelector);
    builder.requestBody("application/json", true, requestBody);

    builder.response(StatusCode.of(200), "application/json", Pet.class);
    builder.response(StatusCode.of(201));
    builder.response(StatusCode.of(204));
    builder.response(StatusCode.of(400), "application/json", Error.class);
    builder.response(StatusCode.DEFAULT, "application/json", Error.class);

    return support.executeRequest(builder.build());
  }
}

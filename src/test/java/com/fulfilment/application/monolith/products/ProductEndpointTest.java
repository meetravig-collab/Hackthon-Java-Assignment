package com.fulfilment.application.monolith.products;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.core.IsNot.not;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class ProductEndpointTest {

  @Test
  public void testCrudProduct() {
    final String path = "product";

    // List all, should have all 3 products the database has initially:
    given()
        .when()
        .get(path)
        .then()
        .statusCode(200)
        .body(containsString("TONSTAD"), containsString("KALLAX"), containsString("BESTÅ"));

    // Delete the TONSTAD:
    given().when().delete(path + "/1").then().statusCode(204);

    // List all, TONSTAD should be missing now:
    given()
        .when()
        .get(path)
        .then()
        .statusCode(200)
        .body(not(containsString("TONSTAD")), containsString("KALLAX"), containsString("BESTÅ"));
  }

  @Test
  public void testProductCrudAndErrorBranches() {
    final String path = "product";

    // unknown id -> 404 (also exercises the ErrorMapper)
    given().when().get(path + "/999999").then().statusCode(404);

    // id set on create -> 422
    given().contentType("application/json").body("{\"id\":123,\"name\":\"X\",\"stock\":1}")
        .when().post(path).then().statusCode(422);

    // create a new product and read its generated id
    int id =
        given()
            .contentType("application/json")
            .body("{\"name\":\"REST-Prod\",\"stock\":4}")
            .when()
            .post(path)
            .then()
            .statusCode(201)
            .extract()
            .path("id");

    // get the created product
    given().when().get(path + "/" + id).then().statusCode(200).body(containsString("REST-Prod"));

    // update it
    given().contentType("application/json").body("{\"name\":\"REST-Prod-2\",\"stock\":9}")
        .when().put(path + "/" + id).then().statusCode(200).body(containsString("REST-Prod-2"));

    // update with missing name -> 422
    given().contentType("application/json").body("{\"stock\":1}")
        .when().put(path + "/" + id).then().statusCode(422);

    // update a non-existent product -> 404
    given().contentType("application/json").body("{\"name\":\"X\"}")
        .when().put(path + "/999999").then().statusCode(404);

    // delete it, then deleting again -> 404
    given().when().delete(path + "/" + id).then().statusCode(204);
    given().when().delete(path + "/999999").then().statusCode(404);
  }
}

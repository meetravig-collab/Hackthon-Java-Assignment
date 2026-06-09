package com.fulfilment.application.monolith.stores;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.Matchers.equalTo;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

/**
 * Exercises {@link StoreResource} (CRUD, PATCH and validation/error branches) and, via the fired
 * domain events, the {@link StoreEventObserver}. Tests create and delete their own stores so they
 * do not depend on seed data.
 */
@QuarkusTest
class StoreEndpointTest {

  private static final String PATH = "store";

  private int createStore(String name, int qty) {
    return given()
        .contentType("application/json")
        .body("{\"name\":\"" + name + "\",\"quantityProductsInStock\":" + qty + "}")
        .when()
        .post(PATH)
        .then()
        .statusCode(201)
        .extract()
        .path("id");
  }

  @Test
  void fullCrudLifecycle() {
    int id = createStore("REST-Store", 5);

    given().when().get(PATH + "/" + id).then().statusCode(200).body("name", equalTo("REST-Store"));

    given().contentType("application/json").body("{\"name\":\"REST-Store-2\",\"quantityProductsInStock\":7}")
        .when().put(PATH + "/" + id).then().statusCode(200).body("name", equalTo("REST-Store-2"));

    given().contentType("application/json").body("{\"name\":\"REST-Store-3\",\"quantityProductsInStock\":9}")
        .when().patch(PATH + "/" + id).then().statusCode(200).body("name", equalTo("REST-Store-3"));

    given().when().delete(PATH + "/" + id).then().statusCode(204);
  }

  @Test
  void listReturnsOk() {
    int id = createStore("REST-List", 3);
    given().when().get(PATH).then().statusCode(200).body(containsString("REST-List"));
    given().when().delete(PATH + "/" + id).then().statusCode(204);
  }

  @Test
  void errorBranches() {
    // unknown id -> 404 (also exercises the ErrorMapper)
    given().when().get(PATH + "/999999").then().statusCode(404);

    // id set on create -> 422
    given().contentType("application/json").body("{\"id\":123,\"name\":\"X\",\"quantityProductsInStock\":1}")
        .when().post(PATH).then().statusCode(422);

    // update a non-existent store -> 404
    given().contentType("application/json").body("{\"name\":\"X\",\"quantityProductsInStock\":1}")
        .when().put(PATH + "/999999").then().statusCode(404);

    int id = createStore("REST-Err", 2);

    // update / patch with missing name -> 422
    given().contentType("application/json").body("{\"quantityProductsInStock\":1}")
        .when().put(PATH + "/" + id).then().statusCode(422);
    given().contentType("application/json").body("{\"quantityProductsInStock\":1}")
        .when().patch(PATH + "/" + id).then().statusCode(422);

    // patch a non-existent store -> 404
    given().contentType("application/json").body("{\"name\":\"X\",\"quantityProductsInStock\":1}")
        .when().patch(PATH + "/999999").then().statusCode(404);

    // delete a non-existent store -> 404
    given().when().delete(PATH + "/999999").then().statusCode(404);

    given().when().delete(PATH + "/" + id).then().statusCode(204);
  }
}

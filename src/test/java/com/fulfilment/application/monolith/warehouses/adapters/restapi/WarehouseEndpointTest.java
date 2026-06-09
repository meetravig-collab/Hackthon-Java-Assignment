package com.fulfilment.application.monolith.warehouses.adapters.restapi;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;

import com.fulfilment.application.monolith.warehouses.adapters.database.WarehouseRepository;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * Exercises {@link WarehouseResourceImpl} through the real HTTP stack. Each test creates its own
 * uniquely-coded warehouse and removes it afterwards, so it is independent of the seed data and of
 * other tests sharing the in-memory database.
 */
@QuarkusTest
class WarehouseEndpointTest {

  private static final String PATH = "warehouse";
  private static final String CODE = "RESTTEST-1";

  @Inject WarehouseRepository warehouseRepository;

  @AfterEach
  void cleanup() {
    QuarkusTransaction.requiringNew().run(() -> {
      Warehouse w = new Warehouse();
      w.businessUnitCode = CODE;
      warehouseRepository.remove(w); // no-op if it was never created
    });
  }

  private void createTestWarehouse() {
    given()
        .contentType("application/json")
        .body("{\"businessUnitCode\":\"" + CODE + "\",\"location\":\"ZWOLLE-002\",\"capacity\":40,\"stock\":10}")
        .when()
        .post(PATH)
        .then()
        .statusCode(200) // the contract-generated resource returns the created entity with 200
        .body(containsString(CODE));
  }

  @Test
  void listIncludesCreatedWarehouse() {
    createTestWarehouse();
    given().when().get(PATH).then().statusCode(200).body(containsString(CODE));
  }

  @Test
  void getByIdReturnsWarehouseOrNotFound() {
    createTestWarehouse();
    given().when().get(PATH + "/" + CODE).then().statusCode(200)
        .body(containsString(CODE), containsString("ZWOLLE-002"));
    given().when().get(PATH + "/UNKNOWN-XYZ").then().statusCode(404);
  }

  @Test
  void createRejectsInvalidCapacity() {
    // capacity 100 exceeds TILBURG-001 max capacity (40) -> domain rejects -> 400
    given()
        .contentType("application/json")
        .body("{\"businessUnitCode\":\"RESTTEST-BAD\",\"location\":\"TILBURG-001\",\"capacity\":100,\"stock\":10}")
        .when()
        .post(PATH)
        .then()
        .statusCode(400);
  }

  @Test
  void replaceUpdatesExistingOrRejectsUnknown() {
    createTestWarehouse();
    given()
        .contentType("application/json")
        .body("{\"location\":\"ZWOLLE-002\",\"capacity\":45,\"stock\":20}")
        .when()
        .post(PATH + "/" + CODE + "/replacement")
        .then()
        .statusCode(200)
        .body(containsString("45"));
    // replacing a non-existent warehouse -> domain rejects -> 400
    given()
        .contentType("application/json")
        .body("{\"location\":\"ZWOLLE-002\",\"capacity\":10,\"stock\":1}")
        .when()
        .post(PATH + "/UNKNOWN-XYZ/replacement")
        .then()
        .statusCode(400);
  }

  @Test
  void archiveHandlesSuccessAlreadyArchivedAndUnknown() {
    createTestWarehouse();
    // first archive succeeds
    given().when().delete(PATH + "/" + CODE).then().statusCode(204);
    // archiving again: it still exists but is already archived -> 400
    given().when().delete(PATH + "/" + CODE).then().statusCode(400);
    // unknown code -> 404
    given().when().delete(PATH + "/UNKNOWN-XYZ").then().statusCode(404);
  }
}

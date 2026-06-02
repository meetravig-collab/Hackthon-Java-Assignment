package com.fulfilment.application.monolith.warehouses.adapters;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;

import com.fulfilment.application.monolith.warehouses.adapters.database.DbWarehouse;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for GET /warehouse/search.
 *
 * Verifies filtering by location and capacity, exclusion of archived warehouses,
 * sorting, and pagination using the full HTTP stack against an H2 in-memory DB.
 */
@QuarkusTest
public class WarehouseSearchIT {

  @Inject
  EntityManager em;

  @BeforeEach
  @Transactional
  public void setup() {
    em.createQuery("DELETE FROM DbWarehouse").executeUpdate();
    // 4 active warehouses
    persist("SRCH-001", "AMSTERDAM-001", 100, 50, LocalDateTime.of(2023, 1, 1, 0, 0), null);
    persist("SRCH-002", "AMSTERDAM-001", 60, 30, LocalDateTime.of(2022, 6, 15, 0, 0), null);
    persist("SRCH-003", "ZWOLLE-001", 40, 20, LocalDateTime.of(2024, 3, 20, 0, 0), null);
    persist("SRCH-004", "TILBURG-001", 30, 15, LocalDateTime.of(2021, 11, 10, 0, 0), null);
    // 1 archived warehouse — must never appear in search results
    persist("SRCH-005", "AMSTERDAM-001", 80, 40, LocalDateTime.of(2023, 8, 1, 0, 0), LocalDateTime.now());
  }

  @Test
  public void testSearchWithNoFiltersReturnsAllActive() {
    given()
        .when().get("/warehouse/search")
        .then()
        .statusCode(200)
        .body("items.size()", equalTo(4))
        .body("total", equalTo(4))
        .body("page", equalTo(0))
        .body("pageSize", equalTo(10));
  }

  @Test
  public void testSearchExcludesArchivedWarehouses() {
    given()
        .when().get("/warehouse/search")
        .then()
        .statusCode(200)
        .body("items.businessUnitCode", not(hasItem("SRCH-005")));
  }

  @Test
  public void testFilterByLocation() {
    given()
        .queryParam("location", "AMSTERDAM-001")
        .when().get("/warehouse/search")
        .then()
        .statusCode(200)
        .body("items.size()", equalTo(2))
        .body("total", equalTo(2))
        .body("items.location", everyItem(equalTo("AMSTERDAM-001")));
  }

  @Test
  public void testFilterByMinCapacity() {
    // SRCH-001 (100) and SRCH-002 (60) are >= 60
    given()
        .queryParam("minCapacity", 60)
        .when().get("/warehouse/search")
        .then()
        .statusCode(200)
        .body("items.size()", equalTo(2))
        .body("items.businessUnitCode", hasItem("SRCH-001"))
        .body("items.businessUnitCode", hasItem("SRCH-002"));
  }

  @Test
  public void testFilterByMaxCapacity() {
    // SRCH-003 (40) and SRCH-004 (30) are <= 40
    given()
        .queryParam("maxCapacity", 40)
        .when().get("/warehouse/search")
        .then()
        .statusCode(200)
        .body("items.size()", equalTo(2))
        .body("items.businessUnitCode", hasItem("SRCH-003"))
        .body("items.businessUnitCode", hasItem("SRCH-004"));
  }

  @Test
  public void testFilterByCapacityRange() {
    // SRCH-002 (60) is between 50 and 70
    given()
        .queryParam("minCapacity", 50)
        .queryParam("maxCapacity", 70)
        .when().get("/warehouse/search")
        .then()
        .statusCode(200)
        .body("items.size()", equalTo(1))
        .body("items[0].businessUnitCode", equalTo("SRCH-002"));
  }

  @Test
  public void testSortByCapacityDescending() {
    given()
        .queryParam("sortBy", "capacity")
        .queryParam("sortOrder", "desc")
        .when().get("/warehouse/search")
        .then()
        .statusCode(200)
        .body("items[0].businessUnitCode", equalTo("SRCH-001"))  // 100
        .body("items[1].businessUnitCode", equalTo("SRCH-002"))  // 60
        .body("items[2].businessUnitCode", equalTo("SRCH-003"))  // 40
        .body("items[3].businessUnitCode", equalTo("SRCH-004")); // 30
  }

  @Test
  public void testSortByCapacityAscending() {
    given()
        .queryParam("sortBy", "capacity")
        .queryParam("sortOrder", "asc")
        .when().get("/warehouse/search")
        .then()
        .statusCode(200)
        .body("items[0].businessUnitCode", equalTo("SRCH-004"))  // 30
        .body("items[1].businessUnitCode", equalTo("SRCH-003"))  // 40
        .body("items[2].businessUnitCode", equalTo("SRCH-002"))  // 60
        .body("items[3].businessUnitCode", equalTo("SRCH-001")); // 100
  }

  @Test
  public void testPaginationFirstPage() {
    given()
        .queryParam("sortBy", "capacity")
        .queryParam("sortOrder", "desc")
        .queryParam("page", 0)
        .queryParam("pageSize", 2)
        .when().get("/warehouse/search")
        .then()
        .statusCode(200)
        .body("items.size()", equalTo(2))
        .body("total", equalTo(4))
        .body("page", equalTo(0))
        .body("pageSize", equalTo(2))
        .body("items[0].businessUnitCode", equalTo("SRCH-001"))
        .body("items[1].businessUnitCode", equalTo("SRCH-002"));
  }

  @Test
  public void testPaginationSecondPage() {
    given()
        .queryParam("sortBy", "capacity")
        .queryParam("sortOrder", "desc")
        .queryParam("page", 1)
        .queryParam("pageSize", 2)
        .when().get("/warehouse/search")
        .then()
        .statusCode(200)
        .body("items.size()", equalTo(2))
        .body("total", equalTo(4))
        .body("page", equalTo(1))
        .body("items[0].businessUnitCode", equalTo("SRCH-003"))
        .body("items[1].businessUnitCode", equalTo("SRCH-004"));
  }

  @Test
  public void testCombinedLocationAndCapacityFilter() {
    // Amsterdam active: SRCH-001 (100), SRCH-002 (60); only SRCH-002 fits minCapacity=50, maxCapacity=80
    given()
        .queryParam("location", "AMSTERDAM-001")
        .queryParam("minCapacity", 50)
        .queryParam("maxCapacity", 80)
        .when().get("/warehouse/search")
        .then()
        .statusCode(200)
        .body("items.size()", equalTo(1))
        .body("items[0].businessUnitCode", equalTo("SRCH-002"));
  }

  private void persist(String code, String location, int capacity, int stock,
      LocalDateTime createdAt, LocalDateTime archivedAt) {
    DbWarehouse w = new DbWarehouse();
    w.businessUnitCode = code;
    w.location = location;
    w.capacity = capacity;
    w.stock = stock;
    w.createdAt = createdAt;
    w.archivedAt = archivedAt;
    em.persist(w);
  }
}
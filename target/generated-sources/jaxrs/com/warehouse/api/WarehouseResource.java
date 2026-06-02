package com.warehouse.api;

import com.warehouse.api.beans.Warehouse;
import com.warehouse.api.beans.WarehouseSearchResult;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import java.math.BigInteger;
import java.util.List;

/**
 * A JAX-RS interface. An implementation of this interface must be provided.
 */
@Path("/warehouse")
public interface WarehouseResource {
  @Path("/search")
  @GET
  @Produces("application/json")
  WarehouseSearchResult searchWarehouses(@QueryParam("location") String location,
      @QueryParam("minCapacity") BigInteger minCapacity, @QueryParam("maxCapacity") BigInteger maxCapacity,
      @QueryParam("sortBy") @DefaultValue("createdAt") String sortBy,
      @QueryParam("sortOrder") @DefaultValue("asc") String sortOrder,
      @QueryParam("page") @DecimalMin(value = "0", inclusive = true) @DefaultValue("0") BigInteger page,
      @QueryParam("pageSize") @DecimalMax(value = "100", inclusive = true) @DecimalMin(value = "1", inclusive = true) @DefaultValue("10") BigInteger pageSize);

  @GET
  @Produces("application/json")
  List<Warehouse> listAllWarehousesUnits();

  @POST
  @Produces("application/json")
  @Consumes("application/json")
  Warehouse createANewWarehouseUnit(@NotNull Warehouse data);

  @Path("/{id}")
  @GET
  @Produces("application/json")
  Warehouse getAWarehouseUnitByID(@PathParam("id") String id);

  @Path("/{id}")
  @DELETE
  void archiveAWarehouseUnitByID(@PathParam("id") String id);

  /**
   * <p>
   * Replaces the current active Warehouse identified by
   * <code>businessUnitCode</code> unit by a new Warehouse provided in the request
   * body A Warehouse can be replaced by another Warehouse with the same Business
   * Unit Code. That means that the previous Warehouse will be archived and the
   * new Warehouse will be created assuming its place.
   * </p>
   * 
   */
  @Path("/{businessUnitCode}/replacement")
  @POST
  @Produces("application/json")
  @Consumes("application/json")
  Warehouse replaceTheCurrentActiveWarehouse(@PathParam("businessUnitCode") String businessUnitCode,
      @NotNull Warehouse data);
}

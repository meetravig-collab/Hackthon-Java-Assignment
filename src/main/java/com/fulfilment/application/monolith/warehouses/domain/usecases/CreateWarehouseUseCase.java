package com.fulfilment.application.monolith.warehouses.domain.usecases;

import com.fulfilment.application.monolith.warehouses.domain.models.Location;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.CreateWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.LocationResolver;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

@ApplicationScoped
public class CreateWarehouseUseCase implements CreateWarehouseOperation {

  private static final Logger LOG = Logger.getLogger(CreateWarehouseUseCase.class);

  private final WarehouseStore warehouseStore;
  private final LocationResolver locationResolver;

  public CreateWarehouseUseCase(WarehouseStore warehouseStore, LocationResolver locationResolver) {
    this.warehouseStore = warehouseStore;
    this.locationResolver = locationResolver;
  }

  @Override
  public void create(Warehouse warehouse) {
    LOG.infof("Attempting to create warehouse '%s' at location='%s', capacity=%d, stock=%d",
        warehouse.businessUnitCode, warehouse.location, warehouse.capacity, warehouse.stock);

    Warehouse existing = warehouseStore.findByBusinessUnitCode(warehouse.businessUnitCode);
    if (existing != null) {
      LOG.warnf("Create failed — warehouse '%s' already exists", warehouse.businessUnitCode);
      throw new IllegalArgumentException(
          "Warehouse with business unit code '" + warehouse.businessUnitCode + "' already exists");
    }

    Location location = locationResolver.resolveByIdentifier(warehouse.location);
    if (location == null) {
      LOG.warnf("Create failed — location '%s' is not valid", warehouse.location);
      throw new IllegalArgumentException(
          "Location '" + warehouse.location + "' is not valid");
    }

    if (warehouse.capacity > location.maxCapacity()) {
      LOG.warnf("Create failed — capacity %d exceeds location '%s' max capacity %d",
          warehouse.capacity, warehouse.location, location.maxCapacity());
      throw new IllegalArgumentException(
          "Warehouse capacity (" + warehouse.capacity +
          ") exceeds location max capacity (" + location.maxCapacity() + ")");
    }

    if (warehouse.stock > warehouse.capacity) {
      LOG.warnf("Create failed — stock %d exceeds capacity %d", warehouse.stock, warehouse.capacity);
      throw new IllegalArgumentException(
          "Warehouse stock (" + warehouse.stock +
          ") exceeds warehouse capacity (" + warehouse.capacity + ")");
    }

    warehouse.createdAt = java.time.LocalDateTime.now();
    warehouseStore.create(warehouse);
    LOG.infof("Warehouse '%s' created successfully at %s", warehouse.businessUnitCode, warehouse.createdAt);
  }
}

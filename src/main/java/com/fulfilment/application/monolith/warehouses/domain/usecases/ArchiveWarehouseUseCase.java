package com.fulfilment.application.monolith.warehouses.domain.usecases;

import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.ArchiveWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

@ApplicationScoped
public class ArchiveWarehouseUseCase implements ArchiveWarehouseOperation {

  private static final Logger LOG = Logger.getLogger(ArchiveWarehouseUseCase.class);

  private final WarehouseStore warehouseStore;

  public ArchiveWarehouseUseCase(WarehouseStore warehouseStore) {
    this.warehouseStore = warehouseStore;
  }

  @Override
  public void archive(Warehouse warehouse) {
    LOG.infof("Attempting to archive warehouse '%s'", warehouse.businessUnitCode);

    Warehouse existing = warehouseStore.findByBusinessUnitCode(warehouse.businessUnitCode);
    if (existing == null) {
      LOG.warnf("Archive failed — warehouse '%s' does not exist", warehouse.businessUnitCode);
      throw new IllegalArgumentException(
          "Warehouse with business unit code '" + warehouse.businessUnitCode + "' does not exist");
    }

    if (existing.archivedAt != null) {
      LOG.warnf("Archive failed — warehouse '%s' is already archived at %s",
          warehouse.businessUnitCode, existing.archivedAt);
      throw new IllegalArgumentException(
          "Warehouse with business unit code '" + warehouse.businessUnitCode + "' is already archived");
    }

    existing.archivedAt = java.time.LocalDateTime.now();
    warehouseStore.update(existing);
    LOG.infof("Warehouse '%s' archived successfully at %s", warehouse.businessUnitCode, existing.archivedAt);
  }
}

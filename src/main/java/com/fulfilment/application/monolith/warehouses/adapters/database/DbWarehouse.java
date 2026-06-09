package com.fulfilment.application.monolith.warehouses.adapters.database;

import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "warehouse")
@Cacheable
public class DbWarehouse {

  @Id
  // Use the warehouse_seq sequence the schema/import.sql is built around (it seeds ids 1-3 and
  // does ALTER SEQUENCE warehouse_seq RESTART WITH 4). IDENTITY ignored that sequence, so generated
  // ids restarted at 1 and collided with the seeded rows; allocationSize=1 keeps ids contiguous.
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "warehouseSeqGen")
  @SequenceGenerator(name = "warehouseSeqGen", sequenceName = "warehouse_seq", allocationSize = 1)
  public Long id;
  
  @Version
  public Long version;

  @Column(unique = true, nullable = false)
  public String businessUnitCode;

  public String location;

  public Integer capacity;

  public Integer stock;

  public LocalDateTime createdAt;

  public LocalDateTime archivedAt;

  public DbWarehouse() {}

  public Warehouse toWarehouse() {
    var warehouse = new Warehouse();
    warehouse.businessUnitCode = this.businessUnitCode;
    warehouse.location = this.location;
    warehouse.capacity = this.capacity;
    warehouse.stock = this.stock;
    warehouse.createdAt = this.createdAt;
    warehouse.archivedAt = this.archivedAt;
    return warehouse;
  }
}

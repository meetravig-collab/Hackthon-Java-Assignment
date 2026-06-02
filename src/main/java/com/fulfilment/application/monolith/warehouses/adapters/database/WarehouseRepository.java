package com.fulfilment.application.monolith.warehouses.adapters.database;

import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;

@ApplicationScoped
public class WarehouseRepository implements WarehouseStore, PanacheRepository<DbWarehouse> {

  @Override
  public List<Warehouse> getAll() {
    return this.listAll().stream().map(DbWarehouse::toWarehouse).toList();
  }

  @Override
  public void create(Warehouse warehouse) {
    DbWarehouse dbWarehouse = new DbWarehouse();
    dbWarehouse.businessUnitCode = warehouse.businessUnitCode;
    dbWarehouse.location = warehouse.location;
    dbWarehouse.capacity = warehouse.capacity;
    dbWarehouse.stock = warehouse.stock;
    dbWarehouse.createdAt = warehouse.createdAt;
    dbWarehouse.archivedAt = warehouse.archivedAt;
    
    this.persist(dbWarehouse);
  }

  @Override
  public void update(Warehouse warehouse) {
    DbWarehouse dbWarehouse = find("businessUnitCode", warehouse.businessUnitCode).firstResult();
    if (dbWarehouse != null) {
      dbWarehouse.location = warehouse.location;
      dbWarehouse.capacity = warehouse.capacity;
      dbWarehouse.stock = warehouse.stock;
      dbWarehouse.archivedAt = warehouse.archivedAt;
      getEntityManager().flush();
    }
  }

  @Override
  public void remove(Warehouse warehouse) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'remove'");
  }

  @Override
  public Warehouse findByBusinessUnitCode(String buCode) {
    DbWarehouse dbWarehouse = find("businessUnitCode", buCode).firstResult();
    return dbWarehouse != null ? dbWarehouse.toWarehouse() : null;
  }


  @Override
  public List<Warehouse> search(String location, Integer minCapacity, Integer maxCapacity,
                                String sortBy, String sortOrder, int page, int pageSize) {
    StringBuilder jpql = new StringBuilder(
        "SELECT w FROM DbWarehouse w WHERE w.archivedAt IS NULL");
    if (location != null && !location.isBlank()) {
      jpql.append(" AND w.location = :location");
    }
    if (minCapacity != null) {
      jpql.append(" AND w.capacity >= :minCapacity");
    }
    if (maxCapacity != null) {
      jpql.append(" AND w.capacity <= :maxCapacity");
    }
    String col = "capacity".equals(sortBy) ? "w.capacity" : "w.createdAt";
    String ord = "desc".equalsIgnoreCase(sortOrder) ? "DESC" : "ASC";
    jpql.append(" ORDER BY ").append(col).append(" ").append(ord);

    var query = getEntityManager().createQuery(jpql.toString(), DbWarehouse.class);
    if (location != null && !location.isBlank()) {
      query.setParameter("location", location);
    }
    if (minCapacity != null) {
      query.setParameter("minCapacity", minCapacity);
    }
    if (maxCapacity != null) {
      query.setParameter("maxCapacity", maxCapacity);
    }
    int safeSize = Math.min(Math.max(pageSize, 1), 100);
    int safePage = Math.max(page, 0);
    query.setFirstResult(safePage * safeSize);
    query.setMaxResults(safeSize);
    return query.getResultList().stream().map(DbWarehouse::toWarehouse).toList();
  }

  @Override
  public long countSearch(String location, Integer minCapacity, Integer maxCapacity) {
    StringBuilder jpql = new StringBuilder(
        "SELECT COUNT(w) FROM DbWarehouse w WHERE w.archivedAt IS NULL");
    if (location != null && !location.isBlank()) {
      jpql.append(" AND w.location = :location");
    }
    if (minCapacity != null) {
      jpql.append(" AND w.capacity >= :minCapacity");
    }
    if (maxCapacity != null) {
      jpql.append(" AND w.capacity <= :maxCapacity");
    }
    var query = getEntityManager().createQuery(jpql.toString(), Long.class);
    if (location != null && !location.isBlank()) {
      query.setParameter("location", location);
    }
    if (minCapacity != null) {
      query.setParameter("minCapacity", minCapacity);
    }
    if (maxCapacity != null) {
      query.setParameter("maxCapacity", maxCapacity);
    }
    return query.getSingleResult();
  }
}

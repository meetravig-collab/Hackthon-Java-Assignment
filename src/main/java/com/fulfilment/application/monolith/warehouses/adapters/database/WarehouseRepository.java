package com.fulfilment.application.monolith.warehouses.adapters.database;

import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.util.List;
import org.jboss.logging.Logger;

@ApplicationScoped
public class WarehouseRepository implements WarehouseStore, PanacheRepository<DbWarehouse> {

  private static final Logger LOG = Logger.getLogger(WarehouseRepository.class);

  @Override
  public List<Warehouse> getAll() {
    LOG.debug("Fetching all warehouses");
    List<Warehouse> result = this.listAll().stream().map(DbWarehouse::toWarehouse).toList();
    LOG.debugf("Found %d warehouse(s)", result.size());
    return result;
  }

  @Override
  @Transactional
  public void create(Warehouse warehouse) {
    LOG.infof("Creating warehouse with businessUnitCode='%s', location='%s', capacity=%d",
        warehouse.businessUnitCode, warehouse.location, warehouse.capacity);
    DbWarehouse dbWarehouse = new DbWarehouse();
    dbWarehouse.businessUnitCode = warehouse.businessUnitCode;
    dbWarehouse.location = warehouse.location;
    dbWarehouse.capacity = warehouse.capacity;
    dbWarehouse.stock = warehouse.stock;
    dbWarehouse.createdAt = warehouse.createdAt;
    dbWarehouse.archivedAt = warehouse.archivedAt;

    this.persist(dbWarehouse);
    LOG.infof("Warehouse '%s' persisted successfully", warehouse.businessUnitCode);
  }

  @Override
  @Transactional
  public void update(Warehouse warehouse) {
    LOG.infof("Updating warehouse '%s'", warehouse.businessUnitCode);
    DbWarehouse dbWarehouse = find("businessUnitCode", warehouse.businessUnitCode).firstResult();
    if (dbWarehouse != null) {
      dbWarehouse.location = warehouse.location;
      dbWarehouse.capacity = warehouse.capacity;
      dbWarehouse.stock = warehouse.stock;
      dbWarehouse.archivedAt = warehouse.archivedAt;
      getEntityManager().flush();
      LOG.infof("Warehouse '%s' updated successfully", warehouse.businessUnitCode);
    } else {
      LOG.warnf("Update skipped — warehouse '%s' not found in database", warehouse.businessUnitCode);
    }
  }

  @Override
  @Transactional
  public void remove(Warehouse warehouse) {
    LOG.infof("Removing warehouse '%s'", warehouse.businessUnitCode);
    DbWarehouse dbWarehouse = find("businessUnitCode", warehouse.businessUnitCode).firstResult();
    if (dbWarehouse != null) {
      delete(dbWarehouse);
      LOG.infof("Warehouse '%s' removed successfully", warehouse.businessUnitCode);
    } else {
      LOG.warnf("Remove skipped — warehouse '%s' not found in database", warehouse.businessUnitCode);
    }
  }

  @Override
  @Transactional
  public Warehouse findByBusinessUnitCode(String buCode) {
    LOG.debugf("Looking up warehouse by businessUnitCode='%s'", buCode);
    DbWarehouse dbWarehouse = find("businessUnitCode", buCode).firstResult();
    if (dbWarehouse == null) {
      LOG.debugf("No warehouse found for businessUnitCode='%s'", buCode);
    }
    return dbWarehouse != null ? dbWarehouse.toWarehouse() : null;
  }


  @Override
  public List<Warehouse> search(String location, Integer minCapacity, Integer maxCapacity,
                                String sortBy, String sortOrder, int page, int pageSize) {
    LOG.debugf("Searching warehouses — location='%s', minCapacity=%s, maxCapacity=%s, sortBy='%s', sortOrder='%s', page=%d, pageSize=%d",
        location, minCapacity, maxCapacity, sortBy, sortOrder, page, pageSize);
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
    List<Warehouse> results = query.getResultList().stream().map(DbWarehouse::toWarehouse).toList();
    LOG.debugf("Search returned %d result(s)", results.size());
    return results;
  }

  @Override
  public long countSearch(String location, Integer minCapacity, Integer maxCapacity) {
    LOG.debugf("Counting warehouses — location='%s', minCapacity=%s, maxCapacity=%s",
        location, minCapacity, maxCapacity);
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
    long count = query.getSingleResult();
    LOG.debugf("Count result: %d", count);
    return count;
  }
}

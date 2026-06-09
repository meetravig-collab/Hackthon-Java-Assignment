package com.fulfilment.application.monolith.stores;

import org.junit.jupiter.api.Test;

/**
 * Directly drives {@link LegacyStoreManagerGateway} so its (temp-file) sync logic is covered
 * deterministically, independent of the asynchronous event delivery used in production.
 */
class LegacyStoreManagerGatewayTest {

  @Test
  void createAndUpdateWriteToLegacySystem() {
    LegacyStoreManagerGateway gateway = new LegacyStoreManagerGateway();

    Store store = new Store();
    store.name = "LegacyTestStore";
    store.quantityProductsInStock = 7;

    // Both paths funnel through writeToFile(); they should complete without throwing.
    gateway.createStoreOnLegacySystem(store);
    gateway.updateStoreOnLegacySystem(store);
  }
}

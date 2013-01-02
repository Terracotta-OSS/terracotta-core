/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.api;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.object.ObjectID;
import com.tc.objectserver.impl.PersistentManagedObjectStore;
import com.tc.objectserver.mgmt.ManagedObjectFacade;

public class ObjectStatsManagerImpl implements ObjectStatsManager {
  private final ObjectManagerMBean objectManager;
  private final PersistentManagedObjectStore objectStore;

  protected final static TCLogger  logger = TCLogging.getLogger(ObjectStatsManagerImpl.class);

  public ObjectStatsManagerImpl(ObjectManagerMBean manager, PersistentManagedObjectStore store) {
    this.objectManager = manager;
    this.objectStore = store;
  }

  @Override
  public String getObjectTypeFromID(ObjectID id) {
    if (!objectStore.containsObject(id)) { return null; }

    ManagedObjectFacade objFacade = null;
    try {
      objFacade = objectManager.lookupFacade(id, 0);
    } catch (Exception e) {
      logger.info("Ignoring exception while fetching lock type", e);
      return "";
    }
    return objFacade.getClassName();
  }
}

/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.api;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.object.ObjectID;
import com.tc.objectserver.mgmt.ManagedObjectFacade;
import com.tc.objectserver.persistence.api.ManagedObjectStore;

public class ObjectStatsManagerImpl implements ObjectStatsManager {
  private final ObjectManagerMBean objectManager;
  private final ManagedObjectStore objectStore;

  protected final static TCLogger  logger = TCLogging.getLogger(ObjectStatsManagerImpl.class);

  public ObjectStatsManagerImpl(ObjectManagerMBean manager, ManagedObjectStore store) {
    this.objectManager = manager;
    this.objectStore = store;
  }

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

package com.tc.stats;

import com.tc.management.TerracottaMBean;
import com.tc.object.ObjectID;
import com.tc.objectserver.api.NoSuchObjectException;
import com.tc.objectserver.lockmanager.api.DeadlockChain;
import com.tc.objectserver.lockmanager.api.LockMBean;
import com.tc.objectserver.mgmt.ManagedObjectFacade;

import javax.management.ObjectName;

/**
 * This describes the management interface for the DSO subsystem. It's envisioned that this acts as a top-level object
 * aggregating statistical, configuration, and operational child interfaces.
 */

public interface DSOMBean extends DSOStats, TerracottaMBean {

  DSOStats getStats();

  static final String GC_COMPLETED = "dso.gc.completed";

  static final String ROOT_ADDED   = "dso.root.added";

  ObjectName[] getRoots();

  LockMBean[] getLocks();

  static final String CLIENT_ATTACHED = "dso.client.attached";
  static final String CLIENT_DETACHED = "dso.client.detached";

  ObjectName[] getClients();

  DSOClassInfo[] getClassInfo();

  DeadlockChain[] scanForDeadLocks();

  ManagedObjectFacade lookupFacade(ObjectID objectID, int limit) throws NoSuchObjectException;

}

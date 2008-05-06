/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.stats;

import com.tc.management.TerracottaMBean;
import com.tc.object.ObjectID;
import com.tc.objectserver.api.NoSuchObjectException;
import com.tc.objectserver.lockmanager.api.LockMBean;
import com.tc.objectserver.mgmt.ManagedObjectFacade;
import com.tc.statistics.StatisticData;
import com.tc.stats.counter.Counter;
import com.tc.stats.statistics.CountStatistic;

import java.util.Map;

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

  Map<String, Counter> getAllPendingRequests();
  
  Map<ObjectName, CountStatistic> getClientTransactionRates();
  
  Map<ObjectName, StatisticData[]> getL1CpuUsages();
  
  ManagedObjectFacade lookupFacade(ObjectID objectID, int limit) throws NoSuchObjectException;

}

/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.stats;

import com.tc.management.TerracottaMBean;
import com.tc.net.groups.NodeID;
import com.tc.object.ObjectID;
import com.tc.objectserver.api.GCStats;
import com.tc.objectserver.api.NoSuchObjectException;
import com.tc.objectserver.lockmanager.api.LockMBean;
import com.tc.objectserver.mgmt.ManagedObjectFacade;
import com.tc.statistics.StatisticData;
import com.tc.stats.statistics.CountStatistic;

import java.util.Map;

import javax.management.ObjectName;

/**
 * This describes the management interface for the DSO subsystem. It's envisioned that this acts as a top-level object
 * aggregating statistical, configuration, and operational child interfaces.
 */

public interface DSOMBean extends DSOStats, TerracottaMBean {

  DSOStats getStats();

  static final String GC_STATUS_UPDATE = "dso.gc.status.update";

  static final String ROOT_ADDED   = "dso.root.added";

  ObjectName[] getRoots();

  LockMBean[] getLocks();

  static final String CLIENT_ATTACHED = "dso.client.attached";
  static final String CLIENT_DETACHED = "dso.client.detached";

  ObjectName[] getClients();

  DSOClassInfo[] getClassInfo();

  Map<ObjectName, CountStatistic> getAllPendingTransactionsCount();
  
  Map<ObjectName, CountStatistic> getClientTransactionRates();
  
  Map<ObjectName, StatisticData[]> getL1CpuUsages();
  
  Map<ObjectName, Map> getL1Statistics();
  
  Map<ObjectName, Map> getPrimaryClientStatistics();
  
  ManagedObjectFacade lookupFacade(ObjectID objectID, int limit) throws NoSuchObjectException;

  Map<ObjectName, Integer> getClientLiveObjectCount();
  
  int getLiveObjectCount();
  
  boolean isResident(NodeID node, ObjectID oid);
  
  public GCStats[] getGarbageCollectorStats();
}

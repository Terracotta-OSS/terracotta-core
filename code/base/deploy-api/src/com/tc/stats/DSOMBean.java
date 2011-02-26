/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.stats;

import com.tc.management.TerracottaMBean;
import com.tc.net.NodeID;
import com.tc.object.ObjectID;
import com.tc.objectserver.api.NoSuchObjectException;
import com.tc.objectserver.locks.LockMBean;
import com.tc.objectserver.mgmt.ManagedObjectFacade;
import com.tc.objectserver.storage.api.OffheapStats;
import com.tc.operatorevent.TerracottaOperatorEvent;
import com.tc.statistics.StatisticData;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.management.ObjectName;

/**
 * This describes the management interface for the DSO subsystem. It's envisioned that this acts as a top-level object
 * aggregating statistical, configuration, and operational child interfaces.
 */

public interface DSOMBean extends DSOStats, OffheapStats, DGCMBean, TerracottaMBean {

  DSOStats getStats();

  static final String GC_STATUS_UPDATE = "dso.gc.status.update";

  static final String ROOT_ADDED       = "dso.root.added";

  ObjectName[] getRoots();

  LockMBean[] getLocks();

  static final String CLIENT_ATTACHED = "dso.client.attached";
  static final String CLIENT_DETACHED = "dso.client.detached";

  ObjectName[] getClients();

  DSOClassInfo[] getClassInfo();

  Map<ObjectName, Long> getAllPendingTransactionsCount();

  long getPendingTransactionsCount();

  Map<ObjectName, Long> getClientTransactionRates();

  Map<ObjectName, StatisticData[]> getL1CpuUsages();

  Map<ObjectName, Map> getL1Statistics();

  Map<ObjectName, Map> getPrimaryClientStatistics();

  ManagedObjectFacade lookupFacade(ObjectID objectID, int limit) throws NoSuchObjectException;

  Map<ObjectName, Integer> getClientLiveObjectCount();

  List<TerracottaOperatorEvent> getOperatorEvents();

  int getLiveObjectCount();

  int getCachedObjectCount();

  boolean isResident(NodeID node, ObjectID oid);

  Map<ObjectName, Exception> setAttribute(Set<ObjectName> onSet, String attrName, Object attrValue);

  Map<ObjectName, Exception> setAttribute(String attrName, Map<ObjectName, Object> attrMap);

  Map<ObjectName, Map<String, Object>> getAttributeMap(Map<ObjectName, Set<String>> attributeMap, long timeout,
                                                       TimeUnit unit);

  Map<ObjectName, Object> invoke(Set<ObjectName> onSet, String operation, long timeout, TimeUnit unit);

  Map<ObjectName, Object> invoke(Set<ObjectName> onSet, String operation, long timeout, TimeUnit unit, Object[] args,
                                 String[] sigs);

  Map<ObjectName, Long> getServerMapGetSizeRequestsCount();

  Map<ObjectName, Long> getServerMapGetValueRequestsCount();

  Map<ObjectName, Long> getServerMapGetSizeRequestsRate();

  Map<ObjectName, Long> getServerMapGetValueRequestsRate();

  void optimizeSearchIndex(String indexName);

  String[] getSearchIndexNames();
}

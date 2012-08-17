/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.express.tests;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.management.ObjectName;

/**
 * This is a stripped down copy of DSOMBean to break core dependencies
 */
public interface DSOMBean {

  ObjectName[] getRoots();

  ObjectName[] getClients();

  Map<ObjectName, Long> getAllPendingTransactionsCount();

  long getPendingTransactionsCount();

  Map<ObjectName, Long> getClientTransactionRates();

  Map<ObjectName, Map> getL1Statistics();

  Map<ObjectName, Map> getPrimaryClientStatistics();

  Map<ObjectName, Integer> getClientLiveObjectCount();

  int getLiveObjectCount();

  int getCachedObjectCount();

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

  int getActiveLicensedClientCount();

  int getLicensedClientHighCount();
}

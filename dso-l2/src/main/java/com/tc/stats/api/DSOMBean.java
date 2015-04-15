/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
 */
package com.tc.stats.api;

import com.tc.management.RemoteManagement;
import com.tc.management.TerracottaMBean;
import com.tc.net.NodeID;
import com.tc.object.ObjectID;
import com.tc.objectserver.locks.LockMBean;
import com.tc.objectserver.storage.api.OffheapStats;
import com.tc.objectserver.storage.api.StorageDataStats;
import com.tc.operatorevent.TerracottaOperatorEvent;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.management.ObjectName;

/**
 * This describes the management interface for the DSO subsystem. It's envisioned that this acts as a top-level object
 * aggregating statistical, configuration, and operational child interfaces.
 */

public interface DSOMBean extends DSOStats, OffheapStats, StorageDataStats, DGCMBean, TerracottaMBean {

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

  Map<ObjectName, Map> getL1Statistics();

  Map<ObjectName, Map> getPrimaryClientStatistics();

  Map<ObjectName, Integer> getClientLiveObjectCount();

  List<TerracottaOperatorEvent> getOperatorEvents();

  /**
   * This method returns operator events which have occurred between a particular time and the current time.
   * @param sinceTimestamp the time since which the operator events need to be fetched
   * @return list of TerracottaOperatorEvents between the date represented by "sinceTimestamp" and now.
   */
  List<TerracottaOperatorEvent> getOperatorEvents(long sinceTimestamp);

  /**
   * Mark an operator event as read or unread.
   * @param operatorEvent the event to mark
   * @param read true if the event should be marked as read, false if it should be marked as unread.
   * @return true if the event was found and marked, false otherwise.
   */
  boolean markOperatorEvent(TerracottaOperatorEvent operatorEvent, boolean read);

  int getLiveObjectCount();

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

  int getActiveLicensedClientCount();

  int getLicensedClientHighCount();

  Map<String, Integer> getUnreadOperatorEventCount();

  RemoteManagement getRemoteManagement();

}

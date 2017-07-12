/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Terracotta Core.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
 */
package com.tc.stats.api;

import com.tc.management.RemoteManagement;
import com.tc.management.TerracottaMBean;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.management.ObjectName;

/**
 * This describes the management interface for the DSO subsystem. It's envisioned that this acts as a top-level object
 * aggregating statistical, configuration, and operational child interfaces.
 */

public interface DSOMBean extends Stats, TerracottaMBean {

  Stats getStats();

  static final String CLIENT_ATTACHED = "dso.client.attached";
  static final String CLIENT_DETACHED = "dso.client.detached";

  ObjectName[] getClients();

  ClassInfo[] getClassInfo();

  Map<ObjectName, Long> getAllPendingTransactionsCount();

  long getPendingTransactionsCount();

  Map<ObjectName, Long> getClientTransactionRates();

  Map<ObjectName, Map<String, Object>> getL1Statistics();

  Map<ObjectName, Map<String, Object>> getPrimaryClientStatistics();

  Map<ObjectName, Integer> getClientLiveObjectCount();

  int getLiveObjectCount();

  Map<ObjectName, Exception> setAttribute(Set<ObjectName> onSet, String attrName, Object attrValue);

  Map<ObjectName, Exception> setAttribute(String attrName, Map<ObjectName, Object> attrMap);

  Map<ObjectName, Map<String, Object>> getAttributeMap(Map<ObjectName, Set<String>> attributeMap, long timeout,
                                                       TimeUnit unit);

  Map<ObjectName, Object> invoke(Set<ObjectName> onSet, String operation, long timeout, TimeUnit unit);

  Map<ObjectName, Object> invoke(Set<ObjectName> onSet, String operation, long timeout, TimeUnit unit, Object[] args,
                                 String[] sigs);

  int getActiveLicensedClientCount();

  int getLicensedClientHighCount();

  RemoteManagement getRemoteManagement();

}

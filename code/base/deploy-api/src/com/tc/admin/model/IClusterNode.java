/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin.model;

import com.tc.object.ObjectID;
import com.tc.statistics.StatisticData;

import java.util.Map;
import java.util.Set;

import javax.management.ObjectName;

public interface IClusterNode extends IClusterModelElement, ILiveObjectCountProvider {
  static final String POLLED_ATTR_CPU_USAGE         = "CpuUsage";
  static final String POLLED_ATTR_USED_MEMORY       = "UsedMemory";
  static final String POLLED_ATTR_MAX_MEMORY        = "MaxMemory";
  static final String POLLED_ATTR_OBJECT_FLUSH_RATE = "ObjectFlushRate";
  static final String POLLED_ATTR_OBJECT_FAULT_RATE = "ObjectFaultRate";
  static final String POLLED_ATTR_TRANSACTION_RATE  = "TransactionRate";
  static final String POLLED_ATTR_LIVE_OBJECT_COUNT = "LiveObjectCount";

  String getProductVersion();

  String getProductPatchLevel();

  String getProductPatchVersion();

  String getProductBuildID();

  String getProductLicense();

  String getProductCopyright();

  boolean isReady();

  String getHost();

  int getPort();

  String getEnvironment();

  String getConfig();

  long getTransactionRate();

  StatisticData[] getCpuUsage();

  String[] getCpuStatNames();

  Map getPrimaryStatistics();

  String takeThreadDump(long moment);

  int getLiveObjectCount();

  boolean isResident(ObjectID oid);

  PolledAttribute getPolledAttribute(String name);

  void addPolledAttributeListener(String name, PolledAttributeListener listener);

  void addPolledAttributeListener(Set<String> names, PolledAttributeListener listener);

  void addPolledAttributeListener(PolledAttribute polledAttribute, PolledAttributeListener listener);

  void addPolledAttributeListener(ObjectName objectName, Set<String> attributeSet, PolledAttributeListener listener);

  void removePolledAttributeListener(String name, PolledAttributeListener listener);

  void removePolledAttributeListener(Set<String> names, PolledAttributeListener listener);

  void removePolledAttributeListener(PolledAttribute polledAttribute, PolledAttributeListener listener);

  void removePolledAttributeListener(ObjectName objectName, Set<String> attributeSet, PolledAttributeListener listener);

  Map<ObjectName, Set<String>> getPolledAttributes();

  Set<PolledAttributeListener> getPolledAttributeListeners();

  void tearDown();

  String dump();
}

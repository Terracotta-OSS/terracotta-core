/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin.model;

import com.tc.object.ObjectID;
import com.tc.statistics.StatisticData;

import java.beans.PropertyChangeListener;
import java.util.Map;

public interface IClusterNode {
  static final String PROP_READY = "ready";

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

  void addPropertyChangeListener(PropertyChangeListener listener);

  void removePropertyChangeListener(PropertyChangeListener listener);
}

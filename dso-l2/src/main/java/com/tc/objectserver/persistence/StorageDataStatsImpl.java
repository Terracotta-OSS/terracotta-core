/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.persistence;

import org.terracotta.corestorage.monitoring.MonitoredResource;

import com.tc.objectserver.storage.api.StorageData;
import com.tc.objectserver.storage.api.StorageDataStats;

import java.util.HashMap;
import java.util.Map;

public class StorageDataStatsImpl implements StorageDataStats {
  public static final long               serialVersionUID = 1L;

  private final MonitoredResource        monitoredResource;
  private final Map<String, StorageData> storageStats     = new HashMap<String, StorageData>();

  public StorageDataStatsImpl(MonitoredResource monitoredResource) {
    this.monitoredResource = monitoredResource;
  }

  @Override
  public Map<String, StorageData> getStorageStats() {
    return storageStats;
  }

  // XXX: this getter is just temp workaround for Eclipse warning of unused variable
  public MonitoredResource getMonitoredResource() {
    return monitoredResource;
  }

}

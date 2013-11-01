/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.persistence;

import org.terracotta.corestorage.monitoring.MonitoredResource;

import com.tc.objectserver.storage.api.StorageData;
import com.tc.objectserver.storage.api.StorageDataStats;
import java.util.Collection;

import java.util.HashMap;
import java.util.Map;

public class StorageDataStatsImpl implements StorageDataStats {
  public static final long               serialVersionUID = 1L;

  private final Collection<MonitoredResource>        monitoredResource;

  public StorageDataStatsImpl(Collection<MonitoredResource> monitoredResource) {
    this.monitoredResource = monitoredResource;
  }

  @Override
  public Map<String, StorageData> getStorageStats() {
    Map<String, StorageData> stats = new HashMap<String, StorageData>();
    for ( MonitoredResource mr : monitoredResource ) {
      stats.put(mr.getType().toString(), new StorageData(mr.getTotal() ,mr.getReserved(),mr.getUsed()));
    }
    return stats;
  }

  // XXX: this getter is just temp workaround for Eclipse warning of unused variable
  public Collection<MonitoredResource> getMonitoredResources() {
    return monitoredResource;
  }

}

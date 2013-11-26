/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.persistence;

import org.terracotta.corestorage.monitoring.MonitoredResource;

import com.tc.objectserver.storage.api.StorageDataStats;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class StorageDataStatsImpl implements StorageDataStats {
  public static final long                    serialVersionUID = 1L;

  private final Collection<MonitoredResource> monitoredResource;

  public StorageDataStatsImpl(Collection<MonitoredResource> monitoredResource) {
    this.monitoredResource = monitoredResource;
  }

  @Override
  public Map<String, Map<String, Long>> getStorageStats() {
    Map<String, Map<String, Long>> stats = new HashMap<String, Map<String, Long>>();
    for (MonitoredResource mr : monitoredResource) {
      stats.put(mr.getType().toString(), toMap(mr.getTotal(), mr.getReserved(), mr.getVital()));
    }
    return stats;
  }

  private static Map<String, Long> toMap(long total, long reserved, long used) {
    Map<String, Long> map = new HashMap<String, Long>();
    map.put("max", Long.valueOf(total));
    map.put("reserved", Long.valueOf(reserved));
    map.put("used", Long.valueOf(used));
    return Collections.unmodifiableMap(map);
  }

  // XXX: this getter is just temp workaround for Eclipse warning of unused variable
  public Collection<MonitoredResource> getMonitoredResources() {
    return monitoredResource;
  }

}

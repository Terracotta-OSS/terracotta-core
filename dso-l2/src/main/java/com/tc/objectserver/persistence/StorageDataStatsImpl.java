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

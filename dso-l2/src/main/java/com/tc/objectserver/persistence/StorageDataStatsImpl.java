/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.persistence;

import static java.util.concurrent.TimeUnit.NANOSECONDS;

import org.terracotta.corestorage.monitoring.MonitoredResource;

import com.tc.objectserver.storage.api.StorageData;
import com.tc.objectserver.storage.api.StorageDataStats;
import com.tc.text.PrettyPrintable;
import com.tc.text.PrettyPrinter;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class StorageDataStatsImpl implements StorageDataStats, PrettyPrintable {
  public static final long               serialVersionUID = 1L;

  private final MonitoredResource        monitoredResource;
  private final Map<String, StorageData> storageStats     = new HashMap<String, StorageData>();

  private static final long              REFRESH_INTERVAL = 10000;

  private final ExecutorService          executorService  = Executors.newCachedThreadPool();
  private final AtomicBoolean            refreshing       = new AtomicBoolean();

  private long                           lastRefreshTime  = System.nanoTime();

  private volatile long                  usedSize         = 0;

  public StorageDataStatsImpl(MonitoredResource monitoredResource) {
    this.monitoredResource = monitoredResource;
  }

  @Override
  public Map<String, StorageData> getStorageStats() {
    collectStats();
    return storageStats;
  }

  // XXX: this getter is just temp workaround for Eclipse warning of unused variable
  public MonitoredResource getMonitoredResource() {
    return monitoredResource;
  }

  private void collectStats() {
    // offheap
    StorageData offheapStorageData = new StorageData();
    if (isOffheapResource()) {
      offheapStorageData.setMaxSize(monitoredResource.getTotal());
      offheapStorageData.setReservedSize(monitoredResource.getReserved());
      refreshUsedSizeIfNecessary();
      offheapStorageData.setUsedSize(usedSize);
    }

    storageStats.put("offheap", offheapStorageData);
  }

  private void refreshUsedSizeIfNecessary() {
    if (!refreshing.get() && NANOSECONDS.toMillis(System.nanoTime() - lastRefreshTime) > REFRESH_INTERVAL) {
      if (refreshing.compareAndSet(false, true)) {
        executorService.submit(new Runnable() {
          @Override
          public void run() {
            try {
              usedSize = monitoredResource.getUsed();
              lastRefreshTime = System.nanoTime();
            } finally {
              refreshing.set(false);
            }
          }
        });
      }
    }
  }

  private boolean isOffheapResource() {
    return monitoredResource.getType() == MonitoredResource.Type.OFFHEAP;
  }

  @Override
  public PrettyPrinter prettyPrint(PrettyPrinter out) {
    out.flush();
    out.println("Storage data Stats:");
    // TODO: fill out these stats in readable format
    return out;
  }
}

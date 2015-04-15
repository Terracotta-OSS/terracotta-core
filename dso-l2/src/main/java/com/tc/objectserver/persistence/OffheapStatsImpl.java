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

import com.tc.objectserver.storage.api.OffheapStats;
import com.tc.text.PrettyPrintable;
import com.tc.text.PrettyPrinter;
import com.tc.util.Conversion;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.util.concurrent.TimeUnit.NANOSECONDS;

public class OffheapStatsImpl implements OffheapStats, PrettyPrintable {
  public static final long                        serialVersionUID = 1L;
  private static final long REFRESH_INTERVAL = 10000;

  private final MonitoredResource monitoredResource;
  private final ExecutorService executorService = Executors.newCachedThreadPool();
  private final AtomicBoolean refreshing = new AtomicBoolean();

  private long lastRefreshTime = System.nanoTime();

  private volatile long usedSize = 0;


  public OffheapStatsImpl(final MonitoredResource monitoredResource) {
    this.monitoredResource = monitoredResource;
  }

  @Override
  public long getOffheapMaxSize() {
    if (isOffheapResource()) {
      return monitoredResource.getTotal();
    } else {
      return 0L;
    }
  }

  @Override
  public long getOffheapReservedSize() {
    if (isOffheapResource()) {
      return monitoredResource.getReserved();
    } else {
      return 0L;
    }
  }

  @Override
  public long getOffheapUsedSize() {
    if (isOffheapResource()) {
      refreshUsedSizeIfNecessary();
      return usedSize;
    } else {
      return 0L;
    }
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
    out.println("OffHeap Stats:");
    try {
      out.println("OffHeap Max: " + Conversion.memoryBytesAsSize(getOffheapMaxSize()));
    } catch (Exception e) {
      out.println("OffHeap Max: " + getOffheapMaxSize());
    }
    try {
      out.println("OffHeap Used: " + Conversion.memoryBytesAsSize(getOffheapUsedSize()));
    } catch (Exception e) {
      out.println("OffHeap Used: " + getOffheapUsedSize());
    }
    try {
      out.println("OffHeap Reserved: " + Conversion.memoryBytesAsSize(getOffheapReservedSize()));
    } catch (Exception e) {
      out.println("OffHeap Reserved: " + getOffheapReservedSize());
    }
    out.flush();
    return out;
  }

}

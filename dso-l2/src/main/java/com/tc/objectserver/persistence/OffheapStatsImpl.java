/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.persistence;

import org.terracotta.corestorage.monitoring.MonitoredResource;

import com.tc.objectserver.storage.api.OffheapStats;
import com.tc.text.PrettyPrintable;
import com.tc.text.PrettyPrinter;
import com.tc.util.Conversion;

public class OffheapStatsImpl implements OffheapStats, PrettyPrintable {
  public static final long                        serialVersionUID = 1L;

  private transient final MonitoredResource monitoredResource;

  public OffheapStatsImpl(final MonitoredResource monitoredResource) {
    this.monitoredResource = monitoredResource;
  }

  public long getOffheapMaxSize() {
    if (monitoredResource.getType() == MonitoredResource.Type.OFFHEAP) {
      return monitoredResource.getTotal();
    } else {
      return 0L;
    }
  }

  public long getOffheapReservedSize() {
    if (monitoredResource.getType() == MonitoredResource.Type.OFFHEAP) {
      return monitoredResource.getReserved();
    } else {
      return 0L;
    }
  }

  public long getOffheapUsedSize() {
    if (monitoredResource.getType() == MonitoredResource.Type.OFFHEAP) {
      return monitoredResource.getUsed();
    } else {
      return 0L;
    }
  }

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

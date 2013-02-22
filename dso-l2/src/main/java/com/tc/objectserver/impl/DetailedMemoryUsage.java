/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.impl;

import com.tc.runtime.MemoryUsage;
import org.terracotta.corestorage.monitoring.MonitoredResource;

/**
 *
 * @author mscott
 */
public class DetailedMemoryUsage implements MemoryUsage {
  private final MonitoredResource rsrc;
  private final long max;
  private final long reserved;
  private long cacheUsed = -1;
  private final long count;

    public DetailedMemoryUsage(MonitoredResource rsrc, long count) {
        this.rsrc = rsrc;
        this.max = rsrc.getTotal();
        this.reserved = rsrc.getReserved();
        this.count = count;
    }
    
    private long checkUsed() {
        if ( cacheUsed < 0 ) {
            cacheUsed = rsrc.getUsed();
        }
        return cacheUsed;
    }
    
    public long getReservedMemory() {
        return reserved;
    }

    @Override
    public long getFreeMemory() {
        return max - reserved;
    }

    @Override
    public String getDescription() {
        return rsrc.getType().toString();
    }

    @Override
    public long getMaxMemory() {
        return max;
    }

    @Override
    public long getUsedMemory() {
        return checkUsed();
    }

    @Override
    public int getUsedPercentage() {
        return Math.round((reserved*100f)/max);
    }

    @Override
    public long getCollectionCount() {
        return count;
    }

    @Override
    public long getCollectionTime() {
        return 0;
    }

    @Override
    public String toString() {
        return "DetailedMemoryUsage{" + "rsrc=" + rsrc + ", max=" + max + ", reserved=" + reserved + ", used=" + cacheUsed + '}';
    }
    
    
           
}

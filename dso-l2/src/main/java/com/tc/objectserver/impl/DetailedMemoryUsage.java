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
            cacheUsed = rsrc.getVital();
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

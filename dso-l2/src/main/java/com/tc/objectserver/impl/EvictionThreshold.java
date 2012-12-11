/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.impl;

import java.util.Arrays;
import java.util.Comparator;
import org.terracotta.corestorage.monitoring.MonitoredResource;

/**
 *
 * @author mscott
 */
public enum EvictionThreshold {
    
    HEAP("HEAP",1l * 1024 * 1024 * 1024, 128l * 1024 * 1024,64l * 1024 * 1024),
    MICRO("INCREASE OFFHEAP TO OVER 1G IF POSSIBLE",512l * 1024 * 1024, 128l * 1024 * 1024,96l * 1024 * 1024),
    SMALL("2G",2l * 1024 * 1024 * 1024, 384l * 1024 * 1024,256l * 1024 * 1024),
    SMALL_MEDIUM("8G",8l * 1024 * 1024 * 1024,1l * 1024 * 1024 * 1024,512l * 1024 * 1024),
    MEDIUM("64G",64l * 1024 * 1024 * 1024,4l * 1024 * 1024 * 1024,2l * 1024 * 1024 * 1024),
    LARGE("BIG MEMORY",Long.MAX_VALUE * 1024 * 1024 * 1024, 16l * 1024 * 1024 * 1024,8l * 1024 * 1024 * 1024);
    
    private final String name;
    private final long maxSize;
    private final long used;
    private final long reserved;
        
    EvictionThreshold(String name,long maxSize, long used, long reserved) {
        this.maxSize = maxSize;
        this.name = name;
        this.used = used;
        this.reserved = reserved;
    }    
    
    public static EvictionThreshold configure(MonitoredResource usage) {
        if ( usage.getType() == MonitoredResource.Type.HEAP ) {
            return HEAP;
        }
        
        EvictionThreshold[] list = EvictionThreshold.values();
        Arrays.sort(list, new Comparator<EvictionThreshold> () {

            @Override
            public int compare(EvictionThreshold t, EvictionThreshold t1) {
                if ( t.maxSize == t.maxSize ) {
                    return 0;
                }
                return (t.maxSize > t1.maxSize) ? -1 : 1;
            }
            
        });
        for ( EvictionThreshold et : list ) {
            if ( et.maxSize >= usage.getTotal() && et != HEAP ) {
                return et;
            }
        }
        return LARGE;
    }
    
    public static void main(String[] args) {
        EvictionThreshold[] list = EvictionThreshold.values();
        Arrays.sort(list, new Comparator<EvictionThreshold> () {

            @Override
            public int compare(EvictionThreshold t, EvictionThreshold t1) {
                if ( t.maxSize == t.maxSize ) {
                    return 0;
                }
                return (t.maxSize > t1.maxSize) ? -1 : 1;
            }
            
        });
        for ( EvictionThreshold et : list ) {
            System.out.println(et);
        }
    }
    
    public boolean shouldThrottle(DetailedMemoryUsage usage) {
        if ( usage.getReservedMemory() > usage.getMaxMemory() - reserved) {
            return true;
        }
        return false;
    }
    
    public boolean shouldNormalize(DetailedMemoryUsage usage) {
        if ( usage.getReservedMemory() < usage.getMaxMemory() - reserved - (used - reserved)/2 ) {
            return true;
        }
        return false;
    }
    
    public boolean isAboveThreshold(DetailedMemoryUsage usage) {
        long max = usage.getMaxMemory();
        long reserve = usage.getReservedMemory();
        if ( usage.getReservedMemory() > max - reserved ) {
            return true;
        }
        if ( reserve > max - used && usage.getUsedMemory() > max - used ) {
            return true;
        }
        return false;
    }

    @Override
    public String toString() {
        return "EvictionThreshold{" + "name=" + name + ", max=" + maxSize + ", used=" + used + ", reserved=" + reserved + '}';
    }
}

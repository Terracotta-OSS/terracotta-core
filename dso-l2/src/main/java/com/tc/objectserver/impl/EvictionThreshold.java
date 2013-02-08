/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.impl;

import org.terracotta.corestorage.monitoring.MonitoredResource;

import java.util.Arrays;
import java.util.Comparator;

/**
 *
 * @author mscott
 */
public enum EvictionThreshold {
    
    HEAP("HEAP",1l * 1024 * 1024 * 1024, 64l * 1024 * 1024,32l * 1024 * 1024),
    MICRO("INCREASE OFFHEAP TO OVER 1G IF POSSIBLE",512l * 1024 * 1024, 128l * 1024 * 1024,32l * 1024 * 1024),
    SMALL("1G",1l * 1024 * 1024 * 1024, 256l * 1024 * 1024, 64l * 1024 * 1024),
    TWO("2G",2l * 1024 * 1024 * 1024, 512l * 1024 * 1024, 96l * 1024 * 1024),
    EIGHT("8G",8l * 1024 * 1024 * 1024, 1024l * 1024 * 1024, 128l * 1024 * 1024),
    SIXTEEN("16G",16l * 1024 * 1024 * 1024, 2048l * 1024 * 1024, 256l * 1024 * 1024),
    THIRTYTWO("32G",32l * 1024 * 1024 * 1024, 3584l * 1024 * 1024, 512l * 1024 * 1024),
    SIXTYFOUR("64G",64l * 1024 * 1024 * 1024, 8l * 1024 * 1024 * 1024, 1l * 1024 * 1024 * 1024),
    ONETWENTYEIGHT("128G",128l * 1024 * 1024 * 1024, 16l * 1024 * 1024 * 1024, 2l * 1024 * 1024 * 1024),
    BIG("BIG MEMORY",Long.MAX_VALUE, 32l * 1024 * 1024 * 1024, 4l * 1024 * 1024 * 1024);
    
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
        return BIG;
    }
    
    public String log(int usedTweak, int reservedTweak) {
        long lres = getReserved(reservedTweak);
        long lused = getUsed(lres,usedTweak);
        return "used:" + lused + ",reserved:" + lres;
    }
    
    public boolean shouldThrottle(DetailedMemoryUsage usage,int usedTweak,int reservedTweak) {
        long reserve = getReserved(reservedTweak);
        // long used = getUsed(reserve, usedTweak);
        if ( usage.getReservedMemory() > usage.getMaxMemory() - (reserve) ) {
            return true;
        }
        return false;
    }
    
    public boolean shouldNormalize(DetailedMemoryUsage usage,int usedTweak,int reservedTweak)  {
        long lres = getReserved(reservedTweak);
        long lused = getUsed(lres,usedTweak);
        if ( usage.getReservedMemory() < usage.getMaxMemory() - lres - ((lused - lres)/2) ) {
            return true;
        }
        return false;
    }
    
    public boolean isInThresholdRegion(DetailedMemoryUsage usage,int usedTweak,int reservedTweak)  {
        long max = usage.getMaxMemory();
        long reserve = usage.getReservedMemory();
        long lres = getReserved(reservedTweak);
        long lused = getUsed(lres,usedTweak);
        if ( reserve > max - lused && reserve < max - lres ) {
            return true;
        }
        return false;
    }
    
    public boolean isAboveThreshold(DetailedMemoryUsage usage,int usedTweak,int reservedTweak)  {
        long max = usage.getMaxMemory();
        long reserve = usage.getReservedMemory();
        long lres = getReserved(reservedTweak);
        long lused = getUsed(lres,usedTweak);
        if ( usage.getReservedMemory() > max - lres ) {
            return true;
        }
        if ( reserve > max - lused && usage.getUsedMemory() > max - lused ) {
            return true;
        }
        return false;
    }
    
    private long getReserved(int tweak) {
        if ( tweak < 0 ) {
            return reserved;
        }
        if ( tweak < 0 || tweak > 300 ) {
            return reserved;
        }
        return Math.round((tweak/100d) * reserved);
    }
    
    private long getUsed(long localReserve, int tweak) {
        if ( tweak < 0 || tweak > 300 ) {
            return used;
        }
        return localReserve + Math.round((tweak/100d) * localReserve);
    }

    @Override
    public String toString() {
        return "EvictionThreshold{" + "name=" + name + ", max=" + maxSize + ", used=" + used + ", reserved=" + reserved + '}';
    }
}

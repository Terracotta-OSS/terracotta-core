/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.persistence.sleepycat;

import com.sleepycat.je.Transaction;
import com.tc.object.ObjectID;
import com.tc.util.OidLongArray;

import java.util.Map;
import java.util.TreeMap;

public class OidBitsArrayMapInMemoryImpl implements OidBitsArrayMap {
  private final TreeMap<Long, OidLongArray> map;
  private final int                         bitsLength;
  private final int                         longsPerDiskUnit;

  public OidBitsArrayMapInMemoryImpl(int longsPerDiskUnit) {
    this.longsPerDiskUnit = longsPerDiskUnit;
    this.bitsLength = longsPerDiskUnit * OidLongArray.BITS_PER_LONG;
    map = new TreeMap();
  }

  public void clear() {
    map.clear();
  }


  private Long oidIndex(long oid) {
    return new Long(oid / bitsLength * bitsLength);
  }

  public Long oidIndex(ObjectID id) {
    long oid = id.toLong();
    return new Long(oid / bitsLength * bitsLength);
  }

  private OidLongArray getOrLoadBitsArray(long oid) {
    Long mapIndex = oidIndex(oid);
    OidLongArray longAry;
    synchronized (map) {
      longAry = map.get(mapIndex);
      if (longAry == null) longAry = new OidLongArray(longsPerDiskUnit, mapIndex.longValue());
      map.put(mapIndex, longAry);
    }
    return longAry;
  }

  public void updateToDisk(Transaction tx) {
    // do nothing
  }

  private OidLongArray getAndModify(long oid, boolean doSet) {
    OidLongArray longAry = getOrLoadBitsArray(oid);
    int oidInArray = (int) (oid % bitsLength);
    synchronized (longAry) {
      if (doSet) {
        longAry.setBit(oidInArray);
      } else {
        longAry.clrBit(oidInArray);
      }
    }
    return (longAry);
  }

  public OidLongArray getAndSet(ObjectID id) {
    return (getAndModify(id.toLong(), true));
  }

  public OidLongArray getAndClr(ObjectID id) {
    return (getAndModify(id.toLong(), false));
  }

  public boolean contains(ObjectID id) {
    long oid = id.toLong();
    Long mapIndex = oidIndex(oid);
    synchronized (map) {
      if (map.containsKey(mapIndex)) {
        OidLongArray longAry = map.get(mapIndex);
        return (longAry.isSet((int) oid % bitsLength));
      }
    }
    return (false);
  }

  // for testing
  public String toString() {
    StringBuffer buf = new StringBuffer();
    for (Map.Entry<Long, OidLongArray> entry : map.entrySet()) {
      Long base = entry.getKey();
      OidLongArray ary = entry.getValue();
      for (int i = 0; i < ary.totalBits(); ++i) {
        if (ary.isSet(i)) {
          buf.append(" " + (base + i));
        }
      }
    }
    return buf.toString();
  }

}

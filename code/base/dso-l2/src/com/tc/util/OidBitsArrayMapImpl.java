/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.util;

import com.tc.object.ObjectID;

import java.util.Map;
import java.util.TreeMap;

public class OidBitsArrayMapImpl implements OidBitsArrayMap {
  protected final TreeMap<Long, OidLongArray> map;
  protected final int                         bitsLength;
  protected final int                         longsPerDiskUnit;

  public OidBitsArrayMapImpl(int longsPerDiskUnit) {
    this.longsPerDiskUnit = longsPerDiskUnit;
    this.bitsLength = longsPerDiskUnit * OidLongArray.BITS_PER_LONG;
    map = new TreeMap();
  }
  
  public Long oidIndex(ObjectID id) {
    return new Long(id.toLong() / bitsLength * bitsLength);
  }
  
  public Long oidIndex(long oid) {
    return new Long(oid / bitsLength * bitsLength);
  }

  public OidLongArray getBitsArray(long oid) {
    return map.get(oidIndex(oid));
  }

  private OidLongArray getOrLoadBitsArray(long oid) {
    Long mapIndex = oidIndex(oid);
    OidLongArray longAry;
    longAry = map.get(mapIndex);
    if (longAry == null) longAry = loadArray(oid, longsPerDiskUnit, mapIndex.longValue());
    map.put(mapIndex, longAry);
    return longAry;
  }
  
  protected OidLongArray loadArray(long oid, int lPerDiskUnit, long mapIndex) {
    return new OidLongArray(lPerDiskUnit, mapIndex);
  }
  
  private OidLongArray getAndModify(long oid, boolean doSet) {
    OidLongArray longAry = getOrLoadBitsArray(oid);
    int oidInArray = (int) (oid % bitsLength);
    if (doSet) {
      longAry.setBit(oidInArray);
    } else {
      longAry.clrBit(oidInArray);
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
    if (map.containsKey(mapIndex)) {
      OidLongArray longAry = map.get(mapIndex);
      return (longAry.isSet((int) oid % bitsLength));
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

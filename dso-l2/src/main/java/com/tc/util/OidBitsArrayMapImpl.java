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
package com.tc.util;

import com.tc.object.ObjectID;
import com.tc.objectserver.api.Transaction;

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
    return oidIndex(id.toLong());
  }

  public Long oidIndex(long oid) {
    long idx = oid / bitsLength * bitsLength;
    if ((oid < 0) && ((oid % bitsLength) != 0)) {
      // take left-most bit as base
      idx -= bitsLength;
    }
    return Long.valueOf(idx);
  }

  public OidLongArray getBitsArray(long oid) {
    return map.get(oidIndex(oid));
  }

  private OidLongArray getOrLoadBitsArray(long oid, Transaction tx) {
    Long mapIndex = oidIndex(oid);
    OidLongArray longAry;
    longAry = map.get(mapIndex);
    if (longAry == null) longAry = loadArray(oid, longsPerDiskUnit, mapIndex.longValue(), tx);
    map.put(mapIndex, longAry);
    return longAry;
  }

  protected OidLongArray loadArray(long oid, int lPerDiskUnit, long mapIndex, Transaction tx) {
    return new OidLongArray(lPerDiskUnit, mapIndex);
  }

  private int arrayOffset(long oid) {
    return (int) (Math.abs(oid) % bitsLength);
  }

  private OidLongArray getAndModify(long oid, boolean doSet, Transaction tx) {
    OidLongArray longAry = getOrLoadBitsArray(oid, tx);
    int oidInArray = arrayOffset(oid);
    if (doSet) {
      longAry.setBit(oidInArray);
    } else {
      longAry.clrBit(oidInArray);
    }
    return (longAry);
  }

  @Override
  public OidLongArray getAndSet(ObjectID id, Transaction tx) {
    return (getAndModify(id.toLong(), true, tx));
  }

  @Override
  public OidLongArray getAndClr(ObjectID id, Transaction tx) {
    return (getAndModify(id.toLong(), false, tx));
  }

  @Override
  public boolean contains(ObjectID id) {
    long oid = id.toLong();
    Long mapIndex = oidIndex(oid);
    if (map.containsKey(mapIndex)) {
      OidLongArray longAry = map.get(mapIndex);
      return (longAry.isSet(arrayOffset(oid)));
    }
    return (false);
  }

  @Override
  public void clear() {
    this.map.clear();
  }

  @Override
  public int size() {
    return this.map.size();
  }

  // for testing
  @Override
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

/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.invalidation;

import com.tc.io.TCByteBufferInput;
import com.tc.io.TCByteBufferOutput;
import com.tc.io.TCSerializable;
import com.tc.object.ObjectID;
import com.tc.util.ObjectIDSet;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class Invalidations implements TCSerializable {
  private final Map<ObjectID, ObjectIDSet> invalidationsPerCdsm;

  public Invalidations() {
    this(new HashMap<ObjectID, ObjectIDSet>());
  }

  public Invalidations(Map<ObjectID, ObjectIDSet> invalidationsPerCdsm) {
    this.invalidationsPerCdsm = invalidationsPerCdsm;
  }

  public void add(ObjectID mapID, ObjectID oid) {
    ObjectIDSet set = invalidationsPerCdsm.get(mapID);
    if (set == null) {
      set = new ObjectIDSet();
      invalidationsPerCdsm.put(mapID, set);
    }

    set.add(oid);
  }

  public Set<ObjectID> getMapIds() {
    return new HashSet(invalidationsPerCdsm.keySet());
  }

  public ObjectIDSet getObjectIDSetForMapId(ObjectID mapID) {
    return invalidationsPerCdsm.get(mapID);
  }

  public boolean isEmpty() {
    return invalidationsPerCdsm.isEmpty();
  }

  public void add(Invalidations newInvalidations) {
    for (Entry<ObjectID, ObjectIDSet> entry : newInvalidations.invalidationsPerCdsm.entrySet()) {
      ObjectID mapID = entry.getKey();
      ObjectIDSet newInvalidationsOidsForMapID = entry.getValue();
      ObjectIDSet thisInvalidationsOidsForMapID = this.getObjectIDSetForMapId(mapID);
      if (thisInvalidationsOidsForMapID == null) {
        thisInvalidationsOidsForMapID = new ObjectIDSet();
        invalidationsPerCdsm.put(mapID, thisInvalidationsOidsForMapID);
      }
      thisInvalidationsOidsForMapID.addAll(newInvalidationsOidsForMapID);
    }
  }

  public Object deserializeFrom(TCByteBufferInput in) throws IOException {
    int size = in.readInt();
    for (int i = 0; i < size; i++) {
      ObjectID mapID = new ObjectID(in.readLong());
      ObjectIDSet oidSet = new ObjectIDSet();
      oidSet.deserializeFrom(in);
      this.invalidationsPerCdsm.put(mapID, oidSet);
    }
    return this;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((invalidationsPerCdsm == null) ? 0 : invalidationsPerCdsm.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;
    Invalidations other = (Invalidations) obj;
    if (invalidationsPerCdsm == null) {
      if (other.invalidationsPerCdsm != null) return false;
    } else if (!invalidationsPerCdsm.equals(other.invalidationsPerCdsm)) return false;
    return true;
  }

  public void serializeTo(TCByteBufferOutput out) {
    out.writeInt(this.invalidationsPerCdsm.size());
    for (Entry<ObjectID, ObjectIDSet> entry : this.invalidationsPerCdsm.entrySet()) {
      ObjectID oid = entry.getKey();
      ObjectIDSet oidSet = entry.getValue();

      out.writeLong(oid.toLong());
      oidSet.serializeTo(out);
    }
  }

  public int size() {
    int size = 0;
    for (Entry<ObjectID, ObjectIDSet> entry : this.invalidationsPerCdsm.entrySet()) {
      size += entry.getValue().size();
    }

    return size;
  }

  public void removeAll(ObjectIDSet validEntries) {
    for (Iterator<Entry<ObjectID, ObjectIDSet>> mapIterator = this.invalidationsPerCdsm.entrySet().iterator(); mapIterator
        .hasNext();) {
      Entry<ObjectID, ObjectIDSet> entry = mapIterator.next();
      ObjectIDSet existingOids = entry.getValue();
      existingOids.removeAll(validEntries);

      if (existingOids.size() == 0) {
        mapIterator.remove();
      }
    }
  }
}

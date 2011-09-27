/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.servermap.localcache;

import com.tc.object.ObjectID;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

public class LocalCacheStoreIncoherentValue extends AbstractLocalCacheStoreValue {
  private volatile long lastCoherentTime;

  public LocalCacheStoreIncoherentValue() {
    //
  }

  public LocalCacheStoreIncoherentValue(ObjectID oid, Object value) {
    super(oid, value);
    this.lastCoherentTime = System.nanoTime();
  }

  @Override
  public boolean isIncoherentValue() {
    return true;
  }

  @Override
  public void writeExternal(ObjectOutput out) throws IOException {
    super.writeExternal(out);
    out.writeLong(lastCoherentTime);
  }

  @Override
  public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
    super.readExternal(in);
    lastCoherentTime = in.readLong();
  }

  public long getLastCoherentTime() {
    return lastCoherentTime;
  }

  @Override
  public ObjectID getValueObjectId() {
    return (ObjectID) id;
  }

}

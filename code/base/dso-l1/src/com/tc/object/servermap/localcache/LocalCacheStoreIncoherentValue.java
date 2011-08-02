/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.servermap.localcache;

import com.tc.object.ObjectID;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

public class LocalCacheStoreIncoherentValue extends AbstractLocalCacheStoreValue {
  public static final long SERVERMAP_INCOHERENT_CACHED_ITEMS_RECYCLE_TIME_MILLIS = 300000;
  // TCPropertiesImpl
  // .getProperties()
  // .getLong(
  // TCPropertiesConsts.EHCACHE_STORAGESTRATEGY_DCV2_LOCALCACHE_INCOHERENT_READ_TIMEOUT);

  private volatile long    lastCoherentTime;

  public LocalCacheStoreIncoherentValue() {
    //
  }

  public LocalCacheStoreIncoherentValue(Object value, ObjectID mapID) {
    super(null, value, mapID);
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

}

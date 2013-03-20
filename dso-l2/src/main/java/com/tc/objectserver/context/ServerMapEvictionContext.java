/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.context;

import com.tc.async.api.EventContext;
import com.tc.object.ObjectID;
import com.tc.objectserver.api.EvictableEntry;
import com.tc.objectserver.api.EvictionTrigger;

import java.util.Map;

public class ServerMapEvictionContext implements EventContext {

  private final EvictionTrigger trigger;
  private final int      tti;
  private final int      ttl;
  private final Map<Object, EvictableEntry>      samples;
  private final String   className;
  private final String   cacheName;

  public ServerMapEvictionContext(final EvictionTrigger trigger, final int tti, final int ttl,
                                  final Map<Object, EvictableEntry> samples, final String className, final String cacheName) {
    this.trigger = trigger;
    this.tti = tti;
    this.ttl = ttl;
    this.samples = samples;
    this.className = className;
    this.cacheName = cacheName;
  }
  
   public ServerMapEvictionContext(final EvictionTrigger trigger, final Map<Object, EvictableEntry> samples, final String className, final String cacheName) {
    this.trigger = trigger;
    this.tti = 0;
    this.ttl = 0;
    this.samples = samples;
    this.className = className;
    this.cacheName = cacheName;
    if ( samples.isEmpty() ) {
        throw new AssertionError("no samples " + cacheName);
    }
  } 

  public ObjectID getOid() {
    return this.trigger.getId();
  }

  public int getTTISeconds() {
    return this.tti;
  }

  public int getTTLSeconds() {
    return this.ttl;
  }

  public Map<Object, EvictableEntry> getRandomSamples() {
    return this.samples;
  }

  public String getClassName() {
    return this.className;
  }

  public String getCacheName() {
    return this.cacheName;
  }

    @Override
    public String toString() {
        return "ServerMapEvictionContext{" + "oid=" + trigger + ", samples=" + samples.size() + ", className=" + className + ", cacheName=" + cacheName + '}';
    }
}

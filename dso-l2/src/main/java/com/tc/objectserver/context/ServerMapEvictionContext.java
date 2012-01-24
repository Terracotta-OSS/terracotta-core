/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.context;

import com.tc.async.api.EventContext;
import com.tc.object.ObjectID;

import java.util.Map;

public class ServerMapEvictionContext implements EventContext {

  private final ObjectID oid;
  private final int      targetMaxTotalCount;
  private final int      tti;
  private final int      ttl;
  private final Map      samples;
  private final int      overshoot;
  private final String   className;
  private final String   cacheName;

  public ServerMapEvictionContext(final ObjectID oid, final int targetMaxTotalCount, final int tti, final int ttl,
                                  final Map samples, final int overshoot, final String className, final String cacheName) {
    this.oid = oid;
    this.targetMaxTotalCount = targetMaxTotalCount;
    this.tti = tti;
    this.ttl = ttl;
    this.samples = samples;
    this.overshoot = overshoot;
    this.className = className;
    this.cacheName = cacheName;
  }

  public ObjectID getOid() {
    return this.oid;
  }

  public int getTargetMaxTotalCount() {
    return this.targetMaxTotalCount;
  }

  public int getTTISeconds() {
    return this.tti;
  }

  public int getTTLSeconds() {
    return this.ttl;
  }

  public Map getRandomSamples() {
    return this.samples;
  }

  public int getOvershoot() {
    return this.overshoot;
  }

  public String getClassName() {
    return this.className;
  }

  public String getCacheName() {
    return this.cacheName;
  }
}

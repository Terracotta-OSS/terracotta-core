/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.api;

import com.tc.async.api.PostInit;
import com.tc.object.ObjectID;
import com.tc.stats.counter.sampled.SampledCounter;
import com.tc.text.PrettyPrintable;

import java.util.Map;

public interface ServerMapEvictionManager extends PostInit, PrettyPrintable {

  public void startEvictor();

  public void runEvictor();

  public boolean doEvictionOn(EvictionTrigger trigger);
  
  public boolean scheduleCapacityEviction(ObjectID oid);
  
  public void evict(ObjectID oid, Map<Object, EvictableEntry> samples, String className, String cacheName);
  
  public SampledCounter getExpirationStatistics();
  
  public SampledCounter getEvictionStatistics();

}

/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.api;

import com.tc.async.api.PostInit;
import com.tc.object.ObjectID;
import com.tc.text.PrettyPrintable;

import java.util.Map;

public interface ServerMapEvictionManager extends PostInit, PrettyPrintable {

  public void startEvictor();

  public void runEvictor();

  public void doEvictionOn(final ObjectID oid, final boolean periodicEvictorRun);

  public void evict(ObjectID oid, Map samples, int targetMaxTotalCount, int ttiSeconds, int ttlSeconds, int overshoot,
                    String className, String cacheName);

}

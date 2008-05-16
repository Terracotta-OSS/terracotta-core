/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.context;

import com.tc.async.api.EventContext;

import java.util.Set;

public class GCResultContext implements EventContext {

  private final int gcIteration;
  private final Set gcedOids;

  public GCResultContext(int gcIteration, Set gcedOids) {
    this.gcIteration = gcIteration;
    this.gcedOids = gcedOids;
  }

  public int getGCIterationCount() {
    return gcIteration;
  }

  public Set getGCedObjectIDs() {
    return gcedOids;
  }

  public String toString() {
    return "GCResultContext [ " + gcIteration + " , " + gcedOids.size() + " ]";
  }
}

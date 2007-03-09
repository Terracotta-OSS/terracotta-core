/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.objectserver.api;

import com.tc.net.protocol.tcm.ChannelID;
import com.tc.object.ObjectID;
import com.tc.objectserver.context.ObjectManagerResultsContext;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * @author steve
 */
public class TestObjectManagerResultsContext implements ObjectManagerResultsContext {

  private final Map results;
  private boolean   pending;
  private final Set objectIDs;

  public TestObjectManagerResultsContext(Map results, Set objectIDs) {
    this.results = results;
    this.objectIDs = objectIDs;
  }

  public Map getResults() {
    return results;
  }

  public void setResults(ChannelID chID, Collection ids, ObjectManagerLookupResults results) {
    pending = false;
    this.results.putAll(results.getObjects());
  }

  public boolean isPendingRequest() {
    return pending;
  }

  public void makePending() {
    pending = true;
  }

  public Set getLookupIDs() {
    return objectIDs;
  }

  public boolean isNewObject(ObjectID id) {
    return false;
  }

}
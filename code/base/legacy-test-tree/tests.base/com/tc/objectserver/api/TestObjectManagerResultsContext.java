/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.objectserver.api;

import com.tc.net.protocol.tcm.ChannelID;
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

  public TestObjectManagerResultsContext(Map results) {
    this.results = results;
  }

  public Map getResults() {
    return results;
  }

  public void setResults(ChannelID chID, Collection ids, ObjectManagerLookupResults results) {
    pending = false;
    this.results.putAll(results.getObjects());
  }

  public Set getCheckedOutObjectIDs() {
    return results.keySet();
  }

  public boolean isPendingRequest() {
    return pending;
  }

  public void makePending(ChannelID channelID, Collection ids) {
    pending = true;
  }

}
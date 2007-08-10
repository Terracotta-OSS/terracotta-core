/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.objectserver.api;

import com.tc.objectserver.context.ObjectManagerResultsContext;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * @author steve
 */
public class TestObjectManagerResultsContext implements ObjectManagerResultsContext {

  private final Map results;
  private final Set objectIDs;

  public TestObjectManagerResultsContext(Map results, Set objectIDs) {
    this.results = results;
    this.objectIDs = objectIDs;
  }

  public Map getResults() {
    return results;
  }

  public void setResults(ObjectManagerLookupResults results) {
    this.results.putAll(results.getObjects());
  }

  public Set getLookupIDs() {
    return objectIDs;
  }

  public Set getNewObjectIDs() {
    return Collections.EMPTY_SET;
  }


}
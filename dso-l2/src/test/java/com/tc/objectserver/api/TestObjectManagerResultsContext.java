/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.api;

import com.tc.object.ObjectID;
import com.tc.objectserver.context.ObjectManagerResultsContext;
import com.tc.objectserver.core.api.ManagedObject;
import com.tc.util.ObjectIDSet;

import java.util.Map;

public class TestObjectManagerResultsContext implements ObjectManagerResultsContext {

  private final Map<ObjectID, ManagedObject> results;
  private final ObjectIDSet                  objectIDs;

  public TestObjectManagerResultsContext(Map<ObjectID, ManagedObject> results, ObjectIDSet objectIDs) {
    this.results = results;
    this.objectIDs = objectIDs;
  }

  public Map getResults() {
    return results;
  }

  public void setResults(ObjectManagerLookupResults results) {
    this.results.putAll(results.getObjects());
    if (!results.getMissingObjectIDs().isEmpty()) { throw new AssertionError("Missing Objects : "
                                                                             + results.getMissingObjectIDs()); }
  }

  public ObjectIDSet getLookupIDs() {
    return objectIDs;
  }

  public ObjectIDSet getNewObjectIDs() {
    return new ObjectIDSet();
  }

  public boolean updateStats() {
    return true;
  }

}
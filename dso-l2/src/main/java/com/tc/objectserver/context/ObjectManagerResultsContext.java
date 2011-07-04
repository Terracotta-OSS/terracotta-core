/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.context;

import com.tc.async.api.EventContext;
import com.tc.objectserver.api.ObjectManagerLookupResults;
import com.tc.util.ObjectIDSet;

/**
 * Interface for a context that needs ObjectManager look results
 */
public interface ObjectManagerResultsContext extends EventContext {

  public ObjectIDSet getLookupIDs();

  public ObjectIDSet getNewObjectIDs();

  public void setResults(ObjectManagerLookupResults results);

  public boolean updateStats();
}
/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.objectserver.impl;

import com.tc.objectserver.api.ObjectManagerLookupResults;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

public class ObjectManagerLookupResultsImpl implements ObjectManagerLookupResults {

  private final Map objects;
  private final Set lookupPendingObjectIDs;

  public ObjectManagerLookupResultsImpl(Map objects ) {
    this(objects, Collections.EMPTY_SET);
  }
  
  public ObjectManagerLookupResultsImpl(Map objects, Set lookupPendingObjectIDs) {
    this.objects = objects;
    this.lookupPendingObjectIDs = lookupPendingObjectIDs;
  }

  public Map getObjects() {
    return this.objects;
  }

  public Set getLookupPendingObjectIDs() {
    return lookupPendingObjectIDs;
  }
}

/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.context;

import com.tc.async.api.EventContext;
import com.tc.object.ObjectID;

public class ManagedObjectFaultingContext implements EventContext {

  private final ObjectID id;
  private final boolean  isRemoveOnRelease;

  public ManagedObjectFaultingContext(ObjectID id, boolean isRemoveOnRelease) {
    this.id = id;
    this.isRemoveOnRelease = isRemoveOnRelease;
  }

  public ObjectID getId() {
    return id;
  }

  public boolean isRemoveOnRelease() {
    return isRemoveOnRelease;
  }

}

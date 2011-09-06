/*
 * Copyright (c) 2003-2008 Terracotta, Inc. All rights reserved.
 */
package com.tc.objectserver.context;

import com.tc.async.api.EventContext;

import java.util.ArrayList;
import java.util.List;

public class ManagedObjectFlushingContext implements EventContext {

  private final List toFlush = new ArrayList();

  public ManagedObjectFlushingContext() {
    super();
  }

  public List getObjectToFlush() {
    return toFlush;
  }

  public void addObjectToFlush(Object dirtyObject) {
    toFlush.add(dirtyObject);
  }

}

/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.context;

import com.tc.async.api.EventContext;

import java.util.List;

public class RecallObjectsContext implements EventContext {

  private final boolean all;
  private final List    recalledObjects;

  public RecallObjectsContext(List recalledObjects, boolean all) {
    this.recalledObjects = recalledObjects;
    this.all = all;
  }

  public boolean recallAll() {
    return all;
  }

  public List getRecallList() {
    return recalledObjects;
  }

}

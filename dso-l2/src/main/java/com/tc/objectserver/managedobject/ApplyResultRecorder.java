/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.managedobject;

import com.tc.object.ObjectID;

import java.util.List;
import java.util.Map;

public interface ApplyResultRecorder {
  public void recordResult(ObjectID oid, Object result);

  public Map<ObjectID, List> getResults();
}

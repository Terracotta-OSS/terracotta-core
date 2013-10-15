/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.managedobject;

import com.tc.object.ObjectID;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class NullResultRecorderImpl implements ApplyResultRecorder {

  @Override
  public void recordResult(ObjectID oid, Object result) {
    // No-Op
  }

  @Override
  public Map<ObjectID, List> getResults() {
    return Collections.emptyMap();
  }

}

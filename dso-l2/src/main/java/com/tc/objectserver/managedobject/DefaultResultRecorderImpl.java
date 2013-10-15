/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.managedobject;

import com.tc.object.ObjectID;
import com.tc.util.Assert;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DefaultResultRecorderImpl implements ApplyResultRecorder {
  private final Map<ObjectID, List> changeResults = new LinkedHashMap<ObjectID, List>();

  @Override
  public void recordResult(ObjectID oid, Object result) {
    List results = changeResults.get(oid);
    if (results == null) {
      results = new ArrayList<Object>();
      Assert.assertNull(changeResults.put(oid, results));
    }
    results.add(result);

  }

  @Override
  public Map<ObjectID, List> getResults() {
    return changeResults;
  }

}

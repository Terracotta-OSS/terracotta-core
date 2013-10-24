/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.managedobject;

import com.tc.object.dna.api.LogicalChangeID;
import com.tc.object.dna.api.LogicalChangeResult;
import com.tc.util.Assert;

import java.util.LinkedHashMap;
import java.util.Map;

public class DefaultResultRecorderImpl implements ApplyResultRecorder {
  private final Map<LogicalChangeID, LogicalChangeResult> changeResults = new LinkedHashMap<LogicalChangeID, LogicalChangeResult>();

  @Override
  public void recordResult(LogicalChangeID changeID, LogicalChangeResult result) {
    if (changeID.isNull()) {
      // L1 not interested in the result
      return;
    }
    Assert.assertNull(changeResults.put(changeID, result));

  }

  @Override
  public Map<LogicalChangeID, LogicalChangeResult> getResults() {
    return changeResults;
  }

}

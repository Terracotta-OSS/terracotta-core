/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.managedobject;

import com.tc.object.dna.api.LogicalChangeID;
import com.tc.object.dna.api.LogicalChangeResult;

import java.util.Map;

public interface ApplyResultRecorder {
  public void recordResult(LogicalChangeID logicalChangeID, LogicalChangeResult result);

  public Map<LogicalChangeID, LogicalChangeResult> getResults();

  public void recordResults(Map<LogicalChangeID, LogicalChangeResult> applyResults);

  boolean needPersist();
}

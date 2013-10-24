/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.tx;

import com.tc.object.dna.api.LogicalChangeResult;

public interface LogicalChangeListener {
  
  public void handleResult(LogicalChangeResult result);

  public void handleAborted();

}

/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.change.event;

import com.tc.object.change.TCChangeBufferEvent;
import com.tc.object.dna.api.DNAWriter;

/**
 * Nov 22, 2004: Event representing any logical actions that need to be logged
 */
public class LogicalChangeEvent implements TCChangeBufferEvent {
  private final int      method;
  private final Object[] parameters;

  public LogicalChangeEvent(int method, Object[] parameters) {
    this.parameters = parameters;
    this.method = method;
  }

  public void write(DNAWriter writer) {
    writer.addLogicalAction(method, parameters);
  }

  public int getMethodID() {
    return method;
  }

  public Object[] getParameters() {
    return parameters;
  }
}
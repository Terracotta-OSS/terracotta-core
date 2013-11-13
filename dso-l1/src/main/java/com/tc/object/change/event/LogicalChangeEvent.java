/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.change.event;

import com.tc.object.change.TCChangeBufferEvent;
import com.tc.object.dna.api.DNAWriter;
import com.tc.object.dna.api.LogicalChangeID;

/**
 * Nov 22, 2004: Event representing any logical actions that need to be logged
 */
public class LogicalChangeEvent implements TCChangeBufferEvent {
  private final int      method;
  private final Object[] parameters;
  private final LogicalChangeID logicalChangeID;

  public LogicalChangeEvent(int method, Object[] parameters, LogicalChangeID id) {
    this.parameters = parameters;
    this.method = method;
    this.logicalChangeID = id;
  }

  @Override
  public void write(DNAWriter writer) {
    writer.addLogicalAction(method, parameters, logicalChangeID);
  }

  public int getMethodID() {
    return method;
  }

  public Object[] getParameters() {
    return parameters;
  }

  public LogicalChangeID getLogicalChangeID(){
    return this.logicalChangeID;
  }

}
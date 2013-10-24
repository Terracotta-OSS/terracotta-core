/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.change.event;

import com.tc.object.change.TCChangeBufferEvent;
import com.tc.object.dna.api.DNAWriter;
import com.tc.object.dna.api.LogicalChangeID;
import com.tc.object.tx.LogicalChangeListener;
import com.tc.util.Assert;

/**
 * Nov 22, 2004: Event representing any logical actions that need to be logged
 */
public class LogicalChangeEvent implements TCChangeBufferEvent {
  private final int      method;
  private final Object[] parameters;
  private final LogicalChangeListener listener;
  private LogicalChangeID             logicalChangeID = LogicalChangeID.NULL_ID;

  public LogicalChangeEvent(int method, Object[] parameters, LogicalChangeListener listener) {
    this.parameters = parameters;
    this.method = method;
    this.listener = listener;
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

  public LogicalChangeListener getListener() {
    return listener;
  }

  public LogicalChangeID getLogicalChangeID(){
    return this.logicalChangeID;
  }

  public void setLogicalChangeID(LogicalChangeID logicalChangeID) {
    Assert.assertTrue("logicalChangeID already set", this.logicalChangeID.isNull());
    this.logicalChangeID = logicalChangeID;
  }
}
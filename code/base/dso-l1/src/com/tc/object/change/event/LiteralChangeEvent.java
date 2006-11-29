/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.object.change.event;

import com.tc.object.ObjectID;
import com.tc.object.change.TCChangeBufferEvent;
import com.tc.object.dna.api.DNAWriter;

public class LiteralChangeEvent implements TCChangeBufferEvent {
  private final Object newValue;

  public LiteralChangeEvent(Object newValue) {
    this.newValue = newValue;
  }

  public Object getNewValue() {
    return newValue;
  }

  public boolean isReference() {
    return newValue instanceof ObjectID;
  }

  public void write(DNAWriter writer) {
    writer.addLiteralValue(newValue);
  }

}
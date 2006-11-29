/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.object.change.event;

import com.tc.object.ObjectID;
import com.tc.object.change.TCChangeBufferEvent;
import com.tc.object.dna.api.DNAWriter;

public class PhysicalChangeEvent implements TCChangeBufferEvent {
  private final Object newValue;
  private final String fieldname;

  public PhysicalChangeEvent(String fieldname, Object newValue) {
    this.newValue = newValue;
    this.fieldname = fieldname;
  }

  public String getFieldName() {
    return fieldname;
  }

  public Object getNewValue() {
    return newValue;
  }

  public boolean isReference() {
    return newValue instanceof ObjectID;
  }

  public void write(DNAWriter writer) {
    writer.addPhysicalAction(fieldname, newValue);
  }

}
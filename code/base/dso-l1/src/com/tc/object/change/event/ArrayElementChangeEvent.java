/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.change.event;

import com.tc.object.ObjectID;
import com.tc.object.change.TCChangeBufferEvent;
import com.tc.object.dna.api.DNAWriter;

public class ArrayElementChangeEvent implements TCChangeBufferEvent {

  private final Object value;
  private final int    index;
  private final int    length;

  public ArrayElementChangeEvent(int index, Object value) {
    this(index, value, -1);
  }

  /**
   * @param index index in the array for the changed element or start index for the subarray
   * @param value new value or copied array for the subarray
   * @param legnth the length of the subarray
   */
  public ArrayElementChangeEvent(int index, Object value, int length) {
    this.index = index;
    this.value = value;
    this.length = length;
  }

  public void write(DNAWriter to) {
    if (isSubarray()) {
      to.addSubArrayAction(index, value, length);
    } else {
      to.addArrayElementAction(index, value);
    }
  }

  public Object getValue() {
    return value;
  }

  public int getIndex() {
    return index;
  }

  public boolean isReference() {
    return value instanceof ObjectID;
  }

  public boolean isSubarray() {
    return length != -1;
  }

  public int getLength() {
    return length;
  }
}

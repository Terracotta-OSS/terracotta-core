/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.search.aggregator;

import com.tc.io.TCByteBufferInput;
import com.tc.io.TCByteBufferOutput;
import com.tc.object.metadata.ValueType;

import java.io.IOException;

public class FloatSum extends Sum {

  private boolean used = false;
  private float   sum  = 0;

  public FloatSum(String attributeName, ValueType type) {
    super(attributeName, type);
  }

  public void accept(Object input) throws IllegalArgumentException {
    if (input == null) { return; }

    if (!(input instanceof Number)) { throw new IllegalArgumentException(input.getClass().getName()
                                                                         + " is not a number for attribute ["
                                                                         + getAttributeName() + "]"); }

    sum += ((Number) input).floatValue();
    used = true;
  }

  public void accept(Aggregator incoming) throws IllegalArgumentException {
    if (incoming instanceof FloatSum) {
      sum += ((FloatSum) incoming).sum;
      used = true;
    } else {
      throw new IllegalArgumentException();
    }
  }

  public Float getResult() {
    if (used) {
      return Float.valueOf(sum);
    } else {
      return null;
    }
  }

  @Override
  Aggregator deserializeData(TCByteBufferInput input) throws IOException {
    used = input.readBoolean();
    sum = input.readFloat();
    return this;
  }

  @Override
  void serializeData(TCByteBufferOutput output) {
    output.writeBoolean(used);
    output.writeFloat(sum);
  }
}

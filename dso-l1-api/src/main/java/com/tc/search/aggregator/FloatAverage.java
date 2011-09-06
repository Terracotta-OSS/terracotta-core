/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.search.aggregator;

import com.tc.io.TCByteBufferInput;
import com.tc.io.TCByteBufferOutput;
import com.tc.object.metadata.ValueType;

import java.io.IOException;

public class FloatAverage extends Average {

  private float sum   = 0;
  private int   count = 0;

  public FloatAverage(String attributeName, ValueType type) {
    super(attributeName, type);
  }

  public void accept(Object input) throws IllegalArgumentException {
    if (input == null) { return; }

    if (!(input instanceof Number)) { throw new IllegalArgumentException(input.getClass().getName()
                                                                         + " is not a number for attribute ["
                                                                         + getAttributeName() + "]"); }

    count++;
    sum += ((Number) input).floatValue();
  }

  public void accept(Aggregator incoming) throws IllegalArgumentException {
    if (incoming instanceof FloatAverage) {
      count += ((FloatAverage) incoming).count;
      sum += ((FloatAverage) incoming).sum;
    } else {
      throw new IllegalArgumentException();
    }
  }

  public Float getResult() {
    if (count == 0) {
      return null;
    } else {
      return Float.valueOf(sum / count);
    }
  }

  @Override
  Aggregator deserializeData(TCByteBufferInput input) throws IOException {
    sum = input.readFloat();
    count = input.readInt();
    return this;
  }

  @Override
  void serializeData(TCByteBufferOutput output) {
    output.writeFloat(sum);
    output.writeInt(count);
  }
}

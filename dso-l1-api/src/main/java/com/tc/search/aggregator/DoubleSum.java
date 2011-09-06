/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.search.aggregator;

import com.tc.io.TCByteBufferInput;
import com.tc.io.TCByteBufferOutput;
import com.tc.object.metadata.ValueType;

import java.io.IOException;

public class DoubleSum extends Sum {

  private boolean used = false;
  private double  sum  = 0;

  public DoubleSum(String attributeName, ValueType type) {
    super(attributeName, type);
  }

  public void accept(Object input) throws IllegalArgumentException {
    if (input == null) { return; }

    if (!(input instanceof Number)) { throw new IllegalArgumentException(input.getClass().getName()
                                                                         + " is not a number for attribute ["
                                                                         + getAttributeName() + "]"); }

    sum += ((Number) input).doubleValue();
    used = true;
  }

  public void accept(Aggregator incoming) throws IllegalArgumentException {
    if (incoming instanceof DoubleSum) {
      sum += ((DoubleSum) incoming).sum;
      used = true;
    } else {
      throw new IllegalArgumentException();
    }
  }

  public Double getResult() {
    if (used) {
      return Double.valueOf(sum);
    } else {
      return null;
    }
  }

  @Override
  Aggregator deserializeData(TCByteBufferInput input) throws IOException {
    used = input.readBoolean();
    sum = input.readDouble();
    return this;
  }

  @Override
  void serializeData(TCByteBufferOutput output) {
    output.writeBoolean(used);
    output.writeDouble(sum);
  }
}

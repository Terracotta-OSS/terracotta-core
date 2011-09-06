/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.search.aggregator;

import com.tc.io.TCByteBufferInput;
import com.tc.io.TCByteBufferOutput;
import com.tc.object.metadata.ValueType;

import java.io.IOException;

public class IntegerAverage extends Average {

  private long sum   = 0;
  private int  count = 0;

  public IntegerAverage(String attributeName, ValueType type) {
    super(attributeName, type);
  }

  public void accept(Object input) throws IllegalArgumentException {
    if (input == null) { return; }

    if (!(input instanceof Number)) { throw new IllegalArgumentException(input.getClass().getName()
                                                                         + " is not a number for attribute ["
                                                                         + getAttributeName() + "]"); }

    count++;
    sum += ((Number) input).intValue();
  }

  public void accept(Aggregator incoming) throws IllegalArgumentException {
    if (incoming instanceof IntegerAverage) {
      count += ((IntegerAverage) incoming).count;
      sum += ((IntegerAverage) incoming).sum;
    } else {
      throw new IllegalArgumentException();
    }
  }

  public Float getResult() {
    if (count == 0) {
      return null;
    } else {
      return Float.valueOf(((float) sum) / count);
    }
  }

  @Override
  Aggregator deserializeData(TCByteBufferInput input) throws IOException {
    sum = input.readLong();
    count = input.readInt();
    return this;
  }

  @Override
  void serializeData(TCByteBufferOutput output) {
    output.writeLong(sum);
    output.writeInt(count);
  }
}

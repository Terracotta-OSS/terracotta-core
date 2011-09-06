/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.search.aggregator;

import com.tc.io.TCByteBufferInput;
import com.tc.io.TCByteBufferOutput;
import com.tc.object.metadata.ValueType;
import com.tc.search.AggregatorOperations;

import java.io.IOException;

public class Count extends AbstractAggregator {

  private int count;

  public Count(String attributeName, ValueType type) {
    super(AggregatorOperations.COUNT, attributeName, type);
  }

  public void accept(Object input) {
    count++;
  }

  public void accept(Aggregator incoming) throws IllegalArgumentException {
    if (incoming instanceof Count) {
      count += ((Count) incoming).count;
    } else {
      throw new IllegalArgumentException();
    }
  }

  public Integer getResult() {
    return Integer.valueOf(count);
  }

  @Override
  Aggregator deserializeData(TCByteBufferInput input) throws IOException {
    count = input.readInt();
    return this;
  }

  @Override
  void serializeData(TCByteBufferOutput input) {
    input.writeInt(count);
  }

}

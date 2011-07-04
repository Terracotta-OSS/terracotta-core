/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.search.aggregator;

import com.tc.io.TCByteBufferInput;
import com.tc.io.TCByteBufferOutput;
import com.tc.object.metadata.ValueType;
import com.tc.search.AggregatorOperations;

import java.io.IOException;

public abstract class AbstractAggregator implements Aggregator {

  private final String               attributeName;
  private final ValueType            type;
  private final AggregatorOperations operation;

  AbstractAggregator(AggregatorOperations operation, String attributeName, ValueType type) {
    this.attributeName = attributeName;
    this.type = type;
    this.operation = operation;
  }

  public final String getAttributeName() {
    return attributeName;
  }

  public final ValueType getType() {
    return type;
  }

  public final AggregatorOperations getOperation() {
    return operation;
  }

  public final void serializeTo(TCByteBufferOutput output) {
    output.writeString(getAttributeName());
    if (type == null) {
      output.writeInt(-1);
    } else {
      output.writeInt(type.ordinal());
    }
    output.writeInt(operation.ordinal());
    serializeData(output);
  }

  public final Object deserializeFrom(TCByteBufferInput input) throws IOException {
    return deserializeInstance(input);
  }

  abstract Aggregator deserializeData(TCByteBufferInput input) throws IOException;

  abstract void serializeData(TCByteBufferOutput output);

  public static Aggregator deserializeInstance(TCByteBufferInput input) throws IOException {
    String attributeName = input.readString();
    ValueType type;
    int typeIndex = input.readInt();
    if (typeIndex < 0) {
      type = null;
    } else {
      type = ValueType.values()[typeIndex];
    }
    AggregatorOperations operation = AggregatorOperations.values()[input.readInt()];

    return aggregator(operation, attributeName, type).deserializeData(input);
  }

  public static AbstractAggregator aggregator(AggregatorOperations operation, String attributeName, ValueType type) {
    switch (operation) {
      case AVERAGE:
        return Average.average(attributeName, type);
      case COUNT:
        return new Count(attributeName, type);
      case MAX:
        return MinMax.max(attributeName, type);
      case MIN:
        return MinMax.min(attributeName, type);
      case SUM:
        return Sum.sum(attributeName, type);
    }
    throw new IllegalArgumentException();
  }
}

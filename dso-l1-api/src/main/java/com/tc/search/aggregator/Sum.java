/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.search.aggregator;

import com.tc.object.metadata.ValueType;
import com.tc.search.AggregatorOperations;

public abstract class Sum extends AbstractAggregator {

  public Sum(String attributeName, ValueType type) {
    super(AggregatorOperations.SUM, attributeName, type);
  }

  public static Sum sum(String attributeName, ValueType type) throws IllegalArgumentException {
    if (type == null) {
      return new EmptySum(attributeName);
    } else {
      switch (type) {
        case BYTE:
        case CHAR:
        case SHORT:
        case INT:
        case LONG:
          return new LongSum(attributeName, type);
        case FLOAT:
          return new FloatSum(attributeName, type);
        case DOUBLE:
          return new DoubleSum(attributeName, type);
        default:
          throw new IllegalArgumentException("Attribute [" + attributeName + ":" + type + "] is not a numeric type");
      }
    }
  }
}

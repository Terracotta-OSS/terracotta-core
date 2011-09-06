/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.search.aggregator;

import com.tc.io.TCSerializable;
import com.tc.object.metadata.ValueType;

public interface Aggregator extends TCSerializable {

  void accept(Object input) throws IllegalArgumentException;

  void accept(Aggregator incoming) throws IllegalArgumentException;

  String getAttributeName();

  Object getResult();

  ValueType getType();
}

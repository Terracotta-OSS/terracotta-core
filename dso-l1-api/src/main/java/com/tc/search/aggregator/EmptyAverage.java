/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.search.aggregator;

import com.tc.io.TCByteBufferInput;
import com.tc.io.TCByteBufferOutput;

import java.io.IOException;

public class EmptyAverage extends Average {

  private Average delegate;

  public EmptyAverage(String attributeName) {
    super(attributeName, null);
  }

  public void accept(Object input) throws IllegalArgumentException {
    if (delegate != null) {
      delegate.accept(input);
    } else if (input != null) { throw new IllegalArgumentException(
                                                                   "Unexpected visitor on EmptyAverage aggregator [attribute:"
                                                                       + getAttributeName() + " value:" + input + "]"); }
  }

  public void accept(Aggregator incoming) throws IllegalArgumentException {
    if (incoming instanceof Average) {
      if (delegate == null) {
        delegate = (Average) incoming;
      } else {
        delegate.accept(incoming);
      }
    } else {
      throw new IllegalArgumentException();
    }
  }

  public Object getResult() {
    if (delegate == null) {
      return null;
    } else {
      return delegate.getResult();
    }
  }

  @Override
  Aggregator deserializeData(TCByteBufferInput input) throws IOException {
    if (input.readBoolean()) {
      return (Aggregator) delegate.deserializeFrom(input);
    } else {
      delegate = null;
      return this;
    }
  }

  @Override
  void serializeData(TCByteBufferOutput output) {
    if (delegate == null) {
      output.writeBoolean(false);
    } else {
      output.writeBoolean(true);
      delegate.serializeTo(output);
    }
  }
}

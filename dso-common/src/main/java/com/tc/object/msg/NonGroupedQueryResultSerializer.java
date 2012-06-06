/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.msg;

import com.tc.io.TCByteBufferInput;
import com.tc.io.TCByteBufferOutput;
import com.terracottatech.search.NonGroupedIndexQueryResultImpl;
import com.terracottatech.search.NonGroupedQueryResult;
import com.terracottatech.search.ValueID;

import java.io.IOException;

public class NonGroupedQueryResultSerializer extends IndexQueryResultSerializer<NonGroupedQueryResult> {
  private final class NonGroupedResultBuilder extends IndexQueryResultBuilder {
    private String  key;
    private ValueID valueID;

    @Override
    protected NonGroupedQueryResult build() {
      return new NonGroupedIndexQueryResultImpl(key, valueID, attributes, sortAttributes);
    }

    private NonGroupedResultBuilder setKey(String key) {
      this.key = key;
      return this;
    }

    private NonGroupedResultBuilder setValueID(ValueID valueID) {
      this.valueID = valueID;
      return this;
    }

  }

  @Override
  public void serialize(NonGroupedQueryResult result, TCByteBufferOutput output) {
    output.writeString(result.getKey());
    output.writeLong(result.getValue().toLong());
    super.serialize(result, output);
  }

  @Override
  public NonGroupedQueryResult deserializeFrom(TCByteBufferInput input) throws IOException {
    String key = input.readString();
    ValueID valueID = new ValueID(input.readLong());
    NonGroupedResultBuilder builder = (NonGroupedResultBuilder) buildCommonFields(input);

    return builder.setKey(key).setValueID(valueID).build();
  }

  @Override
  protected IndexQueryResultBuilder builder() {
    return new NonGroupedResultBuilder();
  }

}

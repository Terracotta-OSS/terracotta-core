/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.msg;

import com.tc.io.TCByteBufferInput;
import com.tc.io.TCByteBufferOutput;
import com.tc.object.dna.impl.NullObjectStringSerializer;
import com.tc.object.dna.impl.ObjectStringSerializer;
import com.tc.object.metadata.NVPairSerializer;
import com.terracottatech.search.IndexQueryResult;
import com.terracottatech.search.NVPair;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class IndexQueryResultSerializer<T extends IndexQueryResult> {

  static final NVPairSerializer       NVPAIR_SERIALIZER = new NVPairSerializer();
  static final ObjectStringSerializer NULL_SERIALIZER   = new NullObjectStringSerializer();

  protected abstract class IndexQueryResultBuilder {
    protected List<NVPair> attributes;
    protected List<NVPair> sortAttributes;

    protected IndexQueryResultBuilder setAttributes(List<NVPair> attrs) {
      attributes = attrs;
      return this;
    }

    protected IndexQueryResultBuilder setSortAttributes(List<NVPair> attrs) {
      sortAttributes = attrs;
      return this;
    }

    protected abstract T build();
  }

  public void serialize(T result, TCByteBufferOutput output) {
    List<NVPair> attributes = result.getAttributes();
    output.writeInt(attributes.size());
    for (NVPair pair : attributes) {
      NVPAIR_SERIALIZER.serialize(pair, output, NULL_SERIALIZER);
    }

    List<NVPair> sortAttributes = result.getSortAttributes();
    output.writeInt(sortAttributes.size());
    for (NVPair pair : sortAttributes) {
      NVPAIR_SERIALIZER.serialize(pair, output, NULL_SERIALIZER);
    }
  }

  abstract T deserializeFrom(TCByteBufferInput input) throws IOException;

  protected abstract IndexQueryResultBuilder builder();

  protected IndexQueryResultBuilder buildCommonFields(TCByteBufferInput input) throws IOException {
    int size = input.readInt();

    List<NVPair> attributes = size > 0 ? new ArrayList<NVPair>() : Collections.EMPTY_LIST;
    for (int i = 0; i < size; i++) {
      NVPair pair = NVPAIR_SERIALIZER.deserialize(input, NULL_SERIALIZER);
      attributes.add(pair);
    }

    int sortSize = input.readInt();
    List<NVPair> sortAttributes = sortSize > 0 ? new ArrayList<NVPair>() : Collections.EMPTY_LIST;

    for (int i = 0; i < sortSize; i++) {
      NVPair pair = NVPAIR_SERIALIZER.deserialize(input, NULL_SERIALIZER);
      sortAttributes.add(pair);
    }
    return builder().setAttributes(attributes).setSortAttributes(sortAttributes);
  }

  static IndexQueryResultSerializer getInstance(boolean isGroupBy) {
    return isGroupBy ? new GroupedQueryResultSerializer() : new NonGroupedQueryResultSerializer();
  }
}

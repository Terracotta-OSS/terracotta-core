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
import com.terracottatech.search.IndexQueryResultImpl;
import com.terracottatech.search.NVPair;
import com.terracottatech.search.ValueID;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class IndexQueryResultSerializer {

  private static final NVPairSerializer       NVPAIR_SERIALIZER = new NVPairSerializer();
  private static final ObjectStringSerializer NULL_SERIALIZER   = new NullObjectStringSerializer();

  public void serialize(IndexQueryResult result, TCByteBufferOutput output) {
    output.writeString(result.getKey());
    output.writeLong(result.getValue().toLong());

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

  public IndexQueryResult deserializeFrom(TCByteBufferInput input) throws IOException {
    String key = input.readString();
    ValueID valueID = new ValueID(input.readLong());
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

    return new IndexQueryResultImpl(key, valueID, attributes, sortAttributes);
  }
}

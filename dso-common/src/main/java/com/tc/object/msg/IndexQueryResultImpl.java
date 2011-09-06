/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.msg;

import com.tc.io.TCByteBufferInput;
import com.tc.io.TCByteBufferOutput;
import com.tc.object.ObjectID;
import com.tc.object.dna.impl.NullObjectStringSerializer;
import com.tc.object.dna.impl.ObjectStringSerializer;
import com.tc.object.metadata.AbstractNVPair;
import com.tc.object.metadata.NVPair;
import com.tc.search.IndexQueryResult;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class IndexQueryResultImpl implements IndexQueryResult, Comparable {

  private static final ObjectStringSerializer NULL_SERIALIZER = new NullObjectStringSerializer();

  private String                              key;
  private List<NVPair>                        attributes;
  private List<NVPair>                        sortAttributes;
  private ObjectID                            valueOID        = ObjectID.NULL_ID;

  public IndexQueryResultImpl() {
    // do nothing.
  }

  public IndexQueryResultImpl(String key, ObjectID valueOID, List<NVPair> attributes, List<NVPair> sortAttributes) {
    this.key = key;
    this.valueOID = valueOID;
    this.attributes = attributes;
    this.sortAttributes = sortAttributes;
  }

  /**
   * {@inheritDoc}
   */
  public String getKey() {
    return this.key;
  }

  public ObjectID getValue() {
    return valueOID;
  }

  /**
   * {@inheritDoc}
   */
  public List<NVPair> getAttributes() {
    return Collections.unmodifiableList(this.attributes);
  }

  /**
   * {@inheritDoc}
   */
  public List<NVPair> getSortAttributes() {
    return Collections.unmodifiableList(this.sortAttributes);
  }

  @Override
  public int hashCode() {
    return key.hashCode();
  }

  public Object deserializeFrom(TCByteBufferInput input) throws IOException {
    this.key = input.readString();
    this.valueOID = new ObjectID(input.readLong());
    int size = input.readInt();

    this.attributes = size > 0 ? new ArrayList<NVPair>() : Collections.EMPTY_LIST;

    for (int i = 0; i < size; i++) {
      NVPair pair = AbstractNVPair.deserializeInstance(input, NULL_SERIALIZER);
      this.attributes.add(pair);
    }

    int sortSize = input.readInt();
    this.sortAttributes = sortSize > 0 ? new ArrayList<NVPair>() : Collections.EMPTY_LIST;

    for (int i = 0; i < sortSize; i++) {
      NVPair pair = AbstractNVPair.deserializeInstance(input, NULL_SERIALIZER);
      this.sortAttributes.add(pair);
    }
    return this;
  }

  public void serializeTo(TCByteBufferOutput output) {
    output.writeString(this.key);
    output.writeLong(this.valueOID.toLong());
    output.writeInt(this.attributes.size());
    for (NVPair pair : this.attributes) {
      pair.serializeTo(output, NULL_SERIALIZER);
    }

    output.writeInt(this.sortAttributes.size());
    for (NVPair pair : this.sortAttributes) {
      pair.serializeTo(output, NULL_SERIALIZER);
    }
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;
    IndexQueryResultImpl other = (IndexQueryResultImpl) obj;
    if (key == null) {
      if (other.key != null) return false;
    } else if (!key.equals(other.key)) return false;
    return true;
  }

  public int compareTo(Object o) {
    if (this == o) return 0;
    if (o == null) return -1;
    if (getClass() != o.getClass()) return -1;
    IndexQueryResultImpl other = (IndexQueryResultImpl) o;
    if (key == null) {
      if (other.key != null) return -1;
    }
    return other.key.compareTo(key);
  }

  @Override
  public String toString() {
    return "SearchQueryResultImpl [attributes=" + attributes + ", key=" + key + "]";
  }

}

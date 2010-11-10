/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.msg;

import com.tc.io.TCByteBufferInput;
import com.tc.io.TCByteBufferOutput;
import com.tc.object.metadata.AbstractNVPair;
import com.tc.object.metadata.NVPair;
import com.tc.search.IndexQueryResult;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class IndexQueryResultImpl implements IndexQueryResult {

  private List<NVPair> attributes;
  private String       key;

  public IndexQueryResultImpl() {
    // do nothing.
  }

  public IndexQueryResultImpl(String key, List<NVPair> attributes) {
    this.key = key;
    this.attributes = attributes;
  }

  /**
   * {@inheritDoc}
   */
  public String getKey() {
    return this.key;
  }

  /**
   * {@inheritDoc}
   */
  public List<NVPair> getAttributes() {
    return Collections.unmodifiableList(this.attributes);
  }

  @Override
  public int hashCode() {
    return key.hashCode();
  }

  public Object deserializeFrom(TCByteBufferInput input) throws IOException {
    this.attributes = new ArrayList<NVPair>();
    this.key = input.readString();
    int size = input.readInt();
    for (int i = 0; i < size; i++) {
      NVPair pair = AbstractNVPair.deserializeInstance(input);
      this.attributes.add(pair);
    }
    return this;
  }

  public void serializeTo(TCByteBufferOutput output) {
    output.writeString(this.key);
    output.writeInt(this.attributes.size());
    for (NVPair pair : this.attributes) {
      pair.serializeTo(output);
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

  @Override
  public String toString() {
    return "SearchQueryResultImpl [attributes=" + attributes + ", key=" + key + "]";
  }

}

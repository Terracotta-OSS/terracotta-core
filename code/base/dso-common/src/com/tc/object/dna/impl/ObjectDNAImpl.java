/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.dna.impl;

import com.tc.io.TCByteBufferInputStream;

import java.io.IOException;

public class ObjectDNAImpl extends DNAImpl {

  private long objectversion;
  
  public ObjectDNAImpl(ObjectStringSerializer serializer, boolean createOutput) {
    super(serializer, createOutput);
  }
  
  public Object deserializeFrom(TCByteBufferInputStream serialInput) throws IOException {
    super.deserializeFrom(serialInput);
    objectversion = this.input.readLong();
    return this;
  }
  
  public long getVersion() {
    return objectversion;
  }

}

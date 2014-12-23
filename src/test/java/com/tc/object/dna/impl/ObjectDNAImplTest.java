/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.dna.impl;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.tc.io.TCByteBufferOutputStream;
import com.tc.object.ObjectID;
import com.tc.object.dna.api.DNAEncodingInternal;
import com.tc.object.dna.api.DNAWriter;

public class ObjectDNAImplTest extends DNAImplTest {

  private final long version = 69;

  @Test @Override
  public void testParentID() throws Exception {
    super.serializeDeserialize(true, false);
    assertEquals(version, dna.getVersion());
  }

  @Test @Override
  public void testArrayLength() throws Exception {
    super.serializeDeserialize(false, false);
    assertEquals(version, dna.getVersion());
  }

  @Override
  protected DNAImpl createDNAImpl(ObjectStringSerializer serializer) {
    return new ObjectDNAImpl(serializer, true);
  }

  @Override
  protected DNAWriter createDNAWriter(TCByteBufferOutputStream out, ObjectID id, String type,
                                      ObjectStringSerializer serializer, DNAEncodingInternal encoding, boolean isDelta) {
    return new ObjectDNAWriterImpl(out, id, type, serializer, encoding, version, isDelta);
  }

}

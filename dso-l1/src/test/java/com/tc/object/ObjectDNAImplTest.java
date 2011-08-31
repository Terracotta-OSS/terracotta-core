/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object;

import com.tc.io.TCByteBufferOutputStream;
import com.tc.object.dna.api.DNAEncodingInternal;
import com.tc.object.dna.api.DNAWriterInternal;
import com.tc.object.dna.impl.DNAImpl;
import com.tc.object.dna.impl.ObjectDNAImpl;
import com.tc.object.dna.impl.ObjectDNAWriterImpl;
import com.tc.object.dna.impl.ObjectStringSerializer;

public class ObjectDNAImplTest extends DNAImplTest {

  private final long version = 69;

  @Override
  public void testParentID() throws Exception {
    super.serializeDeserialize(true, false);
    assertEquals(version, dna.getVersion());
  }

  @Override
  public void testArrayLength() throws Exception {
    super.serializeDeserialize(false, false);
    assertEquals(version, dna.getVersion());
  }

  @Override
  protected DNAImpl createDNAImpl(ObjectStringSerializer serializer) {
    return new ObjectDNAImpl(serializer, true);
  }

  @Override
  protected DNAWriterInternal createDNAWriter(TCByteBufferOutputStream out, ObjectID id, String type,
                                              ObjectStringSerializer serializer, DNAEncodingInternal encoding,
                                              boolean isDelta) {
    return new ObjectDNAWriterImpl(out, id, type, serializer, encoding, "loader description", version, isDelta);
  }

}

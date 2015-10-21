/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Terracotta Core.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
 */
package com.tc.object.dna.impl;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.tc.io.TCByteBufferOutputStream;
import com.tc.object.EntityID;
import com.tc.object.dna.api.DNAEncodingInternal;
import com.tc.object.dna.api.DNAWriter;

public class ObjectDNAImplTest extends DNAImplTest {

  private final long version = 69;


  @Test @Override
  public void testArrayLength() throws Exception {
    super.serializeDeserialize(false);
    assertEquals(version, dna.getVersion());
  }

  @Override
  protected DNAImpl createDNAImpl(ObjectStringSerializer serializer) {
    return new ObjectDNAImpl(serializer, true);
  }

  @Override
  protected DNAWriter createDNAWriter(TCByteBufferOutputStream out, EntityID id, String type,
                                      ObjectStringSerializer serializer, DNAEncodingInternal encoding, boolean isDelta) {
    return new ObjectDNAWriterImpl(out, id, type, serializer, encoding, version, isDelta);
  }

}

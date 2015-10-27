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

import java.util.Random;

import com.tc.bytes.TCByteBuffer;
import com.tc.io.TCByteBufferInputStream;
import com.tc.io.TCByteBufferOutputStream;
import com.tc.object.dna.api.DNAEncoding;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class SerializerDNAEncodingImplTest {

  Random        rnd           = new Random();

  private DNAEncoding getSerializerEncoder() {
    return new SerializerDNAEncodingImpl();
  }

  @SuppressWarnings("resource")
  @Test
  public void testClassSerialize() throws Exception {
    TCByteBufferOutputStream output = new TCByteBufferOutputStream();

    DNAEncoding encoding = getSerializerEncoder();
    encoding.encode(getClass(), output);
    Class<?> c = Object.class;
    UTF8ByteDataHolder name = new UTF8ByteDataHolder(c.getName());
    ClassInstance ci = new ClassInstance(name);
    encoding.encode(ci, output);

    TCByteBuffer[] data = output.toArray();

    encoding = getSerializerEncoder();
    TCByteBufferInputStream input = new TCByteBufferInputStream(data);
    c = (Class<?>) encoding.decode(input);
    assertEquals(getClass(), c);
    ClassInstance holder = (ClassInstance) encoding.decode(input);
    assertEquals(ci, holder);
    assertEquals(0, input.available());
  }

}

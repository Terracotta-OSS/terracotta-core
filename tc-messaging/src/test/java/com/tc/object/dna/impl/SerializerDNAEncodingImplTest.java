/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
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

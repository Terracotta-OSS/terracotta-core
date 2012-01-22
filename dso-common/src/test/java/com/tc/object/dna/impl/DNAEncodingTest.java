/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.dna.impl;

import com.tc.bytes.TCByteBuffer;
import com.tc.io.TCByteBufferInputStream;
import com.tc.io.TCByteBufferOutputStream;
import com.tc.object.bytecode.MockClassProvider;
import com.tc.object.dna.api.DNAEncoding;
import com.tc.object.loaders.ClassProvider;

import java.util.Random;

import junit.framework.TestCase;

public class DNAEncodingTest extends TestCase {

  Random        rnd           = new Random();
  ClassProvider classProvider = new MockClassProvider();

//  private DNAEncoding getStorageEncoder() {
//    return new DNAEncodingImpl(DNAEncoding.STORAGE);
//  }

  private DNAEncoding getSerializerEncoder() {
    return new SerializerDNAEncodingImpl();
  }

  public void testClassSerialize() throws Exception {
    TCByteBufferOutputStream output = new TCByteBufferOutputStream();

    DNAEncoding encoding = getSerializerEncoder();
    encoding.encode(getClass(), output);
    Class c = Object.class;
    UTF8ByteDataHolder name = new UTF8ByteDataHolder(c.getName());
    UTF8ByteDataHolder def = new UTF8ByteDataHolder(classProvider.getLoaderDescriptionFor(c).toDelimitedString());
    ClassInstance ci = new ClassInstance(name, def);
    encoding.encode(ci, output);

    TCByteBuffer[] data = output.toArray();

    encoding = getSerializerEncoder();
    TCByteBufferInputStream input = new TCByteBufferInputStream(data);
    c = (Class) encoding.decode(input);
    assertEquals(getClass(), c);
    ClassInstance holder = (ClassInstance) encoding.decode(input);
    assertEquals(ci, holder);
    assertEquals(0, input.available());
  }



}

/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.io.serializer.impl;

import com.tc.io.serializer.TCObjectInputStream;
import com.tc.io.serializer.TCObjectOutputStream;
import com.tc.io.serializer.api.StringIndex;
import com.tc.objectserver.persistence.impl.NullStringIndexPersistor;
import com.tc.objectserver.persistence.impl.StringIndexImpl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import junit.framework.TestCase;

public class StringSerializerTest extends TestCase {
  public void test() throws Exception {
    StringIndex stringIndex = new StringIndexImpl(new NullStringIndexPersistor());
    StringSerializer ss = new StringSerializer(stringIndex);
    
    ByteArrayOutputStream baout = new ByteArrayOutputStream();
    for (int i=0; i<100; i++) {
      String test = "This is a nice test string: " + i;
      TCObjectOutputStream out = new TCObjectOutputStream(baout);
      ss.serializeTo(test, out);
      out.flush();
      
      ByteArrayInputStream bain = new ByteArrayInputStream(baout.toByteArray());
      assertEquals(test, ss.deserializeFrom(new TCObjectInputStream(bain)));
      baout.reset();
    }
  }
}

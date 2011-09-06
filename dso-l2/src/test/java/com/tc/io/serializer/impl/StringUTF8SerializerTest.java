/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.io.serializer.impl;

import com.tc.io.serializer.TCObjectInputStream;
import com.tc.io.serializer.TCObjectOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import junit.framework.TestCase;

public class StringUTF8SerializerTest extends TestCase {
  public void test() throws Exception {
    StringUTFSerializer ss = new StringUTFSerializer();
    
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

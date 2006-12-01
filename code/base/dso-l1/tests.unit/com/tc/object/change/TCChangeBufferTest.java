/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.change;

import com.tc.io.TCByteBufferInputStream;
import com.tc.io.TCByteBufferOutputStream;
import com.tc.object.MockTCObject;
import com.tc.object.ObjectID;
import com.tc.object.SerializationUtil;
import com.tc.object.bytecode.MockClassProvider;
import com.tc.object.dna.api.DNACursor;
import com.tc.object.dna.api.LogicalAction;
import com.tc.object.dna.api.PhysicalAction;
import com.tc.object.dna.impl.DNAEncoding;
import com.tc.object.dna.impl.DNAImpl;
import com.tc.object.dna.impl.ObjectStringSerializer;
import com.tc.object.loaders.ClassProvider;

import junit.framework.TestCase;

public class TCChangeBufferTest extends TestCase {

  public void testLogicalClassIgnoresPhysicalChanges() throws Exception {
    ObjectStringSerializer serializer = new ObjectStringSerializer();
    ClassProvider classProvider = new MockClassProvider();
    DNAEncoding encoding = new DNAEncoding(classProvider);
    TCChangeBuffer buffer = new TCChangeBufferImpl(new MockTCObject(new ObjectID(1), this, false, true));

    // physical updates should be ignored
    buffer.fieldChanged("classname", "fieldname", new ObjectID(12), -1);
    buffer.fieldChanged("classname", "fieldname", new Long(3), -1);

    buffer.logicalInvoke(SerializationUtil.PUT, new Object[] { new ObjectID(1), new ObjectID(2) });

    TCByteBufferOutputStream output = new TCByteBufferOutputStream();
    buffer.writeTo(output, serializer, encoding);
    output.close();

    DNAImpl dna = new DNAImpl(serializer, true);
    dna.deserializeFrom(new TCByteBufferInputStream(output.toArray()));

    int count = 0;
    DNACursor cursor = dna.getCursor();
    while (cursor.next(encoding)) {
      count++;
      LogicalAction action = dna.getLogicalAction();

      if (action.getMethod() == SerializationUtil.PUT) {
        assertEquals(new ObjectID(1), action.getParameters()[0]);
        assertEquals(new ObjectID(2), action.getParameters()[1]);
      } else {
        fail("method was " + action.getMethod());
      }
    }

    assertEquals(1, count);
  }

  public void testLastPhysicalChangeWins() throws Exception {
    ObjectStringSerializer serializer = new ObjectStringSerializer();
    ClassProvider classProvider = new MockClassProvider();
    DNAEncoding encoding = new DNAEncoding(classProvider);
    TCChangeBuffer buffer = new TCChangeBufferImpl(new MockTCObject(new ObjectID(1), this));

    for (int i = 0; i < 100; i++) {
      buffer.fieldChanged("class", "class.field", new ObjectID(i), -1);
    }

    TCByteBufferOutputStream output = new TCByteBufferOutputStream();
    buffer.writeTo(output, serializer, encoding);
    output.close();

    DNAImpl dna = new DNAImpl(serializer, true);
    dna.deserializeFrom(new TCByteBufferInputStream(output.toArray()));

    int count = 0;
    DNACursor cursor = dna.getCursor();
    while (cursor.next(encoding)) {
      count++;
      PhysicalAction action = dna.getPhysicalAction();

      if (action.isTruePhysical() && action.getFieldName().equals("class.field")) {
        assertEquals(new ObjectID(99), action.getObject());
      } else {
        fail();
      }
    }

    assertEquals(1, count);
  }

  public void testLastArrayChangeWins() throws Exception {
    ObjectStringSerializer serializer = new ObjectStringSerializer();
    ClassProvider classProvider = new MockClassProvider();
    DNAEncoding encoding = new DNAEncoding(classProvider);
    TCChangeBuffer buffer = new TCChangeBufferImpl(new MockTCObject(new ObjectID(1), this, true, false));

    for (int i = 0; i < 100; i++) {
      buffer.fieldChanged("class", "class.arrayField", new ObjectID(1000 + i), 1);
    }

    TCByteBufferOutputStream output = new TCByteBufferOutputStream();
    buffer.writeTo(output, serializer, encoding);
    output.close();

    DNAImpl dna = new DNAImpl(serializer, true);
    dna.deserializeFrom(new TCByteBufferInputStream(output.toArray()));

    int count = 0;
    DNACursor cursor = dna.getCursor();
    while (cursor.next(encoding)) {
      count++;
      PhysicalAction action = dna.getPhysicalAction();

      if (action.isArrayElement() && action.getArrayIndex() == 1) {
        assertEquals(new ObjectID(1099), action.getObject());
      } else {
        fail();
      }
    }

    assertEquals(1, count);
  }

}

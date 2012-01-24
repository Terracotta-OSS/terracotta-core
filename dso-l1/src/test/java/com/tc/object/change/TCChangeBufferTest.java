/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.change;

import com.tc.io.TCByteBufferInputStream;
import com.tc.io.TCByteBufferOutputStream;
import com.tc.object.ApplicatorDNAEncodingImpl;
import com.tc.object.MockTCObject;
import com.tc.object.ObjectID;
import com.tc.object.bytecode.MockClassProvider;
import com.tc.object.dna.api.DNACursor;
import com.tc.object.dna.api.DNAEncodingInternal;
import com.tc.object.dna.api.DNAWriter;
import com.tc.object.dna.api.PhysicalAction;
import com.tc.object.dna.impl.DNAImpl;
import com.tc.object.dna.impl.DNAWriterImpl;
import com.tc.object.dna.impl.ObjectStringSerializer;
import com.tc.object.dna.impl.ObjectStringSerializerImpl;
import com.tc.object.loaders.ClassProvider;

import junit.framework.TestCase;

public class TCChangeBufferTest extends TestCase {

  public void testLastPhysicalChangeWins() throws Exception {
    final ObjectStringSerializer serializer = new ObjectStringSerializerImpl();
    final ClassProvider classProvider = new MockClassProvider();
    final DNAEncodingInternal encoding = new ApplicatorDNAEncodingImpl(classProvider);
    final MockTCObject mockTCObject = new MockTCObject(new ObjectID(1), this);
    final TCChangeBuffer buffer = new TCChangeBufferImpl(mockTCObject);

    for (int i = 0; i < 100; i++) {
      buffer.fieldChanged("class", "class.field", new ObjectID(i), -1);
    }

    final TCByteBufferOutputStream output = new TCByteBufferOutputStream();
    final DNAWriter writer = new DNAWriterImpl(output, mockTCObject.getObjectID(), mockTCObject.getTCClass().getName(),
                                               serializer, encoding, false);

    buffer.writeTo(writer);
    writer.markSectionEnd();
    writer.finalizeHeader();
    output.close();

    final DNAImpl dna = new DNAImpl(serializer, true);
    dna.deserializeFrom(new TCByteBufferInputStream(output.toArray()));

    int count = 0;
    final DNACursor cursor = dna.getCursor();
    while (cursor.next(encoding)) {
      count++;
      final PhysicalAction action = dna.getPhysicalAction();

      if (action.isTruePhysical() && action.getFieldName().equals("class.field")) {
        assertEquals(new ObjectID(99), action.getObject());
      } else {
        fail();
      }
    }

    assertEquals(1, count);
  }

  public void testLastArrayChangeWins() throws Exception {
    final ObjectStringSerializer serializer = new ObjectStringSerializerImpl();
    final ClassProvider classProvider = new MockClassProvider();
    final DNAEncodingInternal encoding = new ApplicatorDNAEncodingImpl(classProvider);
    final MockTCObject mockTCObject = new MockTCObject(new ObjectID(1), this, true, false);
    final TCChangeBuffer buffer = new TCChangeBufferImpl(mockTCObject);

    for (int i = 0; i < 100; i++) {
      buffer.fieldChanged("class", "class.arrayField", new ObjectID(1000 + i), 1);
    }

    final TCByteBufferOutputStream output = new TCByteBufferOutputStream();

    final DNAWriter writer = new DNAWriterImpl(output, mockTCObject.getObjectID(), mockTCObject.getTCClass().getName(),
                                               serializer, encoding, false);

    buffer.writeTo(writer);
    writer.markSectionEnd();
    writer.finalizeHeader();
    output.close();

    final DNAImpl dna = new DNAImpl(serializer, true);
    dna.deserializeFrom(new TCByteBufferInputStream(output.toArray()));

    int count = 0;
    final DNACursor cursor = dna.getCursor();
    while (cursor.next(encoding)) {
      count++;
      final PhysicalAction action = dna.getPhysicalAction();

      if (action.isArrayElement() && action.getArrayIndex() == 1) {
        assertEquals(new ObjectID(1099), action.getObject());
      } else {
        fail();
      }
    }

    assertEquals(1, count);
  }
}

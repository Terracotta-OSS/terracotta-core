/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.dna.impl;

import com.tc.io.TCByteBufferInputStream;
import com.tc.io.TCByteBufferOutputStream;
import com.tc.object.ObjectID;
import com.tc.object.bytecode.MockClassProvider;
import com.tc.object.dna.api.DNACursor;
import com.tc.object.dna.api.DNAWriter;
import com.tc.object.dna.api.DNAEncoding;
import com.tc.object.dna.api.LogicalAction;
import com.tc.object.dna.api.PhysicalAction;
import com.tc.object.loaders.ClassProvider;

import java.util.Arrays;

import junit.framework.TestCase;

public class DNAImplTest extends TestCase {

  protected DNAImpl dna;

  public void testSerializeDeserialize() throws Exception {
    TCByteBufferOutputStream out = new TCByteBufferOutputStream();

    final ObjectID id = new ObjectID(1);
    final ObjectID pid = new ObjectID(2);
    final String type = getClass().getName();
    final int arrayLen = 42;
    ObjectStringSerializer serializer = new ObjectStringSerializer();
    ClassProvider classProvider = new MockClassProvider();
    DNAEncoding encoding = new DNAEncodingImpl(classProvider);
    DNAWriter dnaWriter = createDNAWriter(out, id, type, serializer, encoding, "loader description");
    PhysicalAction action1 = new PhysicalAction("class.field1", new Integer(1), false);
    LogicalAction action2 = new LogicalAction(12, new Object[] { "key", "value" });
    PhysicalAction action3 = new PhysicalAction("class.field2", new ObjectID(3), true);
    dnaWriter.addPhysicalAction(action1.getFieldName(), action1.getObject());
    dnaWriter.addLogicalAction(action2.getMethod(), action2.getParameters());
    dnaWriter.addPhysicalAction(action3.getFieldName(), action3.getObject());
    dnaWriter.setParentObjectID(pid);
    dnaWriter.setArrayLength(arrayLen);
    dnaWriter.finalizeDNA();

    TCByteBufferInputStream in = new TCByteBufferInputStream(out.toArray());
    dna = createDNAImpl(serializer, true);
    assertSame(dna, dna.deserializeFrom(in));
    assertEquals(0, in.available());
    DNACursor cursor = dna.getCursor();
    int count = 1;
    while (cursor.next(encoding)) {
      switch (count) {
        case 1:
          compareAction(action1, cursor.getPhysicalAction());
          break;
        case 2:
          compareAction(action2, cursor.getLogicalAction());
          break;
        case 3:
          compareAction(action3, cursor.getPhysicalAction());
          break;
        default:
          fail("count got to " + count);
      }
      count++;
    }

    assertEquals(id, dna.getObjectID());
    assertEquals(pid, dna.getParentObjectID());
    assertEquals(type, dna.getTypeName());
    assertEquals(arrayLen, dna.getArraySize());
    assertOverridable();
  }

  protected void assertOverridable() {
    assertTrue(dna.isDelta());
  }

  protected DNAImpl createDNAImpl(ObjectStringSerializer serializer, boolean b) {
    return new DNAImpl(serializer, b);
  }

  protected DNAWriter createDNAWriter(TCByteBufferOutputStream out, ObjectID id, String type,
                                      ObjectStringSerializer serializer, DNAEncoding encoding, String string) {
    return new DNAWriterImpl(out, id, type, serializer, encoding, "loader description", true);
  }

  private void compareAction(LogicalAction expect, LogicalAction actual) {
    assertEquals(expect.getMethod(), actual.getMethod());
    assertTrue(Arrays.equals(expect.getParameters(), actual.getParameters()));
  }

  private void compareAction(PhysicalAction expect, PhysicalAction actual) {
    assertEquals(expect.getFieldName(), actual.getFieldName());
    assertEquals(expect.getObject(), actual.getObject());
    assertEquals(expect.isReference(), actual.isReference());
  }

}

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
import com.tc.object.dna.api.LogicalAction;
import com.tc.object.dna.api.PhysicalAction;
import com.tc.object.loaders.ClassProvider;

import java.util.Arrays;

import junit.framework.TestCase;

public class VersionizedDNAWrapperTest extends TestCase {

  public void testResettingDNACursor() throws Exception {
    TCByteBufferOutputStream out = new TCByteBufferOutputStream();

    final ObjectID id = new ObjectID(1);
    final ObjectID pid = new ObjectID(2);
    final String type = getClass().getName();
    final int arrayLen = 42;
    ObjectStringSerializer serializer = new ObjectStringSerializer();
    ClassProvider classProvider = new MockClassProvider();
    DNAEncoding encoding = new DNAEncoding(classProvider);
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
    DNAImpl dna = createDNAImpl(serializer, true);
    assertSame(dna, dna.deserializeFrom(in));
    assertEquals(0, in.available());
    
    VersionizedDNAWrapper vdna = new VersionizedDNAWrapper(dna, 10);
    assertEquals(10, vdna.getVersion());
    try {
      vdna.getCursor().reset();
      assertTrue(false);
    } catch(UnsupportedOperationException use) {
      // this is expected
    }
    
    vdna = new VersionizedDNAWrapper(dna, 10, true);
    DNACursor cursor = vdna.getCursor();
    cursor.reset();
    assertTrue(cursor.next(encoding));
    compareAction(action1, cursor.getPhysicalAction());
    cursor.reset();
    assertTrue(cursor.next(encoding));
    compareAction(action1, cursor.getPhysicalAction());
    assertTrue(cursor.next(encoding));
    compareAction(action2, cursor.getLogicalAction());
    cursor.reset();
    assertTrue(cursor.next(encoding));
    compareAction(action1, cursor.getPhysicalAction());
    assertTrue(cursor.next(encoding));
    compareAction(action2, cursor.getLogicalAction());
    assertTrue(cursor.next(encoding));
    compareAction(action3, cursor.getPhysicalAction());
    assertFalse(cursor.next(encoding));
    cursor.reset();
    assertTrue(cursor.next(encoding));
    compareAction(action1, cursor.getPhysicalAction());
    assertTrue(cursor.next(encoding));
    compareAction(action2, cursor.getLogicalAction());
    assertTrue(cursor.next(encoding));
    compareAction(action3, cursor.getPhysicalAction());
    assertFalse(cursor.next(encoding));
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

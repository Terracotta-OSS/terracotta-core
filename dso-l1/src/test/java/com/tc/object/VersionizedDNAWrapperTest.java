/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object;

import com.tc.io.TCByteBufferInputStream;
import com.tc.io.TCByteBufferOutputStream;
import com.tc.object.bytecode.MockClassProvider;
import com.tc.object.dna.api.DNACursor;
import com.tc.object.dna.api.DNAEncodingInternal;
import com.tc.object.dna.api.DNAWriter;
import com.tc.object.dna.api.LogicalAction;
import com.tc.object.dna.api.PhysicalAction;
import com.tc.object.dna.impl.DNAImpl;
import com.tc.object.dna.impl.DNAWriterImpl;
import com.tc.object.dna.impl.ObjectStringSerializer;
import com.tc.object.dna.impl.ObjectStringSerializerImpl;
import com.tc.object.dna.impl.VersionizedDNAWrapper;
import com.tc.object.loaders.ClassProvider;

import java.util.Arrays;

import junit.framework.TestCase;

public class VersionizedDNAWrapperTest extends TestCase {

  public void testResettingDNACursor() throws Exception {
    final TCByteBufferOutputStream out = new TCByteBufferOutputStream();

    final ObjectID id = new ObjectID(1);
    final ObjectID pid = new ObjectID(2);
    final String type = getClass().getName();

    final ObjectStringSerializer serializer = new ObjectStringSerializerImpl();
    final ClassProvider classProvider = new MockClassProvider();
    final DNAEncodingInternal encoding = new ApplicatorDNAEncodingImpl(classProvider);
    final DNAWriter dnaWriter = createDNAWriter(out, id, type, serializer, encoding, "loader description");
    final PhysicalAction action1 = new PhysicalAction("class.field1", new Integer(1), false);
    final LogicalAction action2 = new LogicalAction(12, new Object[] { "key", "value" });
    final PhysicalAction action3 = new PhysicalAction("class.field2", new ObjectID(3), true);
    dnaWriter.setParentObjectID(pid);
    dnaWriter.addPhysicalAction(action1.getFieldName(), action1.getObject());
    dnaWriter.addLogicalAction(action2.getMethod(), action2.getParameters());
    dnaWriter.addPhysicalAction(action3.getFieldName(), action3.getObject());
    dnaWriter.markSectionEnd();
    dnaWriter.finalizeHeader();

    final TCByteBufferInputStream in = new TCByteBufferInputStream(out.toArray());
    final DNAImpl dna = createDNAImpl(serializer, true);
    assertSame(dna, dna.deserializeFrom(in));
    assertEquals(0, in.available());

    VersionizedDNAWrapper vdna = new VersionizedDNAWrapper(dna, 10);
    assertEquals(10, vdna.getVersion());
    try {
      vdna.getCursor().reset();
      assertTrue(false);
    } catch (final UnsupportedOperationException use) {
      // this is expected
    }

    vdna = new VersionizedDNAWrapper(dna, 10, true);
    final DNACursor cursor = vdna.getCursor();
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

  protected DNAImpl createDNAImpl(final ObjectStringSerializer serializer, final boolean b) {
    return new DNAImpl(serializer, b);
  }

  protected DNAWriter createDNAWriter(final TCByteBufferOutputStream out, final ObjectID id, final String type,
                                      final ObjectStringSerializer serializer, final DNAEncodingInternal encoding,
                                      final String string) {
    return new DNAWriterImpl(out, id, type, serializer, encoding, "loader description", false);
  }

  private void compareAction(final LogicalAction expect, final LogicalAction actual) {
    assertEquals(expect.getMethod(), actual.getMethod());
    assertTrue(Arrays.equals(expect.getParameters(), actual.getParameters()));
  }

  private void compareAction(final PhysicalAction expect, final PhysicalAction actual) {
    assertEquals(expect.getFieldName(), actual.getFieldName());
    assertEquals(expect.getObject(), actual.getObject());
    assertEquals(expect.isReference(), actual.isReference());
  }

}

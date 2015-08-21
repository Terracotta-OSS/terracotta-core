/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.dna.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import org.junit.Test;

import com.tc.io.TCByteBufferInputStream;
import com.tc.io.TCByteBufferOutputStream;
import com.tc.object.ApplicatorDNAEncodingImpl;
import com.tc.object.EntityID;
import com.tc.object.LogicalOperation;
import com.tc.object.ObjectID;
import com.tc.object.dna.api.DNACursor;
import com.tc.object.dna.api.DNAEncodingInternal;
import com.tc.object.dna.api.DNAWriter;
import com.tc.object.dna.api.LogicalAction;
import com.tc.object.dna.api.LogicalChangeID;
import com.tc.object.dna.api.PhysicalAction;
import com.tc.object.loaders.ClassProvider;

import java.util.Arrays;

public class VersionizedDNAWrapperTest {

  @SuppressWarnings("resource")
  @Test
  public void testResettingDNACursor() throws Exception {
    final TCByteBufferOutputStream out = new TCByteBufferOutputStream();

    final EntityID id = new EntityID("foo", "bar");
    final String type = getClass().getName();

    final ObjectStringSerializer serializer = new ObjectStringSerializerImpl();
    final ClassProvider classProvider = mock(ClassProvider.class);
    final DNAEncodingInternal encoding = new ApplicatorDNAEncodingImpl(classProvider);
    final DNAWriter dnaWriter = createDNAWriter(out, id, type, serializer, encoding, "loader description");
    final PhysicalAction action1 = new PhysicalAction("class.field1", new Integer(1), false);
    final LogicalAction action2 = new LogicalAction(LogicalOperation.FOR_TESTING_ONLY, new Object[] { "key", "value" });
    final PhysicalAction action3 = new PhysicalAction("class.field2", new ObjectID(3), true);
    dnaWriter.addPhysicalAction(action1.getFieldName(), action1.getObject());
    dnaWriter.addLogicalAction(action2.getLogicalOperation(), action2.getParameters(), LogicalChangeID.NULL_ID);
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

  protected DNAImpl createDNAImpl(ObjectStringSerializer serializer, boolean b) {
    return new DNAImpl(serializer, b);
  }

  protected DNAWriter createDNAWriter(TCByteBufferOutputStream out, EntityID id, String type,
                                      ObjectStringSerializer serializer, DNAEncodingInternal encoding,
                                      String string) {
    return new DNAWriterImpl(out, id, serializer, encoding, false);
  }

  private void compareAction(LogicalAction expect, LogicalAction actual) {
    assertEquals(expect.getLogicalOperation(), actual.getLogicalOperation());
    assertTrue(Arrays.equals(expect.getParameters(), actual.getParameters()));
  }

  private void compareAction(PhysicalAction expect, PhysicalAction actual) {
    assertEquals(expect.getFieldName(), actual.getFieldName());
    assertEquals(expect.getObject(), actual.getObject());
    assertEquals(expect.isReference(), actual.isReference());
  }

}

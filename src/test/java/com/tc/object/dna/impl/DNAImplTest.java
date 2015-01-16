/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.dna.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;

import org.junit.Test;

import com.tc.io.TCByteBufferInputStream;
import com.tc.io.TCByteBufferOutputStream;
import com.tc.object.ApplicatorDNAEncodingImpl;
import com.tc.object.LogicalOperation;
import com.tc.object.ObjectID;
import com.tc.object.dna.api.DNA;
import com.tc.object.dna.api.DNACursor;
import com.tc.object.dna.api.DNAEncodingInternal;
import com.tc.object.dna.api.DNAWriter;
import com.tc.object.dna.api.LogicalAction;
import com.tc.object.dna.api.LogicalChangeID;
import com.tc.object.dna.api.PhysicalAction;
import com.tc.object.loaders.ClassProvider;
import com.tc.util.Assert;

import java.util.Arrays;

public class DNAImplTest {

  protected DNAImpl dna;

  @Test
  public void testParentID() throws Exception {
    serializeDeserialize(true, false);
  }

  @Test
  public void testArrayLength() throws Exception {
    serializeDeserialize(false, false);
  }

  @Test
  public void testDelta() throws Exception {
    serializeDeserialize(false, true);
  }

  @SuppressWarnings("resource")
  protected void serializeDeserialize(final boolean parentID, final boolean isDelta) throws Exception {
    TCByteBufferOutputStream out = new TCByteBufferOutputStream();

    final ObjectID id = new ObjectID(1);
    final ObjectID pid = new ObjectID(2);
    final String type = getClass().getName();
    final int arrayLen = 42;

    final ObjectStringSerializer serializer = new ObjectStringSerializerImpl();
    final ClassProvider classProvider = mock(ClassProvider.class);
    final DNAEncodingInternal encoding = new ApplicatorDNAEncodingImpl(classProvider);
    final DNAWriter dnaWriter = createDNAWriter(out, id, type, serializer, encoding, isDelta);
    final PhysicalAction action1 = new PhysicalAction("class.field1", new Integer(1), false);
    final LogicalAction action2 = new LogicalAction(LogicalOperation.PUT, new Object[] { "key", "value" });
    final PhysicalAction action3 = new PhysicalAction("class.field2", new ObjectID(3), true);
    final PhysicalAction action4 = new PhysicalAction("class.field3", new ObjectID(4), true);
    final PhysicalAction action5 = new PhysicalAction("class.field4", new ObjectID(5), true);
    final LogicalAction action6 = new LogicalAction(LogicalOperation.PUT_IF_ABSENT, new Object[] { "key", "value" }, new LogicalChangeID(6));

    if (parentID) {
      dnaWriter.setParentObjectID(pid);
    } else {
      dnaWriter.setArrayLength(arrayLen);
    }
    dnaWriter.addPhysicalAction(action1.getFieldName(), action1.getObject());
    dnaWriter.addLogicalAction(action2.getLogicalOperation(), action2.getParameters());
    dnaWriter.addPhysicalAction(action3.getFieldName(), action3.getObject());
    assertTrue(dnaWriter.isContiguous());
    dnaWriter.markSectionEnd();

    // simulate folding
    DNAWriter appender = dnaWriter.createAppender();
    assertTrue(dnaWriter.isContiguous());
    appender.addPhysicalAction(action4.getFieldName(), action4.getObject());
    appender.markSectionEnd();

    // fold in more actions without any meta data
    appender = dnaWriter.createAppender();
    appender.addPhysicalAction(action5.getFieldName(), action5.getObject());
    appender.markSectionEnd();

    appender = dnaWriter.createAppender();
    appender.addLogicalAction(action6.getLogicalOperation(), action6.getParameters(), action6.getLogicalChangeID());
    appender.markSectionEnd();
    // collapse this folded DNA into contiguous buffer
    dnaWriter.finalizeHeader();
    out = new TCByteBufferOutputStream();
    dnaWriter.copyTo(out);

    final TCByteBufferInputStream in = new TCByteBufferInputStream(out.toArray());
    this.dna = createDNAImpl(serializer);
    assertSame(this.dna, this.dna.deserializeFrom(in));
    assertEquals(0, in.available());
    final DNACursor cursor = this.dna.getCursor();
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
        case 4:
          compareAction(action4, cursor.getPhysicalAction());
          break;
        case 5:
          compareAction(action5, cursor.getPhysicalAction());
          break;
        case 6:
          compareAction(action6, cursor.getLogicalAction());
          break;
        default:
          fail("count got to " + count);
      }
      count++;
    }

    if (count != 7) { throw new AssertionError("not enough action seen: " + count); }

    assertEquals(id, this.dna.getObjectID());
    if (parentID) {
      assertEquals(pid, this.dna.getParentObjectID());
      assertEquals(DNA.NULL_ARRAY_SIZE, this.dna.getArraySize());
      assertFalse(this.dna.hasLength());
    } else {
      assertEquals(ObjectID.NULL_ID, this.dna.getParentObjectID());
      assertTrue(this.dna.hasLength());
      assertEquals(arrayLen, this.dna.getArraySize());
    }

    Assert.assertEquals(isDelta, this.dna.isDelta());

    if (!isDelta) {
      assertEquals(type, this.dna.getTypeName());
    }

  }


  protected DNAImpl createDNAImpl(final ObjectStringSerializer serializer) {
    return new DNAImpl(serializer, true);
  }

  protected DNAWriter createDNAWriter(final TCByteBufferOutputStream out, final ObjectID id, final String type,
                                              final ObjectStringSerializer serializer,
                                              final DNAEncodingInternal encoding, final boolean isDelta) {
    return new DNAWriterImpl(out, id, type, serializer, encoding, isDelta);
  }

  private void compareAction(final LogicalAction expect, final LogicalAction actual) {
    assertEquals(expect.getLogicalOperation(), actual.getLogicalOperation());
    assertTrue(Arrays.equals(expect.getParameters(), actual.getParameters()));
    assertEquals(expect.getLogicalChangeID(), actual.getLogicalChangeID());
  }

  private void compareAction(final PhysicalAction expect, final PhysicalAction actual) {
    assertEquals(expect.getFieldName(), actual.getFieldName());
    assertEquals(expect.getObject(), actual.getObject());
    assertEquals(expect.isReference(), actual.isReference());
  }

}

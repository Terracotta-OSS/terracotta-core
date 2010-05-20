/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object;

import com.tc.io.TCByteBufferInputStream;
import com.tc.io.TCByteBufferOutputStream;
import com.tc.object.bytecode.MockClassProvider;
import com.tc.object.dna.api.DNA;
import com.tc.object.dna.api.DNACursor;
import com.tc.object.dna.api.DNAEncoding;
import com.tc.object.dna.api.DNAWriter;
import com.tc.object.dna.api.LogicalAction;
import com.tc.object.dna.api.PhysicalAction;
import com.tc.object.dna.impl.DNAImpl;
import com.tc.object.dna.impl.DNAWriterImpl;
import com.tc.object.dna.impl.ObjectStringSerializer;
import com.tc.object.loaders.ClassProvider;
import com.tc.util.Assert;

import java.util.Arrays;

import junit.framework.TestCase;

public class DNAImplTest extends TestCase {

  protected DNAImpl dna;

  public void testParentID() throws Exception {
    serializeDeserialize(true, false);
  }

  public void testArrayLength() throws Exception {
    serializeDeserialize(false, false);
  }

  public void testDelta() throws Exception {
    serializeDeserialize(false, true);
  }

  protected void serializeDeserialize(final boolean parentID, final boolean isDelta) throws Exception {
    final TCByteBufferOutputStream out = new TCByteBufferOutputStream();

    final ObjectID id = new ObjectID(1);
    final ObjectID pid = new ObjectID(2);
    final String type = getClass().getName();
    final int arrayLen = 42;

    final ObjectStringSerializer serializer = new ObjectStringSerializer();
    final ClassProvider classProvider = new MockClassProvider();
    final DNAEncoding encoding = new ApplicatorDNAEncodingImpl(classProvider);
    final DNAWriter dnaWriter = createDNAWriter(out, id, type, serializer, encoding, isDelta);
    final PhysicalAction action1 = new PhysicalAction("class.field1", new Integer(1), false);
    final LogicalAction action2 = new LogicalAction(12, new Object[] { "key", "value" });
    final PhysicalAction action3 = new PhysicalAction("class.field2", new ObjectID(3), true);

    if (parentID) {
      dnaWriter.setParentObjectID(pid);
    } else {
      dnaWriter.setArrayLength(arrayLen);
    }
    dnaWriter.addPhysicalAction(action1.getFieldName(), action1.getObject());
    dnaWriter.addLogicalAction(action2.getMethod(), action2.getParameters());
    dnaWriter.addPhysicalAction(action3.getFieldName(), action3.getObject());
    dnaWriter.markSectionEnd();
    dnaWriter.finalizeHeader();

    final TCByteBufferInputStream in = new TCByteBufferInputStream(out.toArray());
    this.dna = createDNAImpl(serializer, true);
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
        default:
          fail("count got to " + count);
      }
      count++;
    }

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
      assertEquals("loader description", this.dna.getDefiningLoaderDescription());
    }
  }

  protected DNAImpl createDNAImpl(final ObjectStringSerializer serializer, final boolean b) {
    return new DNAImpl(serializer, b);
  }

  protected DNAWriter createDNAWriter(final TCByteBufferOutputStream out, final ObjectID id, final String type,
                                      final ObjectStringSerializer serializer, final DNAEncoding encoding,
                                      final boolean isDelta) {
    return new DNAWriterImpl(out, id, type, serializer, encoding, "loader description", isDelta);
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

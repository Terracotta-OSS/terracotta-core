/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
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

  protected void serializeDeserialize(boolean parentID, boolean isDelta) throws Exception {
    TCByteBufferOutputStream out = new TCByteBufferOutputStream();

    final ObjectID id = new ObjectID(1);
    final ObjectID pid = new ObjectID(2);
    final String type = getClass().getName();
    final int arrayLen = 42;

    ObjectStringSerializer serializer = new ObjectStringSerializer();
    ClassProvider classProvider = new MockClassProvider();
    DNAEncoding encoding = new ApplicatorDNAEncodingImpl(classProvider);
    DNAWriter dnaWriter = createDNAWriter(out, id, type, serializer, encoding, isDelta);
    PhysicalAction action1 = new PhysicalAction("class.field1", new Integer(1), false);
    LogicalAction action2 = new LogicalAction(12, new Object[] { "key", "value" });
    PhysicalAction action3 = new PhysicalAction("class.field2", new ObjectID(3), true);

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
    if (parentID) {
      assertEquals(pid, dna.getParentObjectID());
      assertEquals(DNA.NULL_ARRAY_SIZE, dna.getArraySize());
      assertFalse(dna.hasLength());
    } else {
      assertEquals(ObjectID.NULL_ID, dna.getParentObjectID());
      assertTrue(dna.hasLength());
      assertEquals(arrayLen, dna.getArraySize());
    }
    
    Assert.assertEquals(isDelta, dna.isDelta());
    
    if(! isDelta) {
      assertEquals(type, dna.getTypeName());
      assertEquals("loader description", dna.getDefiningLoaderDescription());
    }
  }

  protected DNAImpl createDNAImpl(ObjectStringSerializer serializer, boolean b) {
    return new DNAImpl(serializer, b);
  }

  protected DNAWriter createDNAWriter(TCByteBufferOutputStream out, ObjectID id, String type,
                                      ObjectStringSerializer serializer, DNAEncoding encoding, boolean isDelta) {
    return new DNAWriterImpl(out, id, type, serializer, encoding, "loader description", isDelta);
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


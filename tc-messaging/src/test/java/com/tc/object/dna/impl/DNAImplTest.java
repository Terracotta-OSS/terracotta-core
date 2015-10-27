/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Terracotta Core.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
 */
package com.tc.object.dna.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
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
import com.tc.object.dna.api.PhysicalAction;
import com.tc.object.loaders.ClassProvider;
import com.tc.util.Assert;

import java.util.Arrays;

public class DNAImplTest {

  protected DNAImpl dna;

  @Test
  public void testArrayLength() throws Exception {
    serializeDeserialize(false);
  }

  @Test
  public void testDelta() throws Exception {
    serializeDeserialize(true);
  }

  @SuppressWarnings("resource")
  protected void serializeDeserialize(boolean isDelta) throws Exception {
    TCByteBufferOutputStream out = new TCByteBufferOutputStream();

    final EntityID id = new EntityID("foo", "bar");
    final String type = getClass().getName();
    final int arrayLen = 42;

    final ObjectStringSerializer serializer = new ObjectStringSerializerImpl();
    final ClassProvider classProvider = mock(ClassProvider.class);
    final DNAEncodingInternal encoding = new ApplicatorDNAEncodingImpl(classProvider);
    final DNAWriter dnaWriter = createDNAWriter(out, id, type, serializer, encoding, isDelta);
    final PhysicalAction action1 = new PhysicalAction("class.field1", new Integer(1), false);
    final LogicalAction action2 = new LogicalAction(LogicalOperation.FOR_TESTING_ONLY, new Object[] { "key", "value" });
    final PhysicalAction action3 = new PhysicalAction("class.field2", new ObjectID(3), true);
 

    dnaWriter.setArrayLength(arrayLen);
    dnaWriter.addPhysicalAction(action1.getFieldName(), action1.getObject());
    dnaWriter.addLogicalAction(action2.getLogicalOperation(), action2.getParameters());
    dnaWriter.addPhysicalAction(action3.getFieldName(), action3.getObject());
    dnaWriter.markSectionEnd();

    // collapse this DNA into contiguous buffer
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
        default:
          fail("count got to " + count);
      }
      count++;
    }

    if (count != 4) { throw new AssertionError("not enough action seen: " + count); }

    assertEquals(id, this.dna.getEntityID());
    assertTrue(this.dna.hasLength());
    assertEquals(arrayLen, this.dna.getArraySize());

    Assert.assertEquals(isDelta, this.dna.isDelta());

  }


  protected DNAImpl createDNAImpl(ObjectStringSerializer serializer) {
    return new DNAImpl(serializer, true);
  }

  protected DNAWriter createDNAWriter(TCByteBufferOutputStream out, EntityID id, String type,
                                              ObjectStringSerializer serializer,
                                              DNAEncodingInternal encoding, boolean isDelta) {
    return new DNAWriterImpl(out, id, serializer, encoding, isDelta);
  }

  private void compareAction(LogicalAction expect, LogicalAction actual) {
    assertEquals(expect.getLogicalOperation(), actual.getLogicalOperation());
    assertTrue(Arrays.equals(expect.getParameters(), actual.getParameters()));
    assertEquals(expect.getLogicalChangeID(), actual.getLogicalChangeID());
  }

  private void compareAction(PhysicalAction expect, PhysicalAction actual) {
    assertEquals(expect.getFieldName(), actual.getFieldName());
    assertEquals(expect.getObject(), actual.getObject());
    assertEquals(expect.isReference(), actual.isReference());
  }

}

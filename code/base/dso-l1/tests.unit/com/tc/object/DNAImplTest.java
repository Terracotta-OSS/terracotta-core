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
import com.tc.object.dna.api.DNAEncodingInternal;
import com.tc.object.dna.api.DNAWriterInternal;
import com.tc.object.dna.api.LogicalAction;
import com.tc.object.dna.api.MetaDataReader;
import com.tc.object.dna.api.PhysicalAction;
import com.tc.object.dna.impl.DNAImpl;
import com.tc.object.dna.impl.DNAWriterImpl;
import com.tc.object.dna.impl.ObjectStringSerializer;
import com.tc.object.dna.impl.ObjectStringSerializerImpl;
import com.tc.object.loaders.ClassProvider;
import com.tc.object.metadata.MetaDataDescriptorImpl;
import com.tc.object.metadata.MetaDataDescriptorInternal;
import com.tc.object.metadata.NVPair;
import com.tc.object.metadata.ValueType;
import com.tc.util.Assert;

import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;

import junit.framework.TestCase;

public class DNAImplTest extends TestCase {

  protected DNAImpl dna;

  public void testParentID() throws Exception {
    serializeDeserialize(true, false);
  }

  public void testEmptyMetaDataReader() throws Exception {
    TCByteBufferOutputStream out = new TCByteBufferOutputStream();

    final ObjectStringSerializer serializer = new ObjectStringSerializerImpl();
    final ClassProvider classProvider = new MockClassProvider();
    final DNAEncodingInternal encoding = new ApplicatorDNAEncodingImpl(classProvider);
    final DNAWriterInternal dnaWriter = createDNAWriter(out, new ObjectID(1), "foo", serializer, encoding, false);

    dnaWriter.addPhysicalAction("sdfsdf", "bar");
    dnaWriter.markSectionEnd();
    dnaWriter.finalizeHeader();

    this.dna = createDNAImpl(serializer);
    dna.deserializeFrom(new TCByteBufferInputStream(out.toArray()));

    MetaDataReader metaDataReader = dna.getMetaDataReader();
    assertFalse(metaDataReader.iterator().hasNext());
  }

  public void testArrayLength() throws Exception {
    serializeDeserialize(false, false);
  }

  public void testDelta() throws Exception {
    serializeDeserialize(false, true);
  }

  protected void serializeDeserialize(final boolean parentID, final boolean isDelta) throws Exception {
    TCByteBufferOutputStream out = new TCByteBufferOutputStream();

    final ObjectID id = new ObjectID(1);
    final ObjectID pid = new ObjectID(2);
    final String type = getClass().getName();
    final int arrayLen = 42;

    final ObjectStringSerializer serializer = new ObjectStringSerializerImpl();
    final ClassProvider classProvider = new MockClassProvider();
    final DNAEncodingInternal encoding = new ApplicatorDNAEncodingImpl(classProvider);
    final DNAWriterInternal dnaWriter = createDNAWriter(out, id, type, serializer, encoding, isDelta);
    final PhysicalAction action1 = new PhysicalAction("class.field1", new Integer(1), false);
    final LogicalAction action2 = new LogicalAction(12, new Object[] { "key", "value" });
    final PhysicalAction action3 = new PhysicalAction("class.field2", new ObjectID(3), true);
    final PhysicalAction action4 = new PhysicalAction("class.field3", new ObjectID(4), true);
    final PhysicalAction action5 = new PhysicalAction("class.field4", new ObjectID(5), true);

    MetaDataDescriptorInternal md = new MetaDataDescriptorImpl("cat1");
    md.setObjectID(id);
    md.add("name1", 42);

    MetaDataDescriptorInternal md2 = new MetaDataDescriptorImpl("cat2");
    md2.setObjectID(id);
    md2.add("name2", true);
    md2.add("name3", "sdfsdfsdf".getBytes());
    md2.add("name4", (byte) 4);
    md2.add("name5", 'q');
    md2.add("name6", new Date());
    md2.add("name7", Math.PI);
    md2.add("name8", 4.0296432F);
    md2.add("name9", 42);
    md2.add("name10", 666L);
    md2.add("steve", (short) 53);
    md2.add("tim", "rulez");
    md2.add("e-noom", TestEnum.FOO);
    md2.add("null value", (Object) null);
    md2.add("oid", new ObjectID(23));
    md2.add("sql date", new java.sql.Date(4345));

    // make sure we cover all the types (if you add a new type and this is going off then add it to this test) :-)
    assertEquals(ValueType.values().length, md2.numberOfNvPairs());

    if (parentID) {
      dnaWriter.setParentObjectID(pid);
    } else {
      dnaWriter.setArrayLength(arrayLen);
    }
    dnaWriter.addPhysicalAction(action1.getFieldName(), action1.getObject());
    dnaWriter.addLogicalAction(action2.getMethod(), action2.getParameters());
    dnaWriter.addPhysicalAction(action3.getFieldName(), action3.getObject());
    assertTrue(dnaWriter.isContiguous());
    dnaWriter.addMetaData(md);
    assertTrue(dnaWriter.isContiguous());
    dnaWriter.markSectionEnd();

    // simulate folding
    DNAWriterInternal appender = (DNAWriterInternal) dnaWriter.createAppender();
    assertFalse(dnaWriter.isContiguous());
    appender.addPhysicalAction(action4.getFieldName(), action4.getObject());
    appender.addMetaData(md2);
    appender.markSectionEnd();

    // fold in more actions without any meta data
    appender = (DNAWriterInternal) dnaWriter.createAppender();
    appender.addPhysicalAction(action5.getFieldName(), action5.getObject());
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
        default:
          fail("count got to " + count);
      }
      count++;
    }

    if (count != 6) { throw new AssertionError("not enough action seen: " + count); }

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

    // verify meta data
    count = 1;
    MetaDataReader metaReader = dna.getMetaDataReader();
    for (MetaDataDescriptorInternal meta : metaReader) {
      switch (count) {
        case 1: {
          verifyMetaData(md, meta);
          break;
        }
        case 2: {
          verifyMetaData(md2, meta);
          break;
        }
        default: {
          throw new AssertionError(count);
        }
      }
      count++;
    }
  }

  public void testMetaDataFoldNotFirstTxn() throws Exception {
    TCByteBufferOutputStream out = new TCByteBufferOutputStream();

    final ObjectID id = new ObjectID(1);
    final String type = getClass().getName();

    final ObjectStringSerializer serializer = new ObjectStringSerializerImpl();
    final ClassProvider classProvider = new MockClassProvider();
    final DNAEncodingInternal encoding = new ApplicatorDNAEncodingImpl(classProvider);
    final DNAWriterInternal dnaWriter = createDNAWriter(out, id, type, serializer, encoding, false);
    final PhysicalAction action1 = new PhysicalAction("class.field1", new Integer(1), false);

    dnaWriter.addPhysicalAction(action1.getFieldName(), action1.getObject());
    assertTrue(dnaWriter.isContiguous());
    dnaWriter.markSectionEnd();

    // simulate folding with a meta data (but without having a meta data in the first txn)
    DNAWriterInternal appender = (DNAWriterInternal) dnaWriter.createAppender();
    assertTrue(dnaWriter.isContiguous());
    appender.addPhysicalAction(action1.getFieldName(), action1.getObject());
    MetaDataDescriptorInternal md = new MetaDataDescriptorImpl("cat");
    md.setObjectID(id);
    md.add("foo", "bar");
    appender.addMetaData(md);
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
    int count = 0;
    while (cursor.next(encoding)) {
      count++;
      switch (count) {
        case 1:
          compareAction(action1, cursor.getPhysicalAction());
          break;
        case 2:
          compareAction(action1, cursor.getPhysicalAction());
          break;
        default:
          fail("count got to " + count);
      }
    }

    if (count != 2) { throw new AssertionError("not enough action seen: " + count); }

    assertEquals(id, this.dna.getObjectID());

    assertEquals(ObjectID.NULL_ID, this.dna.getParentObjectID());

    Assert.assertEquals(false, this.dna.isDelta());

    // verify meta data
    count = 0;
    MetaDataReader metaReader = dna.getMetaDataReader();
    for (MetaDataDescriptorInternal meta : metaReader) {
      count++;
      switch (count) {
        case 1: {
          verifyMetaData(md, meta);
          break;
        }
        default: {
          throw new AssertionError(count);
        }
      }
    }

    assertEquals(1, count);

  }

  private void verifyMetaData(MetaDataDescriptorInternal expect, MetaDataDescriptorInternal actual) {
    assertEquals(expect.getCategory(), actual.getCategory());
    assertEquals(expect.numberOfNvPairs(), actual.numberOfNvPairs());

    Iterator<NVPair> i1 = expect.getMetaDatas();
    Iterator<NVPair> i2 = actual.getMetaDatas();
    while (i1.hasNext()) {
      assertEquals(i1.next(), i2.next());
    }
    assertFalse(i2.hasNext());
  }

  protected DNAImpl createDNAImpl(final ObjectStringSerializer serializer) {
    return new DNAImpl(serializer, true);
  }

  protected DNAWriterInternal createDNAWriter(final TCByteBufferOutputStream out, final ObjectID id, final String type,
                                              final ObjectStringSerializer serializer,
                                              final DNAEncodingInternal encoding, final boolean isDelta) {
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

  private enum TestEnum {
    FOO;
  }

}

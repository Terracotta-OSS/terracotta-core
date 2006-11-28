/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.object.change;

import com.tc.exception.ImplementMe;
import com.tc.io.TCByteBufferInputStream;
import com.tc.io.TCByteBufferOutputStream;
import com.tc.object.ClientObjectManager;
import com.tc.object.MockTCObject;
import com.tc.object.ObjectID;
import com.tc.object.SerializationUtil;
import com.tc.object.TCClass;
import com.tc.object.TCObject;
import com.tc.object.TestTCObject;
import com.tc.object.TraversedReferences;
import com.tc.object.bytecode.MockClassProvider;
import com.tc.object.dna.api.DNA;
import com.tc.object.dna.api.DNACursor;
import com.tc.object.dna.api.DNAWriter;
import com.tc.object.dna.api.LogicalAction;
import com.tc.object.dna.api.PhysicalAction;
import com.tc.object.dna.impl.DNAEncoding;
import com.tc.object.dna.impl.DNAImpl;
import com.tc.object.dna.impl.ObjectStringSerializer;
import com.tc.object.field.TCField;
import com.tc.object.loaders.ClassProvider;
import com.tc.object.tx.optimistic.OptimisticTransactionManager;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;

public class TCChangeBufferTest extends TestCase {

  public void testAddNewObjectTo() throws Exception {
    Object myObject = new Object();
    TestTCObject tcObject = new TestTCObject(myObject);
    tcObject.setTCClass(new TestTCClass());
    List newObjects = new ArrayList(1);

    TCChangeBuffer changeBuffer = new TCChangeBufferImpl(tcObject);
    changeBuffer.addNewObjectTo(newObjects);
    assertTrue(newObjects.isEmpty());

    tcObject.setIsNew();
    changeBuffer = new TCChangeBufferImpl(tcObject);
    changeBuffer.addNewObjectTo(newObjects);
    assertEquals(1, newObjects.size());
    assertSame(myObject, newObjects.get(0));
  }

  public void testLogicalClassIgnoresPhysicalChanges() throws Exception {
    ObjectStringSerializer serializer = new ObjectStringSerializer();
    ClassProvider classProvider = new MockClassProvider();
    DNAEncoding encoding = new DNAEncoding(classProvider);
    TCChangeBuffer buffer = new TCChangeBufferImpl(new MockTCObject(new ObjectID(1), this, false, true));

    // physical updates should be ignored
    buffer.fieldChanged("classname", "fieldname", new ObjectID(12), -1);
    buffer.fieldChanged("classname", "fieldname", new Long(3), -1);

    buffer.logicalInvoke(SerializationUtil.PUT, new Object[] { new ObjectID(1), new ObjectID(2) });

    TCByteBufferOutputStream output = new TCByteBufferOutputStream();
    buffer.writeTo(output, serializer, encoding);
    output.close();

    DNAImpl dna = new DNAImpl(serializer, true);
    dna.deserializeFrom(new TCByteBufferInputStream(output.toArray()));

    int count = 0;
    DNACursor cursor = dna.getCursor();
    while (cursor.next(encoding)) {
      count++;
      LogicalAction action = dna.getLogicalAction();

      if (action.getMethod() == SerializationUtil.PUT) {
        assertEquals(new ObjectID(1), action.getParameters()[0]);
        assertEquals(new ObjectID(2), action.getParameters()[1]);
      } else {
        fail("method was " + action.getMethod());
      }
    }

    assertEquals(1, count);
  }

  public void testLastPhysicalChangeWins() throws Exception {
    ObjectStringSerializer serializer = new ObjectStringSerializer();
    ClassProvider classProvider = new MockClassProvider();
    DNAEncoding encoding = new DNAEncoding(classProvider);
    TCChangeBuffer buffer = new TCChangeBufferImpl(new MockTCObject(new ObjectID(1), this));

    for (int i = 0; i < 100; i++) {
      buffer.fieldChanged("class", "class.field", new ObjectID(i), -1);
    }

    TCByteBufferOutputStream output = new TCByteBufferOutputStream();
    buffer.writeTo(output, serializer, encoding);
    output.close();

    DNAImpl dna = new DNAImpl(serializer, true);
    dna.deserializeFrom(new TCByteBufferInputStream(output.toArray()));

    int count = 0;
    DNACursor cursor = dna.getCursor();
    while (cursor.next(encoding)) {
      count++;
      PhysicalAction action = dna.getPhysicalAction();

      if (action.isTruePhysical() && action.getFieldName().equals("class.field")) {
        assertEquals(new ObjectID(99), action.getObject());
      } else {
        fail();
      }
    }

    assertEquals(1, count);
  }

  public void testLastArrayChangeWins() throws Exception {
    ObjectStringSerializer serializer = new ObjectStringSerializer();
    ClassProvider classProvider = new MockClassProvider();
    DNAEncoding encoding = new DNAEncoding(classProvider);
    TCChangeBuffer buffer = new TCChangeBufferImpl(new MockTCObject(new ObjectID(1), this, true, false));

    for (int i = 0; i < 100; i++) {
      buffer.fieldChanged("class", "class.arrayField", new ObjectID(1000 + i), 1);
    }

    TCByteBufferOutputStream output = new TCByteBufferOutputStream();
    buffer.writeTo(output, serializer, encoding);
    output.close();

    DNAImpl dna = new DNAImpl(serializer, true);
    dna.deserializeFrom(new TCByteBufferInputStream(output.toArray()));

    int count = 0;
    DNACursor cursor = dna.getCursor();
    while (cursor.next(encoding)) {
      count++;
      PhysicalAction action = dna.getPhysicalAction();

      if (action.isArrayElement() && action.getArrayIndex() == 1) {
        assertEquals(new ObjectID(1099), action.getObject());
      } else {
        fail();
      }
    }

    assertEquals(1, count);
  }

  private static class TestTCClass implements TCClass {

    public Class getPeerClass() {
      throw new ImplementMe();
    }

   public Map connectedCopy(Object source, Object dest, Map visited, OptimisticTransactionManager txManager){
      throw new ImplementMe();
   }

    public boolean hasOnLoadExecuteScript() {
      throw new ImplementMe();
    }

    public boolean hasOnLoadMethod() {
      throw new ImplementMe();
    }

    public String getOnLoadMethod() {
      throw new ImplementMe();
    }

    public String getOnLoadExecuteScript() {
      throw new ImplementMe();
    }

    public Field getParentField() {
      throw new ImplementMe();
    }

    public String getParentFieldName() {
      throw new ImplementMe();
    }

    public TCField[] getPortableFields() {
      throw new ImplementMe();
    }

    public TraversedReferences getPortableObjects(Object pojo, TraversedReferences addTo) {
      throw new ImplementMe();
    }

    public Constructor getConstructor() {
      throw new ImplementMe();
    }

    public String getName() {
      throw new ImplementMe();
    }

    public Class getComponentType() {
      throw new ImplementMe();
    }

    public boolean isLogical() {
      return false;
    }

    public TCClass getSuperclass() {
      throw new ImplementMe();
    }

    public boolean isNonStaticInner() {
      throw new ImplementMe();
    }

    public boolean isUseNonDefaultConstructor() {
      throw new ImplementMe();
    }

    public Object getNewInstanceFromNonDefaultConstructor(DNA dna) {
      throw new ImplementMe();
    }

    public TCField getField(String name) {
      throw new ImplementMe();
    }

    public boolean isIndexed() {
      return false;
    }

    public void hydrate(TCObject tcObject, DNA dna, Object pojo, boolean force) {
      throw new ImplementMe();
    }

    public void dehydrate(TCObject tcObject, DNAWriter writer, Object pojo) {
      throw new ImplementMe();
    }

    public String getDefiningLoaderDescription() {
      throw new ImplementMe();
    }

    public TCObject createTCObject(ObjectID id, Object peer) {
      throw new ImplementMe();
    }

    public String getFieldNameByOffset(long fieldOffset) {
      throw new ImplementMe();
    }

    public ClientObjectManager getObjectManager() {
      throw new ImplementMe();
    }

    public boolean isProxyClass() {
      return false;
    }

    public String getExtendingClassName() {
      return getName();
    }

  }

}

/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object;

import com.tc.exception.ImplementMe;
import com.tc.object.bytecode.TransparentAccess;
import com.tc.object.dna.api.DNA;
import com.tc.object.dna.api.DNAWriter;
import com.tc.object.field.TCField;
import com.tc.object.field.TCFieldFactory;
import com.tc.object.tx.optimistic.OptimisticTransactionManager;
import com.tc.objectserver.core.api.TestDNAWriter;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * @author steve
 */
public class TCObjectImplTest extends BaseDSOTestCase {

  public void tests() throws Exception {
    TestClientObjectManager objectManager = new TestClientObjectManager();
    TCClass clazz = new TestTCClass(objectManager);
    TestObject to1 = new TestObject(null, null);
    TestObject to2 = new TestObject("TestObject2", null);
    ObjectID id1 = new ObjectID(1);
    ObjectID id2 = new ObjectID(2);
    objectManager.add(id2, new TCObjectPhysical(objectManager.getReferenceQueue(), id2, to2, clazz));

    TCObjectImpl tcObj = new TCObjectPhysical(objectManager.getReferenceQueue(), id1, to1, clazz);
    tcObj.resolveReference(TestObject.class.getName() + ".test1");
    tcObj.resolveReference(TestObject.class.getName() + ".test2");
    assertTrue(to1.test1 == null);// nothing should happen from that
    assertTrue(to1.test2 == null);

    tcObj.setReference(TestObject.class.getName() + ".test2", new ObjectID(2));

    assertTrue(to1.test1 == null);// nothing should happen from that
    assertTrue(to1.test2 == null);

    tcObj.resolveReference(TestObject.class.getName() + ".test2");
    assertTrue(to1.test1 == null);// nothing should happen from that
    assertTrue(to1.test2 == to2);

    tcObj.dehydrate(new TestDNAWriter());
    tcObj.clearReferences(100);
    assertTrue(to1.test2 == null);
    tcObj.resolveReference(TestObject.class.getName() + ".test2");
    assertTrue(to1.test2 == to2);
  }

  private static class TestObject implements TransparentAccess {
    public String     test1;
    public TestObject test2;

    public TCObject   managed;

    public TestObject(String test1, TestObject test2) {
      this.test1 = test1;
      this.test2 = test2;
    }

    public void __tc_getallfields(Map map) {
      map.put(getClass().getName() + "." + "test1", test1);
      map.put(getClass().getName() + "." + "test2", test2);
    }

    public void __tc_setfield(String fieldName, Object value) {
      if (fieldName.equals(TestObject.class.getName() + ".test1")) {
        test1 = (String) value;
      }
      if (fieldName.equals(TestObject.class.getName() + ".test2")) {
        test2 = (TestObject) value;
      }
    }

    public void __tc_managed(TCObject b) {
      this.managed = b;
    }

    public TCObject __tc_managed() {
      return managed;
    }

    public Object __tc_getmanagedfield(String name) {
      throw new ImplementMe();
    }

    public void __tc_setmanagedfield(String name, Object value) {
      throw new ImplementMe();
    }

  }

  public class TestTCClass implements TCClass {
    private TCFieldFactory fieldFactory;
    private Map            fields = new HashMap();
    private final TestClientObjectManager objectManager;

    public Field getParentField() {
      return null;
    }

    public String getParentFieldName() {
      return "className.this$0";
    }

    public TestTCClass(TestClientObjectManager objectManager) throws Exception {
      this.objectManager = objectManager;
      fieldFactory = new TCFieldFactory(configHelper());
      Field[] flds = TestObject.class.getDeclaredFields();
      for (int i = 0; i < flds.length; i++) {
        fields.put(TestObject.class.getName() + "." + flds[i].getName(), fieldFactory.getInstance(this, flds[i]));
      }
    }

    public TCField[] getPortableFields() {
      Collection fs = fields.values();
      return (TCField[]) fs.toArray(new TCField[fs.size()]);
    }

    public Constructor getConstructor() throws SecurityException {
      // TODO Auto-generated method stub
      return null;
    }

    public TCClass getSuperclass() {
      // TODO Auto-generated method stub
      return null;
    }

    public boolean isPortable() {
      // TODO Auto-generated method stub
      return false;
    }

    public TCField getDeclaredField(String name) {
      try {
        return (TCField) fields.get(name);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }

    public TCField getField(String name) {
      return getDeclaredField(name);
    }

    public TCField getField(String classname, String fieldname) {
      // TODO Auto-generated method stub
      return null;
    }

    public TCField getField(Field field) {
      // TODO Auto-generated method stub
      return null;
    }

    public boolean isIndexed() {
      // TODO Auto-generated method stub
      return false;
    }

    public void hydrate(TCObject tcObject, DNA dna, Object pojo, boolean force) {
      //
    }

    public void dehydrate(TCObject tcObject, DNAWriter writer, Object pojo) {
      //
    }

    public String getName() {
      return TestObject.class.getName();
    }

    public Class getComponentType() {
      return null;
    }

    public TraversedReferences getPortableObjects(Object pojo, TraversedReferences addTo) {
      return addTo;
    }

    public String getDefiningLoaderDescription() {
      throw new ImplementMe();
    }

    public boolean isNonStaticInner() {
      return false;
    }

    public boolean isLogical() {
      return false;
    }

    public TCObject createTCObject(ObjectID id, Object peer) {
      throw new ImplementMe();
    }

    public boolean hasOnLoadExecuteScript() {
      return false;
    }

    public String getOnLoadExecuteScript() {

      return null;
    }

    public boolean hasOnLoadMethod() {
      return false;
    }

    public String getOnLoadMethod() {
      return null;
    }

    public boolean isUseNonDefaultConstructor() {
      return false;
    }

    public Object getNewInstanceFromNonDefaultConstructor(DNA dna) {
      return null;
    }

    public Class getPeerClass() {
      throw new ImplementMe();
    }

    public Map connectedCopy(Object source, Object dest, Map visited, OptimisticTransactionManager txManager) {
      throw new ImplementMe();
    }

    public String getFieldNameByOffset(long fieldOffset) {
      throw new ImplementMe();
    }

    public ClientObjectManager getObjectManager() {
      return objectManager;
    }

    public boolean isProxyClass() {
      return false;
    }

    public String getExtendingClassName() {
      return getName();
    }
  }
}

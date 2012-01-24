/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object;

import com.tc.exception.ImplementMe;
import com.tc.object.bytecode.TransparentAccess;
import com.tc.object.dna.api.DNA;
import com.tc.object.dna.api.DNAWriter;
import com.tc.object.field.TCField;
import com.tc.object.field.TCFieldFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author steve
 */
public class TCObjectImplTest extends BaseDSOTestCase {

  public void tests() throws Exception {
    final TestClientObjectManager objectManager = new TestClientObjectManager();
    final TCClass clazz = new TestTCClass(objectManager);
    final TestObject to1 = new TestObject(null, null);
    final TestObject to2 = new TestObject("TestObject2", null);
    final ObjectID id1 = new ObjectID(1);
    final ObjectID id2 = new ObjectID(2);
    objectManager.add(id2, new TCObjectPhysical(id2, to2, clazz, false));

    final TCObjectImpl tcObj = new TCObjectPhysical(id1, to1, clazz, false);
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

    tcObj.getTCClass().dehydrate(tcObj, new TestDNAWriter(), tcObj.getPeerObject());
    tcObj.clearReferences(100);
    assertTrue(to1.test2 == null);
    tcObj.resolveReference(TestObject.class.getName() + ".test2");
    assertTrue(to1.test2 == to2);
  }

  private static class TestObject implements TransparentAccess {
    public String     test1;
    public TestObject test2;

    public TestObject(final String test1, final TestObject test2) {
      this.test1 = test1;
      this.test2 = test2;
    }

    public void __tc_getallfields(final Map map) {
      map.put(getClass().getName() + "." + "test1", this.test1);
      map.put(getClass().getName() + "." + "test2", this.test2);
    }

    public void __tc_setfield(final String fieldName, final Object value) {
      if (fieldName.equals(TestObject.class.getName() + ".test1")) {
        this.test1 = (String) value;
      }
      if (fieldName.equals(TestObject.class.getName() + ".test2")) {
        this.test2 = (TestObject) value;
      }
    }

    public Object __tc_getmanagedfield(final String name) {
      throw new ImplementMe();
    }

    public void __tc_setmanagedfield(final String name, final Object value) {
      throw new ImplementMe();
    }

  }

  public class TestTCClass implements TCClass {
    private final TCFieldFactory          fieldFactory;
    private final Map                     fields = new HashMap();
    private final TestClientObjectManager objectManager;

    public Field getParentField() {
      return null;
    }

    public String getParentFieldName() {
      return "className.this$0";
    }

    public TestTCClass(final TestClientObjectManager objectManager) throws Exception {
      this.objectManager = objectManager;
      this.fieldFactory = new TCFieldFactory(configHelper());
      final Field[] flds = TestObject.class.getDeclaredFields();
      for (final Field fld : flds) {
        this.fields.put(TestObject.class.getName() + "." + fld.getName(), this.fieldFactory.getInstance(this, fld));
      }
    }

    public TCField[] getPortableFields() {
      final Collection fs = this.fields.values();
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

    public TCField getDeclaredField(final String name) {
      try {
        return (TCField) this.fields.get(name);
      } catch (final Exception e) {
        throw new RuntimeException(e);
      }
    }

    public TCField getField(final String name) {
      return getDeclaredField(name);
    }

    public TCField getField(final String classname, final String fieldname) {
      // TODO Auto-generated method stub
      return null;
    }

    public TCField getField(final Field field) {
      // TODO Auto-generated method stub
      return null;
    }

    public boolean isIndexed() {
      // TODO Auto-generated method stub
      return false;
    }

    public void hydrate(final TCObject tcObject, final DNA dna, final Object pojo, final boolean force) {
      //
    }

    public void dehydrate(final TCObject tcObject, final DNAWriter writer, final Object pojo) {
      //
    }

    public String getName() {
      return TestObject.class.getName();
    }

    public Class getComponentType() {
      return null;
    }

    public TraversedReferences getPortableObjects(final Object pojo, final TraversedReferences addTo) {
      return addTo;
    }

    public boolean isNonStaticInner() {
      return false;
    }

    public boolean isLogical() {
      return false;
    }

    public TCObject createTCObject(final ObjectID id, final Object peer, final boolean isNew) {
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

    public Object getNewInstanceFromNonDefaultConstructor(final DNA dna) {
      return null;
    }

    public Class getPeerClass() {
      throw new ImplementMe();
    }

    public String getFieldNameByOffset(final long fieldOffset) {
      throw new ImplementMe();
    }

    public ClientObjectManager getObjectManager() {
      return this.objectManager;
    }

    public boolean isProxyClass() {
      return false;
    }

    public String getExtendingClassName() {
      return getName();
    }

    public boolean isEnum() {
      return false;
    }

    public boolean isPortableField(final long fieldOffset) {
      throw new ImplementMe();
    }

    public boolean useResolveLockWhileClearing() {
      return true;
    }

    public boolean hasOnLoadInjection() {
      return false;
    }

    public boolean isNotClearable() {
      return false;
    }

    public List<Method> getPostCreateMethods() {
      throw new ImplementMe();
    }

    public List<Method> getPreCreateMethods() {
      throw new ImplementMe();
    }
  }
}

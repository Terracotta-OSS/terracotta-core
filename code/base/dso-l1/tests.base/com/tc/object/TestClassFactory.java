/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object;

import com.tc.exception.ImplementMe;
import com.tc.object.applicator.ChangeApplicator;
import com.tc.object.dna.api.DNA;
import com.tc.object.dna.api.DNAWriter;
import com.tc.object.field.TCField;
import com.tc.object.tx.optimistic.OptimisticTransactionManager;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.Map;

public class TestClassFactory implements TCClassFactory {

  public TCClass getOrCreate(Class clazz, ClientObjectManager objectManager) {
    return new MockTCClass();
  }

  public ChangeApplicator createApplicatorFor(TCClass clazz, boolean indexed) {
    throw new ImplementMe();
  }

  public static class MockTCClass implements TCClass {

    private ClientObjectManager clientObjectManager;

    private boolean             hasOnLoadExecuteScript = false;

    private boolean             hasOnLoadMethod        = false;

    private TCField[]           portableFields         = null;

    private boolean             portable               = false;

    public MockTCClass() {
      //
    }

    public MockTCClass(ClientObjectManager clientObjectManager, boolean hasOnLoadExecuteScript,
                       boolean hasOnLoadMethod, boolean portable, TCField[] portableFields) {
      this.clientObjectManager = clientObjectManager;
      this.hasOnLoadExecuteScript = hasOnLoadExecuteScript;
      this.hasOnLoadMethod = hasOnLoadMethod;
      this.portable = portable;
      this.portableFields = portableFields;
    }

    public boolean hasOnLoadExecuteScript() {
      return hasOnLoadExecuteScript;
    }

    public boolean hasOnLoadMethod() {
      return hasOnLoadMethod;
    }

    public String getOnLoadMethod() {
      return null;
    }

    public String getOnLoadExecuteScript() {
      return "";
    }

    public Field getParentField() {
      return null;
    }

    public String getParentFieldName() {
      return null;
    }

    public TCField[] getPortableFields() {
      return portableFields;
    }

    public TraversedReferences getPortableObjects(Object pojo, TraversedReferences addTo) {
      return addTo;
    }

    public Constructor getConstructor() {
      return null;
    }

    public String getName() {
      return null;
    }

    public Class getComponentType() {
      return null;
    }

    public boolean isLogical() {
      return false;
    }

    public TCClass getSuperclass() {
      return null;
    }

    public boolean isNonStaticInner() {
      return false;
    }

    public boolean isPortable() {
      return portable;
    }

    public TCField getField(String name) {
      for (int i = 0; i < portableFields.length; i++) {
        TCField field = portableFields[i];
        if (name.equals(field.getName())) return field;
      }
      return null;
    }

    public boolean isIndexed() {
      return false;
    }

    public void hydrate(TCObject tcObject, DNA dna, Object pojo, boolean force) {
      //
    }

    public void dehydrate(TCObject tcObject, DNAWriter writer, Object pojo) {
      //
    }

    public String getDefiningLoaderDescription() {
      return null;
    }

    public TCObject createTCObject(ObjectID id, Object peer, boolean isNew) {
      return null;
    }

    public boolean isUseNonDefaultConstructor() {
      return false;
    }

    public Object getNewInstanceFromNonDefaultConstructor(DNA dna) {
      return null;
    }

    public Class getPeerClass() {
      return Object.class;
    }

    public Map connectedCopy(Object source, Object dest, Map visited, OptimisticTransactionManager txManager) {
      throw new ImplementMe();
    }

    public String getFieldNameByOffset(long fieldOffset) {
      throw new ImplementMe();
    }

    public ClientObjectManager getObjectManager() {
      return clientObjectManager;
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

    public boolean isPortableField(long fieldOffset) {
      throw new ImplementMe();
    }

    public boolean useResolveLockWhileClearing() {
      return true;
    }
  }

  public static class MockTCField implements TCField {

    private final String name;

    public MockTCField(String name) {
      this.name = name;
    }

    public boolean canBeReference() {
      return true;
    }

    public TCClass getDeclaringTCClass() {
      throw new ImplementMe();
    }

    public String getName() {
      return name;
    }

    public boolean isArray() {
      throw new ImplementMe();
    }

    public boolean isFinal() {
      throw new ImplementMe();
    }

    public boolean isPortable() {
      return true;
    }

  }
}

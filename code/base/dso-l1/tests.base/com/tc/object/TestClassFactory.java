/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
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

    public boolean hasOnLoadExecuteScript() {
      return false;
    }

    public boolean hasOnLoadMethod() {
      return false;
    }

    public String getOnLoadMethod() {
      return null;
    }

    public String getOnLoadExecuteScript() {
      return null;
    }
    
    public Field getParentField() {
      return null;
    }

    public String getParentFieldName() {
      return null;
    }

    public TCField[] getPortableFields() {
      return null;
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
      return false;
    }

    public TCField getField(String name) {
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

    public TCObject createTCObject(ObjectID id, Object peer) {
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

    public Map connectedCopy(Object source, Object dest, Map visited,  OptimisticTransactionManager txManager) {
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

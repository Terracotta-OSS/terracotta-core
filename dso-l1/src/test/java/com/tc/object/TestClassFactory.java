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

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;

public class TestClassFactory implements TCClassFactory {

  @Override
  public TCClass getOrCreate(final Class clazz, final ClientObjectManager objectManager) {
    return new MockTCClass();
  }

  @Override
  public ChangeApplicator createApplicatorFor(final TCClass clazz, final boolean indexed) {
    throw new ImplementMe();
  }

  public static class MockTCClass implements TCClass {

    private ClientObjectManager clientObjectManager;

    private boolean             hasOnLoadExecuteScript = false;

    private boolean             hasOnLoadMethod        = false;

    private TCField[]           portableFields         = null;

    public MockTCClass() {
      //
    }

    public MockTCClass(final ClientObjectManager clientObjectManager, final boolean hasOnLoadExecuteScript,
                       final boolean hasOnLoadMethod, final TCField[] portableFields) {
      this.clientObjectManager = clientObjectManager;
      this.hasOnLoadExecuteScript = hasOnLoadExecuteScript;
      this.hasOnLoadMethod = hasOnLoadMethod;
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

    @Override
    public Field getParentField() {
      return null;
    }

    @Override
    public String getParentFieldName() {
      return null;
    }

    @Override
    public TCField[] getPortableFields() {
      return portableFields;
    }

    @Override
    public TraversedReferences getPortableObjects(final Object pojo, final TraversedReferences addTo) {
      return addTo;
    }

    @Override
    public Constructor getConstructor() {
      return null;
    }

    @Override
    public String getName() {
      return null;
    }

    @Override
    public Class getComponentType() {
      return null;
    }

    @Override
    public boolean isLogical() {
      return false;
    }

    @Override
    public TCClass getSuperclass() {
      return null;
    }

    @Override
    public boolean isNonStaticInner() {
      return false;
    }

    @Override
    public TCField getField(final String name) {
      for (TCField field : portableFields) {
        if (name.equals(field.getName())) return field;
      }
      return null;
    }

    @Override
    public boolean isIndexed() {
      return false;
    }

    @Override
    public void hydrate(final TCObject tcObject, final DNA dna, final Object pojo, final boolean force) {
      //
    }

    @Override
    public void dehydrate(final TCObject tcObject, final DNAWriter writer, final Object pojo) {
      //
    }

    @Override
    public TCObject createTCObject(final ObjectID id, final Object peer, final boolean isNew) {
      return null;
    }

    @Override
    public boolean isUseNonDefaultConstructor() {
      return false;
    }

    @Override
    public Object getNewInstanceFromNonDefaultConstructor(final DNA dna) {
      return null;
    }

    @Override
    public Class getPeerClass() {
      return Object.class;
    }

    @Override
    public ClientObjectManager getObjectManager() {
      return clientObjectManager;
    }

    @Override
    public boolean isProxyClass() {
      return false;
    }

    @Override
    public String getExtendingClassName() {
      return getName();
    }

    @Override
    public boolean isEnum() {
      return false;
    }

    @Override
    public boolean isPortableField(final long fieldOffset) {
      throw new ImplementMe();
    }

    @Override
    public boolean useResolveLockWhileClearing() {
      return true;
    }

    public boolean hasOnLoadInjection() {
      return false;
    }

    @Override
    public List<Method> getPostCreateMethods() {
      return Collections.EMPTY_LIST;
    }

    @Override
    public List<Method> getPreCreateMethods() {
      return Collections.EMPTY_LIST;
    }
  }

  public static class MockTCField implements TCField {

    private final String name;

    public MockTCField(final String name) {
      this.name = name;
    }

    @Override
    public boolean canBeReference() {
      return true;
    }

    @Override
    public TCClass getDeclaringTCClass() {
      throw new ImplementMe();
    }

    @Override
    public String getName() {
      return name;
    }

    @Override
    public boolean isArray() {
      throw new ImplementMe();
    }

    @Override
    public boolean isPortable() {
      return true;
    }

  }
}

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
import com.tc.object.loaders.LoaderDescription;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;

public class TestClassFactory implements TCClassFactory {

  public TCClass getOrCreate(final Class clazz, final ClientObjectManager objectManager) {
    return new MockTCClass();
  }

  public ChangeApplicator createApplicatorFor(final TCClass clazz, final boolean indexed) {
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

    public MockTCClass(final ClientObjectManager clientObjectManager, final boolean hasOnLoadExecuteScript,
                       final boolean hasOnLoadMethod, final boolean portable, final TCField[] portableFields) {
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

    public TraversedReferences getPortableObjects(final Object pojo, final TraversedReferences addTo) {
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

    public TCField getField(final String name) {
      for (TCField field : portableFields) {
        if (name.equals(field.getName())) return field;
      }
      return null;
    }

    public boolean isIndexed() {
      return false;
    }

    public void hydrate(final TCObject tcObject, final DNA dna, final Object pojo, final boolean force) {
      //
    }

    public void dehydrate(final TCObject tcObject, final DNAWriter writer, final Object pojo) {
      //
    }

    public LoaderDescription getDefiningLoaderDescription() {
      return new LoaderDescription(null, "mock");
    }

    public TCObject createTCObject(final ObjectID id, final Object peer, final boolean isNew) {
      return null;
    }

    public boolean isUseNonDefaultConstructor() {
      return false;
    }

    public Object getNewInstanceFromNonDefaultConstructor(final DNA dna) {
      return null;
    }

    public Class getPeerClass() {
      return Object.class;
    }

    public String getFieldNameByOffset(final long fieldOffset) {
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
      return Collections.EMPTY_LIST;
    }

    public List<Method> getPreCreateMethods() {
      return Collections.EMPTY_LIST;
    }
  }

  public static class MockTCField implements TCField {

    private final String name;

    public MockTCField(final String name) {
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

    public boolean isPortable() {
      return true;
    }

  }
}

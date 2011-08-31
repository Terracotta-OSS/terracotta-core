/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object;

import com.tc.exception.ImplementMe;
import com.tc.object.dna.api.DNA;
import com.tc.object.dna.api.DNAWriter;
import com.tc.object.field.TCField;
import com.tc.object.loaders.LoaderDescription;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

public class MockTCClass implements TCClass {
  private final String  name = MockTCClass.class.getName();
  private final boolean isIndexed;
  private final boolean isLogical;

  public MockTCClass(final boolean isIndexed, final boolean isLogical) {
    this.isIndexed = isIndexed;
    this.isLogical = isLogical;
  }

  public TCField[] getPortableFields() {
    throw new ImplementMe();
  }

  public TraversedReferences getPortableObjects(final Object pojo, final TraversedReferences addTo) {
    throw new ImplementMe();
  }

  public Constructor getConstructor() {
    throw new ImplementMe();
  }

  public String getName() {
    return this.name;
  }

  public Class getComponentType() {
    throw new ImplementMe();
  }

  public TCClass getSuperclass() {
    throw new ImplementMe();
  }

  public boolean isLogical() {
    return this.isLogical;
  }

  public TCField getField(final String fieldName) {
    throw new ImplementMe();
  }

  public boolean isIndexed() {
    return this.isIndexed;
  }

  public void hydrate(final TCObject tcObject, final DNA dna, final Object pojo, final boolean force) {
    throw new ImplementMe();
  }

  public void dehydrate(final TCObject tcObject, final DNAWriter writer, final Object pojo) {
    throw new ImplementMe();
  }

  public Field getParentField() {
    throw new ImplementMe();
  }

  public String getParentFieldName() {
    return name + ".this$0";
  }

  public LoaderDescription getDefiningLoaderDescription() {
    return new LoaderDescription(null, "mock");
  }

  public boolean isNonStaticInner() {
    throw new ImplementMe();
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
    throw new ImplementMe();
  }

  public Class getPeerClass() {
    return getClass();
  }

  public String getFieldNameByOffset(final long fieldOffset) {
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

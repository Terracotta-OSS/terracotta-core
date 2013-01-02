/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object;

import com.tc.exception.ImplementMe;
import com.tc.object.dna.api.DNA;
import com.tc.object.dna.api.DNAWriter;
import com.tc.object.field.TCField;

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

  @Override
  public TCField[] getPortableFields() {
    throw new ImplementMe();
  }

  @Override
  public TraversedReferences getPortableObjects(final Object pojo, final TraversedReferences addTo) {
    throw new ImplementMe();
  }

  @Override
  public Constructor getConstructor() {
    throw new ImplementMe();
  }

  @Override
  public String getName() {
    return this.name;
  }

  @Override
  public Class getComponentType() {
    throw new ImplementMe();
  }

  @Override
  public TCClass getSuperclass() {
    throw new ImplementMe();
  }

  @Override
  public boolean isLogical() {
    return this.isLogical;
  }

  @Override
  public TCField getField(final String fieldName) {
    throw new ImplementMe();
  }

  @Override
  public boolean isIndexed() {
    return this.isIndexed;
  }

  @Override
  public void hydrate(final TCObject tcObject, final DNA dna, final Object pojo, final boolean force) {
    throw new ImplementMe();
  }

  @Override
  public void dehydrate(final TCObject tcObject, final DNAWriter writer, final Object pojo) {
    throw new ImplementMe();
  }

  @Override
  public Field getParentField() {
    throw new ImplementMe();
  }

  @Override
  public String getParentFieldName() {
    return name + ".this$0";
  }

  @Override
  public boolean isNonStaticInner() {
    throw new ImplementMe();
  }

  @Override
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

  @Override
  public boolean isUseNonDefaultConstructor() {
    return false;
  }

  @Override
  public Object getNewInstanceFromNonDefaultConstructor(final DNA dna) {
    throw new ImplementMe();
  }

  @Override
  public Class getPeerClass() {
    return getClass();
  }

  @Override
  public ClientObjectManager getObjectManager() {
    throw new ImplementMe();
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
  public boolean isNotClearable() {
    return false;
  }

  @Override
  public List<Method> getPostCreateMethods() {
    throw new ImplementMe();
  }

  @Override
  public List<Method> getPreCreateMethods() {
    throw new ImplementMe();
  }

}

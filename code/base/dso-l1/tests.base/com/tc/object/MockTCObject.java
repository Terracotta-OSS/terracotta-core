/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object;

import com.tc.exception.ImplementMe;
import com.tc.object.dna.api.DNA;
import com.tc.object.dna.api.DNAWriter;
import com.tc.object.util.ToggleableStrongReference;

import gnu.trove.TLinkable;

import java.util.LinkedList;
import java.util.List;

/**
 * Mock implementation of TCObject for testing.
 */
public class MockTCObject implements TCObject {
  private final ObjectID id;
  private final Object   peer;
  private final List     history          = new LinkedList();
  private final Object   resolveLock      = new Object();
  private long           version          = 0;
  private final TCClass  tcClazz;
  private boolean        accessed         = false;
  private boolean        isNew            = false;
  private Exception      hydrateException = null;

  public MockTCObject(final ObjectID id, final Object obj) {
    this(id, obj, false, false);
  }

  public MockTCObject(final ObjectID id, final Object obj, final boolean isIndexed, final boolean isLogical) {
    this.peer = obj;
    this.id = id;
    this.tcClazz = new MockTCClass(isIndexed, isLogical);
  }

  public List getHistory() {
    return this.history;
  }

  public ObjectID getObjectID() {
    return this.id;
  }

  public Object getPeerObject() {
    return this.peer;
  }

  public TCClass getTCClass() {
    return this.tcClazz;
  }

  public void booleanFieldChanged(final String classname, final String fieldname, final boolean newValue,
                                  final int index) {
    if ("java.lang.reflect.AccessibleObject.override".equals(fieldname)) {
      // do nothing since the support for AccessibleObject looks up the
      // TCObject instance in the currently active ClientObjectManager, which causes
      // and exception to be thrown during the tests since their accessible status is
      // always set to 'true' before execution
    } else {
      throw new ImplementMe();
    }
  }

  public void byteFieldChanged(final String classname, final String fieldname, final byte newValue, final int index) {
    throw new ImplementMe();
  }

  public void charFieldChanged(final String classname, final String fieldname, final char newValue, final int index) {
    throw new ImplementMe();
  }

  public void doubleFieldChanged(final String classname, final String fieldname, final double newValue, final int index) {
    throw new ImplementMe();
  }

  public void floatFieldChanged(final String classname, final String fieldname, final float newValue, final int index) {
    throw new ImplementMe();
  }

  public void intFieldChanged(final String classname, final String fieldname, final int newValue, final int index) {
    throw new ImplementMe();
  }

  public void longFieldChanged(final String classname, final String fieldname, final long newValue, final int index) {
    throw new ImplementMe();
  }

  public void shortFieldChanged(final String classname, final String fieldname, final short newValue, final int index) {
    throw new ImplementMe();
  }

  public void setHydrateException(final Exception hydrateException) {
    this.hydrateException = hydrateException;
  }

  public void hydrate(final DNA from, final boolean force) throws ClassNotFoundException {
    if (this.hydrateException != null) {
      if (this.hydrateException instanceof RuntimeException) { throw (RuntimeException) this.hydrateException; }
      throw (ClassNotFoundException) this.hydrateException;
    }
    // nothing
  }

  public void resolveReference(final String fieldName) {
    throw new ImplementMe();
  }

  public void resolveArrayReference(final int index) {
    return;
  }

  public void objectFieldChanged(final String classname, final String fieldname, final Object newValue, final int index) {
    return;
  }

  public static class MethodCall {
    public int      method;
    public Object[] parameters;

    public MethodCall(final int method, final Object[] parameters) {
      this.method = method;
      this.parameters = parameters;
    }

    @Override
    public String toString() {
      final StringBuffer sb = new StringBuffer();
      sb.append(this.method);
      sb.append("(");
      for (int i = 0; i < this.parameters.length; i++) {
        sb.append(this.parameters[i].toString());
        if (i + 1 < this.parameters.length) {
          sb.append(',');
        }
      }
      sb.append(")");
      return sb.toString();
    }
  }

  public ObjectID setReference(final String fieldName, final ObjectID id) {
    throw new ImplementMe();
  }

  public void setArrayReference(final int index, final ObjectID id) {
    throw new ImplementMe();
  }

  public void setValue(final String fieldName, final Object obj) {
    throw new ImplementMe();
  }

  public long getVersion() {
    return this.version;
  }

  public void setVersion(final long version) {
    this.version = version;
  }

  public int clearReferences(final int toClear) {
    return 0;
  }

  public Object getResolveLock() {
    return this.resolveLock;
  }

  public void setNext(final TLinkable link) {
    throw new ImplementMe();
  }

  public void setPrevious(final TLinkable link) {
    throw new ImplementMe();
  }

  public TLinkable getNext() {
    return null;
  }

  public TLinkable getPrevious() {
    return null;
  }

  public void markAccessed() {
    this.accessed = true;
  }

  public void clearAccessed() {
    this.accessed = false;
  }

  public boolean recentlyAccessed() {
    return this.accessed;
  }

  public int accessCount(final int factor) {
    throw new ImplementMe();
  }

  public void clearReference(final String fieldName) {
    throw new ImplementMe();
  }

  public void resolveAllReferences() {
    //
  }

  public boolean isNew() {
    return this.isNew;
  }

  public void setNew(final boolean isNew) {
    this.isNew = isNew;
  }

  public boolean isShared() {
    return true;
  }

  public void objectFieldChangedByOffset(final String classname, final long fieldOffset, final Object newValue,
                                         final int index) {
    return;
  }

  public void logicalInvoke(final int method, final String methodSignature, final Object[] params) {
    this.history.add(new MethodCall(method, params));
  }

  public String getFieldNameByOffset(final long fieldOffset) {
    throw new ImplementMe();
  }

  public void disableAutoLocking() {
    throw new ImplementMe();
  }

  public boolean autoLockingDisabled() {
    return false;
  }

  public boolean canEvict() {
    throw new ImplementMe();
  }

  public void objectArrayChanged(final int startPos, final Object[] array, final int length) {
    throw new ImplementMe();
  }

  public void primitiveArrayChanged(final int startPos, final Object array, final int length) {
    throw new ImplementMe();
  }

  public void literalValueChanged(final Object newValue, final Object oldValue) {
    throw new ImplementMe();
  }

  public void setLiteralValue(final Object newValue) {
    throw new ImplementMe();
  }

  public boolean isFieldPortableByOffset(final long fieldOffset) {
    throw new ImplementMe();
  }

  public ToggleableStrongReference getOrCreateToggleRef() {
    throw new ImplementMe();
  }

  public void setNotNew() {
    this.isNew = false;
  }

  public void dehydrate(final DNAWriter writer) {
    //
  }

  public void unresolveReference(final String fieldName) {
    throw new ImplementMe();
  }

  public boolean isCacheManaged() {
    return true;
  }
}

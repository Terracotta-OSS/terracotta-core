/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object;

import com.tc.exception.TCClassNotFoundException;
import com.tc.object.bytecode.TransparentAccess;
import com.tc.object.field.TCField;
import com.tc.util.Assert;
import com.tc.util.ClassUtils;

import gnu.trove.THashMap;

import java.lang.ref.ReferenceQueue;
import java.util.HashMap;
import java.util.Map;

public class TCObjectPhysical extends TCObjectImpl {
  private Map references = null;

  public TCObjectPhysical(ReferenceQueue queue, ObjectID id, Object peer, TCClass tcc) {
    super(queue, id, peer, tcc);
  }

  private Map getReferences() {
    synchronized (getResolveLock()) {
      if (references == null) {
        references = new THashMap(0);
      }
      return references;
    }
  }

  private boolean hasReferences() {
    return references != null && !references.isEmpty();
  }

  private ObjectID removeReference(String fieldName) {
    synchronized (getResolveLock()) {
      ObjectID rv = (ObjectID) references.remove(fieldName);
      if (references.isEmpty()) {
        references = null;
      }
      return rv;
    }
  }

  public void resolveAllReferences() {
    TCClass tcc = getTCClass();

    while (tcc != null) {
      TCField[] fields = tcc.getPortableFields();
      for (int i = 0; i < fields.length; i++) {
        if (fields[i].canBeReference()) resolveReference(fields[i].getName());
      }
      tcc = tcc.getSuperclass();
    }
  }

  public ArrayIndexOutOfBoundsException checkArrayIndex(int index) {
    Object[] po = (Object[]) getPeerObject();
    if (index >= po.length || index < 0) {
      //
      return new ArrayIndexOutOfBoundsException(index);
    }
    return null;
  }

  public final void resolveArrayReference(int index) {
    this.markAccessed();

    Object[] po = (Object[]) getPeerObject();
    if (!hasReferences()) return;

    ObjectID id = removeReference(Integer.toString(index));

    if (id == null) return;
    if (id.isNull()) {
      po[index] = null;
    } else {
      Object o;
      try {
        o = getObjectManager().lookupObject(id);
      } catch (ClassNotFoundException e) {
        throw new TCClassNotFoundException(e);
      }
      po[index] = o;
    }
  }

  public void setReference(String fieldName, ObjectID id) {
    synchronized (getResolveLock()) {
      getReferences().put(fieldName, id);
    }
  }

  public void setArrayReference(int index, ObjectID id) {
    synchronized (getResolveLock()) {
      Object[] po = (Object[]) getPeerObject();
      if (po == null) return;
      po[index] = null;
      setReference(String.valueOf(index), id);
    }
  }

  public void clearReference(String fieldName) {
    synchronized (getResolveLock()) {
      if (hasReferences()) {
        removeReference(fieldName);
      }
    }
  }

  public final void resolveReference(String fieldName) {
    synchronized (getResolveLock()) {
      this.markAccessed();
      if (!hasReferences()) return;

      Object po = getPeerObject();
      TCClass tcClass = getTCClass();
      Assert.eval(tcClass != null);
      TCField field = tcClass.getField(fieldName);
      if (!field.canBeReference()) { return; }

      ObjectID id = (ObjectID) getReferences().get(fieldName);
      // Already resolved
      if (id == null) { return; }

      Object setObject = null;
      if (id != null && !id.isNull()) {
        try {
          setObject = getObjectManager().lookupObject(id);
        } catch (ClassNotFoundException e) {
          throw new TCClassNotFoundException(e);
        }
      }
      removeReference(fieldName);
      ((TransparentAccess) po).__tc_setfield(field.getName(), setObject);
    }
  }

  public void logicalInvoke(int method, String methodSignature, Object[] params) {
    throw new UnsupportedOperationException();
  }

  public void literalValueChanged(Object newValue, Object oldValue) {
    getObjectManager().getTransactionManager().literalValueChanged(this, newValue, oldValue);
    setPeerObject(new WeakObjectReference(getObjectID(), newValue, getObjectManager().getReferenceQueue()));
  }

  /**
   * Unlike literalValueChange, this method is not synchronized on getResolveLock() because this method is called by the
   * applicator thread which has been synchronized on getResolveLock() in TCObjectImpl.hydrate().
   */
  public void setLiteralValue(Object newValue) {
    setPeerObject(new WeakObjectReference(getObjectID(), newValue, getObjectManager().getReferenceQueue()));
  }

  protected boolean isEvictable() {
    return true;
  }

  protected int clearReferences(Object pojo, int toClear) {
    if (tcClazz.isIndexed()) {
      if (ClassUtils.isPrimitiveArray(pojo)) return 0;
      return clearArrayReferences((Object[]) pojo);
    } else if (pojo instanceof TransparentAccess) {
      return clearObjectReferences((TransparentAccess) pojo);
    } else {
      return 0;
    }
  }

  private int clearArrayReferences(Object[] array) {
    int cleared = 0;
    int l = array.length;
    for (int i = 0; i < l; i++) {
      Object o = array[i];
      if (o == null) continue;
      if (getObjectManager().isManaged(o)) {
        ObjectID lid = getObjectManager().lookupExistingObjectID(o);
        setReference(Integer.toString(i), lid);
        array[i] = null;
        cleared++;
      }
    }
    return cleared;
  }

  private int clearObjectReferences(TransparentAccess ta) {
    TCField[] fields = tcClazz.getPortableFields();
    if (fields.length == 0) { return 0; }
    Map fieldValues = null;
    int cleared = 0;
    for (int i = 0; i < fields.length; i++) {
      TCField field = fields[i];
      if (field.isFinal() || !field.canBeReference()) continue;
      if (fieldValues == null) {
        // lazy instantiation. TODO:: Add a new method in TransparentAccess __tc_getFieldNoResolve()
        fieldValues = new HashMap();
        ta.__tc_getallfields(fieldValues);
      }
      Object obj = fieldValues.get(field.getName());
      if (obj == null) continue;
      TCObject tobj = getObjectManager().lookupExistingOrNull(obj);
      if (tobj != null) {
        ObjectID lid = tobj.getObjectID();
        setValue(field.getName(), lid);
        cleared++;
      }
    }
    return cleared;
  }
}

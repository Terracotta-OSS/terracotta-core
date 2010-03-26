/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object;

import com.tc.exception.TCClassNotFoundException;
import com.tc.object.bytecode.TransparentAccess;
import com.tc.object.field.TCField;
import com.tc.util.Assert;
import com.tc.util.ClassUtils;

import gnu.trove.THashMap;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

public class TCObjectPhysical extends TCObjectImpl {
  private Map references = null;

  public TCObjectPhysical(ObjectID id, Object peer, TCClass tcc, boolean isNew) {
    super(id, peer, tcc, isNew);
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

  @Override
  public void resolveAllReferences() {
    TCClass tcc = getTCClass();

    if (tcc.isIndexed()) {
      if (!hasReferences()) return;
      Object[] po = (Object[]) getPeerObject();

      for (Iterator iter = references.entrySet().iterator(); iter.hasNext();) {
        Map.Entry entry = (Entry) iter.next();

        int index = Integer.parseInt((String) entry.getKey());
        ObjectID id = (ObjectID) entry.getValue();
        setArrayReference(po, index, id);

        iter.remove();
      }
    } else {
      while (tcc != null) {
        TCField[] fields = tcc.getPortableFields();
        for (TCField field : fields) {
          if (field.canBeReference()) resolveReference(field.getName());
        }
        tcc = tcc.getSuperclass();
      }
    }
  }

  @Override
  public final void resolveArrayReference(int index) {
    this.markAccessed();

    if (!hasReferences()) return;

    ObjectID id = removeReference(Integer.toString(index));
    if (id == null) return;

    setArrayReference((Object[]) getPeerObject(), index, id);
  }

  private void setArrayReference(Object[] po, int index, ObjectID id) {
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

  @Override
  public ObjectID setReference(String fieldName, ObjectID id) {
    synchronized (getResolveLock()) {
      return (ObjectID) getReferences().put(fieldName, id);
    }
  }

  @Override
  public void setArrayReference(int index, ObjectID id) {
    synchronized (getResolveLock()) {
      Object[] po = (Object[]) getPeerObject();
      if (po == null) return;
      po[index] = null;
      setReference(String.valueOf(index), id);
    }
  }

  @Override
  public void clearReference(String fieldName) {
    synchronized (getResolveLock()) {
      if (hasReferences()) {
        removeReference(fieldName);
      }
    }
  }

  @Override
  public final void resolveReference(String fieldName) {
    synchronized (getResolveLock()) {
      this.markAccessed();
      if (!hasReferences()) return;

      Object po = getPeerObject();
      TCClass tcClass = getTCClass();
      Assert.eval(tcClass != null);
      TCField field = tcClass.getField(fieldName);
      if (!field.canBeReference()) { return; }

      final ObjectID id = (ObjectID) getReferences().get(fieldName);
      // Already resolved
      if (id == null) { return; }

      Object setObject = null;
      if (!id.isNull()) {
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

  @Override
  public void literalValueChanged(Object newValue, Object oldValue) {
    getObjectManager().getTransactionManager().literalValueChanged(this, newValue, oldValue);
    setPeerObject(newValue == null ? null : getObjectManager().newWeakObjectReference(getObjectID(), newValue));
  }

  /**
   * Unlike literalValueChange, this method is not synchronized on getResolveLock() because this method is called by the
   * applicator thread which has been synchronized on getResolveLock() in TCObjectImpl.hydrate().
   */
  @Override
  public void setLiteralValue(Object newValue) {
    setPeerObject(newValue == null ? null : getObjectManager().newWeakObjectReference(getObjectID(), newValue));
  }

  @Override
  protected boolean isEvictable() {
    return true;
  }

  @Override
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
        ObjectID old = setReference(Integer.toString(i), lid);
        if (old != null && !lid.equals(old)) {
          // Formatting
          throw new AssertionError("clearArrayReferences : mapped [" + i + "] to " + lid
                                   + " while there was an exisiting mapping in references : " + old + " : TCObject =  "
                                   + getObjectID() + " : " + this + " version = " + this.getVersion());
        }
        array[i] = null;
        cleared++;
      }
    }
    return cleared;
  }
  
  private int clearObjectReferences(TransparentAccess ta) {

    Map fieldValues = null;
    int cleared = 0;

    TCClass aClazz = tcClazz;

    while (aClazz != null) {

      TCField[] fields = aClazz.getPortableFields();

      for (TCField field : fields) {

        if (!field.canBeReference()) continue;

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
      
      aClazz = aClazz.getSuperclass();
    }
    return cleared;
  }


  public void unresolveReference(String fieldName) {
    TCField field = tcClazz.getField(fieldName);
    if (field == null) { throw new IllegalArgumentException("No such field: " + fieldName); }
    if (!field.canBeReference()) return;

    TransparentAccess ta = (TransparentAccess) getPeerObject();
    if (ta == null) return;

    synchronized (getResolveLock()) {
      // check if already unresolved
      if (hasReferences() && references.containsKey(fieldName)) return;

      HashMap values = new HashMap();
      ta.__tc_getallfields(values);
      Object obj = values.get(fieldName);

      if (obj != null) {
        TCObject tobj = getObjectManager().lookupExistingOrNull(obj);
        if (tobj != null) {
          setValue(fieldName, tobj.getObjectID());
        }
      }
    }
  }
}

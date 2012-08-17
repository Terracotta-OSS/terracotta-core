/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.collections;

import org.terracotta.toolkit.collections.ToolkitSet;
import org.terracotta.toolkit.concurrent.locks.ToolkitReadWriteLock;
import org.terracotta.toolkit.object.serialization.NotSerializableRuntimeException;

import com.tc.net.GroupID;
import com.tc.object.LiteralValues;
import com.tc.object.ObjectID;
import com.tc.object.SerializationUtil;
import com.tc.object.TCObject;
import com.tc.object.bytecode.Manageable;
import com.tc.object.bytecode.ManagerUtil;
import com.terracotta.toolkit.concurrent.locks.UnnamedToolkitReadWriteLock;
import com.terracotta.toolkit.object.AbstractTCToolkitObject;
import com.terracotta.toolkit.object.serialization.SerializedClusterObject;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class ToolkitSetImpl<E> extends AbstractTCToolkitObject implements ToolkitSet<E> {
  protected final Map<E, ObjectID>        localMap;

  protected volatile Object               localResolveLock;
  protected volatile ToolkitReadWriteLock lock;
  protected final List<MutateOperation>   pendingChanges;

  public ToolkitSetImpl() {
    this(new HashMap());
  }

  public ToolkitSetImpl(Map<E, ObjectID> localMap) {
    this.localMap = localMap;
    this.pendingChanges = new ArrayList<MutateOperation>();
  }

  @Override
  public String getName() {
    throw new UnsupportedOperationException();
  }

  void writeLock() {
    lock.writeLock().lock();
  }

  void writeUnlock() {
    lock.writeLock().unlock();
  }

  protected void readLock() {
    lock.readLock().lock();
  }

  protected void readUnlock() {
    lock.readLock().unlock();
  }

  @Override
  public ToolkitReadWriteLock getReadWriteLock() {
    return lock;
  }

  private E getValueFromTCCompatibleObject(Object o) {
    boolean isLiteral = LiteralValues.isLiteralInstance(o);
    if (isLiteral) { return (E) o; }

    return ((SerializedClusterObject<E>) o).getValue(strategy, false);
  }

  @Override
  public boolean add(E e) {
    if (e == null) { throw new NullPointerException("Object passed in to add was null"); }

    writeLock();
    try {
      return unlockedAdd(e);
    } finally {
      writeUnlock();
    }
  }

  private boolean unlockedAdd(E e) {
    synchronized (localResolveLock) {
      applyPendingChanges();
      if (!localMap.containsKey(e)) {
        Object o = createTCCompatibleObject(e);
        ObjectID oid = getObjectIDFromObject(o);
        localMap.put(e, oid);

        logicalInvoke(SerializationUtil.ADD_SIGNATURE, new Object[] { o });
        return true;
      }
      return false;
    }
  }

  private ObjectID getObjectIDFromObject(Object o) {
    if (o instanceof Manageable) { return ((Manageable) o).__tc_managed().getObjectID(); }
    return ObjectID.NULL_ID;
  }

  @Override
  public boolean addAll(Collection<? extends E> c) {
    writeLock();
    try {
      boolean changed = false;
      for (E e : c) {
        if (unlockedAdd(e) && !changed) {
          changed = true;
        }
      }
      return changed;
    } finally {
      writeUnlock();
    }
  }

  @Override
  public void clear() {
    writeLock();
    try {
      synchronized (localResolveLock) {
        pendingChanges.clear();
        localMap.clear();
        logicalInvoke(SerializationUtil.CLEAR_SIGNATURE, new Object[] {});
      }
    } finally {
      writeUnlock();
    }
  }

  @Override
  public boolean isEmpty() {
    readLock();
    try {
      synchronized (localResolveLock) {
        applyPendingChanges();
        return localMap.isEmpty();
      }
    } finally {
      readUnlock();
    }
  }

  private void logicalInvoke(String signature, Object[] params) {
    ManagerUtil.logicalInvoke(this, signature, params);
  }

  @Override
  public void __tc_managed(TCObject t) {
    tcObject = t;
    gid = new GroupID(t.getObjectID().getGroupID());
    localResolveLock = tcObject.getResolveLock();
    String lockID = "__tc_set_" + tcObject.getObjectID().toLong();
    lock = new UnnamedToolkitReadWriteLock(lockID);
  }

  @Override
  public TCObject __tc_managed() {
    return tcObject;
  }

  @Override
  public boolean __tc_isManaged() {
    return tcObject != null;
  }

  @Override
  public Iterator<E> iterator() {
    readLock();
    try {
      applyPendingChanges();
      return new SimpleSetIterator(localMap.keySet().iterator());
    } finally {
      readUnlock();
    }
  }

  class SimpleSetIterator implements Iterator<E> {
    private final Iterator<E> myLocalIterator;
    private E                 currentItem;

    SimpleSetIterator(Iterator<E> myLocalIterator) {
      this.myLocalIterator = myLocalIterator;
    }

    @Override
    public boolean hasNext() {
      readLock();
      try {
        synchronized (localResolveLock) {
          applyPendingChanges();
          return myLocalIterator.hasNext();
        }
      } finally {
        readUnlock();
      }
    }

    @Override
    public E next() {
      readLock();
      try {
        synchronized (localResolveLock) {
          applyPendingChanges();
          currentItem = myLocalIterator.next();
          return currentItem;
        }
      } finally {
        readUnlock();
      }
    }

    @Override
    public void remove() {
      writeLock();
      try {
        synchronized (localResolveLock) {
          applyPendingChanges();
          ObjectID oid = localMap.get(currentItem);
          myLocalIterator.remove();
          Object o = currentItem;
          if (!ObjectID.NULL_ID.equals(oid)) {
            o = ManagerUtil.lookupObject(oid);
          }

          logicalInvoke(SerializationUtil.REMOVE_SIGNATURE, new Object[] { o });
        }
      } finally {
        writeUnlock();
      }
    }
  }

  /**
   * @param o should be an instance of Serializable
   */
  @Override
  public boolean remove(Object o) {
    if (!(o instanceof Serializable)) { throw new NotSerializableRuntimeException(
                                                                                  "Object passed should be Serializable "
                                                                                      + o); }
    writeLock();
    try {
      applyPendingChanges();
      return unlockedRemove(o);
    } finally {
      writeUnlock();
    }
  }

  boolean unlockedRemove(Object o) {
    synchronized (localResolveLock) {
      ObjectID oid = localMap.remove(o);
      if (oid != null) {
        Object tcCompatibleObject = o;
        if (!ObjectID.NULL_ID.equals(oid)) {
          tcCompatibleObject = ManagerUtil.lookupObject(oid);
        }
        logicalInvoke(SerializationUtil.REMOVE_SIGNATURE, new Object[] { tcCompatibleObject });
      }
      return oid != null;
    }
  }

  @Override
  public int size() {
    readLock();
    try {
      synchronized (localResolveLock) {
        applyPendingChanges();
        return localMap.size();
      }
    } finally {
      readUnlock();
    }
  }

  @Override
  public Object[] toArray() {
    readLock();
    try {
      synchronized (localResolveLock) {
        applyPendingChanges();
        return localMap.keySet().toArray();
      }
    } finally {
      readUnlock();
    }
  }

  @Override
  public <T> T[] toArray(T[] a) {
    readLock();
    try {
      synchronized (localResolveLock) {
        applyPendingChanges();
        return localMap.keySet().toArray(a);
      }

    } finally {
      readUnlock();
    }
  }

  @Override
  public void cleanupOnDestroy() {
    this.localMap.clear();
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((localMap == null) ? 0 : localMap.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;
    ToolkitSetImpl other = (ToolkitSetImpl) obj;
    if (localMap == null) {
      if (other.localMap != null) return false;
    } else {
      applyPendingChanges();
      other.applyPendingChanges();
      if (!localMap.equals(other.localMap)) return false;
    }
    return true;
  }

  @Override
  public boolean contains(Object o) {
    readLock();
    try {
      synchronized (localResolveLock) {
        applyPendingChanges();
        return localMap.containsKey(o);
      }
    } finally {
      readUnlock();
    }
  }

  @Override
  public boolean containsAll(Collection<?> c) {
    readLock();
    try {
      synchronized (localResolveLock) {
        applyPendingChanges();
        return localMap.keySet().containsAll(c);
      }
    } finally {
      readUnlock();
    }
  }

  @Override
  public boolean removeAll(Collection<?> c) {
    writeLock();
    try {
      boolean isRemoved = false;
      synchronized (localResolveLock) {
        applyPendingChanges();
        for (Object o : c) {
          if (unlockedRemove(o)) {
            isRemoved = true;
          }
        }
      }
      return isRemoved;
    } finally {
      writeUnlock();
    }
  }

  @Override
  public boolean retainAll(Collection<?> c) {
    writeLock();
    try {
      synchronized (localResolveLock) {
        applyPendingChanges();
        boolean modified = false;
        Iterator<E> e = iterator();
        while (e.hasNext()) {
          if (!c.contains(e.next())) {
            e.remove();
            modified = true;
          }
        }
        return modified;
      }
    } finally {
      writeUnlock();
    }
  }

  /**
   * Internal method to apply all pending changes locally.
   */
  protected void applyPendingChanges() {
    synchronized (localResolveLock) {
      Iterator<MutateOperation> iter = pendingChanges.iterator();
      while (iter.hasNext()) {
        MutateOperation operation = iter.next();
        iter.remove();
        switch (operation.getMethod()) {
          case SerializationUtil.ADD:
            E o = getValueFromTCCompatibleObject(operation.getValue());
            localMap.put(o, getObjectIDFromObject(operation.getValue()));
            break;
          case SerializationUtil.REMOVE:
            localMap.remove(getValueFromTCCompatibleObject(operation.getValue()));
            break;
        }
      }
    }
  }

  public void internalClear() {
    pendingChanges.clear();
    localMap.clear();
  }

  /*
   * Internal methods - called by the applicator. No need to broadcast but only apply locally
   */
  public void internalMutate(int method, Object value) {
    if (!(method == SerializationUtil.REMOVE && pendingChanges
        .remove(new MutateOperation(SerializationUtil.ADD, value)))) {
      pendingChanges.add(new MutateOperation(method, value));
    }
  }

  private static class MutateOperation {
    private final int    method;
    private final Object value;

    public MutateOperation(int method, Object value) {
      super();
      this.method = method;
      this.value = value;
    }

    public int getMethod() {
      return method;
    }

    public Object getValue() {
      return value;
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + method;
      result = prime * result + ((value == null) ? 0 : value.hashCode());
      return result;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) return true;
      if (obj == null) return false;
      if (getClass() != obj.getClass()) return false;
      MutateOperation other = (MutateOperation) obj;
      if (method != other.method) return false;
      if (value == null) {
        if (other.value != null) return false;
      } else if (!value.equals(other.value)) return false;
      return true;
    }
  }

}

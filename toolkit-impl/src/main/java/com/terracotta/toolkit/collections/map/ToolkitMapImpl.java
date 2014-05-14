/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.collections.map;

import org.terracotta.toolkit.ToolkitObjectType;
import org.terracotta.toolkit.collections.ToolkitMap;
import org.terracotta.toolkit.concurrent.locks.ToolkitLock;
import org.terracotta.toolkit.concurrent.locks.ToolkitReadWriteLock;
import org.terracotta.toolkit.internal.concurrent.locks.ToolkitLockTypeInternal;

import com.google.common.base.Preconditions;
import com.tc.abortable.AbortedOperationException;
import com.tc.net.GroupID;
import com.tc.object.LiteralValues;
import com.tc.object.LogicalOperation;
import com.tc.object.ObjectID;
import com.tc.object.TCObject;
import com.tc.object.bytecode.Manageable;
import com.terracotta.toolkit.abortable.ToolkitAbortableOperationException;
import com.terracotta.toolkit.concurrent.locks.ToolkitLockingApi;
import com.terracotta.toolkit.object.AbstractTCToolkitObject;
import com.terracotta.toolkit.object.serialization.SerializedClusterObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ToolkitMapImpl<K, V> extends AbstractTCToolkitObject implements ToolkitMap<K, V> {
  protected final KeyValueHolder<K, V>    keyValueHolder;

  protected volatile Object               localResolveLock;
  protected volatile ToolkitReadWriteLock lock;
  private final transient ToolkitLock     concurrentLock;
  private final List<MutateOperation>     pendingChanges = new ArrayList();

  public ToolkitMapImpl() {
    this(new KeyValueHolder(new ConcurrentHashMap<K, V>()));
  }

  public ToolkitMapImpl(KeyValueHolder keyValueHolder) {
    this.keyValueHolder = keyValueHolder;
    concurrentLock = ToolkitLockingApi.createConcurrentTransactionLock("CONCURRENT", platformService);
  }

  private void readUnlock() {
    lock.readLock().unlock();
  }

  private void readLock() {
    lock.readLock().lock();
  }

  private void writeUnlock() {
    lock.writeLock().unlock();
  }

  private void writeLock() {
    lock.writeLock().lock();
  }

  private void logicalInvoke(LogicalOperation method, Object[] params) {
    concurrentLock.lock();
    try {
      platformService.logicalInvoke(ToolkitMapImpl.this, method, params);
    } finally {
      concurrentLock.unlock();
    }
  }

  protected void applyPendingChanges() {
    for (Iterator<MutateOperation> iterator = pendingChanges.iterator(); iterator.hasNext();) {
      MutateOperation mutateOperation = iterator.next();
      Object tcKey = mutateOperation.getKey();
      Object actualKey = getValueFromTCCompatibleObject(tcKey);

      switch (mutateOperation.getMethod()) {
        case PUT:
          ObjectID oidKey = getObjectIDForKey(tcKey);
          Object actualValue = getValueFromTCCompatibleObject(mutateOperation.getValue());
          keyValueHolder.put((K) actualKey, (V) actualValue, oidKey);
          break;
        case REMOVE:
          keyValueHolder.remove(actualKey);
          break;
      }

      iterator.remove();
    }
  }

  private K getValueFromTCCompatibleObject(Object o) {
    boolean isLiteral = LiteralValues.isLiteralInstance(o);
    if (isLiteral) { return (K) o; }

    return ((SerializedClusterObject<K>) o).getValue(serStrategy, false, false);
  }

  @Override
  public void __tc_managed(TCObject t) {
    tcObject = t;
    gid = new GroupID(t.getObjectID().getGroupID());
    localResolveLock = tcObject.getResolveLock();
    lock = ToolkitLockingApi.createUnnamedReadWriteLock(ToolkitObjectType.MAP, tcObject.getObjectID(), platformService,
                                                        ToolkitLockTypeInternal.WRITE);
  }

  @Override
  public TCObject __tc_managed() {
    return tcObject;
  }

  @Override
  public boolean __tc_isManaged() {
    return tcObject != null;
  }

  private Object getOrCreateObjectForKey(Object key) {
    if (LiteralValues.isLiteralInstance(key)) { return key; }
    ObjectID oidKey = keyValueHolder.getKeyObjectID(key);
    if (oidKey == null) {
      return createTCCompatibleObject(key);
    } else {
      try {
        return platformService.lookupObject(oidKey);
      } catch (AbortedOperationException e) {
        throw new ToolkitAbortableOperationException(e);
      }
    }
  }

  private ObjectID getObjectIDForKey(Object key) {
    if (key instanceof Manageable) {
      return ((Manageable) key).__tc_managed().getObjectID();
    } else if (LiteralValues.isLiteralInstance(key)) { return ObjectID.NULL_ID; }
    throw new IllegalStateException();
  }

  @Override
  public int size() {
    readLock();
    try {
      synchronized (localResolveLock) {
        applyPendingChanges();
        return keyValueHolder.size();
      }
    } finally {
      readUnlock();
    }
  }

  @Override
  public boolean isEmpty() {
    return size() == 0;
  }

  @Override
  public boolean containsKey(Object key) {
    readLock();
    try {
      synchronized (localResolveLock) {
        applyPendingChanges();
        return keyValueHolder.containsKey(key);
      }
    } finally {
      readUnlock();
    }
  }

  @Override
  public boolean containsValue(Object value) {
    readLock();
    try {
      synchronized (localResolveLock) {
        applyPendingChanges();
        return keyValueHolder.containsValue(value);
      }
    } finally {
      readUnlock();
    }
  }

  @Override
  public V get(Object key) {
    readLock();
    try {
      synchronized (localResolveLock) {
        applyPendingChanges();
        return keyValueHolder.get(key);
      }
    } finally {
      readUnlock();
    }
  }

  @Override
  public V put(K key, V value) {
    writeLock();
    try {
      synchronized (localResolveLock) {
        return unlockedPut(key, value);
      }
    } finally {
      writeUnlock();
    }
  }

  @Override
  public V putIfAbsent(K key, V value) {
    writeLock();
    try {
      synchronized (localResolveLock) {
        applyPendingChanges();
        V old = keyValueHolder.get(key);
        if (old != null) { return old; }
        unlockedPut(key, value);

        return null;
      }
    } finally {
      writeUnlock();
    }
  }

  private V unlockedPut(K key, V value) {
    applyPendingChanges();
    Preconditions.checkNotNull(value);

    V rv = keyValueHolder.get(key);

    Object tcKey = getOrCreateObjectForKey(key);
    Object tcVal = createTCCompatibleObject(value);

    logicalInvoke(LogicalOperation.PUT, new Object[] { tcKey, tcVal });
    keyValueHolder.put(key, value, getObjectIDForKey(tcKey));

    return rv;
  }

  @Override
  public V remove(Object key) {
    writeLock();
    try {
      synchronized (localResolveLock) {
        return unlockedRemove(key);
      }
    } finally {
      writeUnlock();
    }
  }

  @Override
  public boolean remove(Object key, Object value) {
    writeLock();
    try {
      synchronized (localResolveLock) {
        applyPendingChanges();

        V v = keyValueHolder.get(key);
        if (!v.equals(value)) { return false; }

        unlockedRemove(key);
        return true;
      }
    } finally {
      writeUnlock();
    }
  }

  @Override
  public V replace(K key, V value) {
    writeLock();
    try {
      synchronized (localResolveLock) {
        applyPendingChanges();

        if (keyValueHolder.containsKey(key)) { return unlockedPut(key, value); }
        return null;
      }
    } finally {
      writeUnlock();
    }
  }

  @Override
  public boolean replace(K key, V oldValue, V newValue) {
    writeLock();
    try {
      synchronized (localResolveLock) {
        applyPendingChanges();

        V valueFetched = keyValueHolder.get(key);
        if (valueFetched != null && valueFetched.equals(oldValue)) {
          unlockedPut(key, newValue);
          return true;
        }
        return false;
      }
    } finally {
      writeUnlock();
    }
  }

  protected V unlockedRemove(Object key) {
    applyPendingChanges();
    ObjectID oidKey = keyValueHolder.getKeyObjectID(key);
    if (oidKey == null) { return null; }

    Object tcKey = getOrCreateObjectForKey(key);
    logicalInvoke(LogicalOperation.REMOVE, new Object[] { tcKey });
    V oldValue = keyValueHolder.remove(key);

    return oldValue;
  }

  @Override
  public void putAll(Map<? extends K, ? extends V> m) {
    writeLock();
    try {
      synchronized (localResolveLock) {
        applyPendingChanges();
        for (Entry<? extends K, ? extends V> entry : m.entrySet()) {
          unlockedPut(entry.getKey(), entry.getValue());
        }
      }
    } finally {
      writeUnlock();
    }
  }

  @Override
  public void clear() {
    writeLock();
    try {
      synchronized (localResolveLock) {
        applyPendingChanges();
        logicalInvoke(LogicalOperation.CLEAR, new Object[] {});
        this.keyValueHolder.clear();
      }
    } finally {
      writeUnlock();
    }
  }

  @Override
  public Set<K> keySet() {
    readLock();
    try {
      synchronized (localResolveLock) {
        applyPendingChanges();
        return new ToolkitKeySet(keyValueHolder.keySet());
      }
    } finally {
      readUnlock();
    }
  }

  @Override
  public Collection<V> values() {
    readLock();
    try {
      synchronized (localResolveLock) {
        applyPendingChanges();
        return new ToolkitValueCollection(keyValueHolder.getMap());
      }
    } finally {
      readUnlock();
    }
  }

  @Override
  public Set<Entry<K, V>> entrySet() {
    readLock();
    try {
      synchronized (localResolveLock) {
        applyPendingChanges();
        return new ToolkitMapEntrySet(keyValueHolder.entrySet());
      }
    } finally {
      readUnlock();
    }
  }

  @Override
  public String getName() {
    throw new UnsupportedOperationException();
  }

  @Override
  public ToolkitReadWriteLock getReadWriteLock() {
    return this.lock;
  }

  private static class MutateOperation {
    private final METHOD method;
    private final Object value;
    private final Object key;

    public enum METHOD {
      PUT, REMOVE
    }

    public MutateOperation(METHOD method, Object key, Object value) {
      super();
      this.method = method;
      this.key = key;
      this.value = value;
    }

    public METHOD getMethod() {
      return method;
    }

    public Object getValue() {
      return value;
    }

    public Object getKey() {
      return key;
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((key == null) ? 0 : key.hashCode());
      return result;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) return true;
      if (obj == null) return false;
      if (getClass() != obj.getClass()) return false;
      MutateOperation other = (MutateOperation) obj;
      if (key == null) {
        if (other.key != null) return false;
      } else if (!key.equals(other.key)) return false;
      return true;
    }
  }

  @Override
  public void cleanupOnDestroy() {
    this.keyValueHolder.clear();
  }

  static class KeyValueHolder<K, V> {
    private final Map<K, V>            keyToValue;
    private final HashMap<K, ObjectID> keyToIds = new HashMap<K, ObjectID>();

    public KeyValueHolder(Map<K, V> map) {
      this.keyToValue = map;
    }

    public Map<K, V> getMap() {
      return keyToValue;
    }
    public void clear() {
      keyToIds.clear();
      keyToValue.clear();
    }

    public V remove(Object key) {
      keyToIds.remove(key);
      return keyToValue.remove(key);
    }

    public void put(K key, V value, ObjectID oid) {
      keyToValue.put(key, value);
      keyToIds.put(key, oid);
    }

    public ObjectID getKeyObjectID(Object key) {
      return keyToIds.get(key);
    }

    public V get(Object key) {
      return keyToValue.get(key);
    }

    public boolean containsValue(Object value) {
      return keyToValue.containsValue(value);
    }

    public boolean containsKey(Object key) {
      return keyToValue.containsKey(key);
    }

    public int size() {
      return keyToValue.size();
    }

    public void removeObjectIDForKey(Object key) {
      keyToIds.remove(key);
    }

    public Collection<V> values() {
      return keyToValue.values();
    }

    public Set<java.util.Map.Entry<K, V>> entrySet() {
      return keyToValue.entrySet();
    }

    public Set<K> keySet() {
      return keyToValue.keySet();
    }
  }

  class ToolkitKeySet implements Set<K> {
    private final Set<K> keySet;

    public ToolkitKeySet(Set<K> keySet) {
      this.keySet = keySet;
    }

    @Override
    public int size() {
      readLock();
      try {
        synchronized (localResolveLock) {
          applyPendingChanges();
          return keySet.size();
        }
      } finally {
        readUnlock();
      }
    }

    @Override
    public boolean isEmpty() {
      return size() == 0;
    }

    @Override
    public boolean contains(Object o) {
      readLock();
      try {
        synchronized (localResolveLock) {
          applyPendingChanges();
          return keySet.contains(o);
        }
      } finally {
        readUnlock();
      }
    }

    @Override
    public Iterator<K> iterator() {
      readLock();
      try {
        synchronized (localResolveLock) {
          applyPendingChanges();
          return new KeySetIterator(keySet.iterator());
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
          return keySet.toArray();
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
          return keySet.toArray(a);
        }
      } finally {
        readUnlock();
      }
    }

    @Override
    public String toString() {
      readLock();
      try {
        synchronized (localResolveLock) {
          applyPendingChanges();
          return keySet.toString();
        }
      } finally {
        readUnlock();
      }

    }

    @Override
    public boolean add(K e) {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean remove(Object o) {
      return ToolkitMapImpl.this.remove(o) != null;
    }

    @Override
    public boolean containsAll(Collection<?> c) {
      readLock();
      try {
        synchronized (localResolveLock) {
          applyPendingChanges();
          return keySet.containsAll(c);
        }
      } finally {
        readUnlock();
      }
    }

    @Override
    public boolean addAll(Collection<? extends K> c) {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean retainAll(Collection<?> c) {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean removeAll(Collection<?> c) {
      writeLock();
      try {
        synchronized (localResolveLock) {
          boolean changed = false;
          for (Object obj : c) {
            if (ToolkitMapImpl.this.remove(obj) != null && !changed) {
              changed = true;
            }
          }
          return changed;
        }
      } finally {
        writeUnlock();
      }
    }

    @Override
    public void clear() {
      ToolkitMapImpl.this.clear();
    }
  }

  private class KeySetIterator implements Iterator<K> {
    private final Iterator<K> iterator;
    private volatile K        currentValue;

    public KeySetIterator(Iterator<K> iterator) {
      this.iterator = iterator;
    }

    @Override
    public boolean hasNext() {
      readLock();
      try {
        synchronized (localResolveLock) {
          applyPendingChanges();
          return iterator.hasNext();
        }
      } finally {
        readUnlock();
      }
    }

    @Override
    public K next() {
      readLock();
      try {
        synchronized (localResolveLock) {
          applyPendingChanges();
          currentValue = iterator.next();
          return currentValue;
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
          iterator.remove();
          Object tcKey = getOrCreateObjectForKey(currentValue);
          logicalInvoke(LogicalOperation.REMOVE, new Object[] { tcKey });
          keyValueHolder.removeObjectIDForKey(currentValue);
        }
      } finally {
        writeUnlock();
      }
    }
  }

  class ToolkitValueCollection implements Collection<V> {
    private final Collection<V>   values;
    private final Map<K, V>     localMap;

    public ToolkitValueCollection(Map<K, V> localMap) {
      this.values = localMap.values();
      this.localMap = localMap;
    }

    @Override
    public int size() {
      readLock();
      try {
        synchronized (localResolveLock) {
          applyPendingChanges();
          return values.size();
        }
      } finally {
        readUnlock();
      }
    }

    @Override
    public boolean isEmpty() {
      return size() == 0;
    }

    @Override
    public boolean contains(Object o) {
      readLock();
      try {
        synchronized (localResolveLock) {
          applyPendingChanges();
          return values.contains(o);
        }
      } finally {
        readUnlock();
      }
    }

    @Override
    public Iterator<V> iterator() {
      readLock();
      try {
        synchronized (localResolveLock) {
          applyPendingChanges();
          return new ValuesIterator(values.iterator());
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
          return values.toArray();
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
          return values.toArray(a);
        }
      } finally {
        readUnlock();
      }
    }

    @Override
    public String toString() {
      readLock();
      try {
        synchronized (localResolveLock) {
          applyPendingChanges();
          return values.toString();
        }
      } finally {
        readUnlock();
      }
    }

    @Override
    public boolean add(V e) {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean remove(Object o) {
      writeLock();
      try {
        synchronized (localResolveLock) {
          applyPendingChanges();
          if (!values.contains(o)) { return false; }
          Set s = new ToolkitKeySet(localMap.keySet());
          for (Iterator iter = s.iterator(); iter.hasNext();) {
            if (o.equals(localMap.get(iter.next()))) {
              iter.remove();
              return true;
            }
          }
          return false;
        }
      } finally {
        writeUnlock();
      }
    }

    @Override
    public boolean containsAll(Collection<?> c) {
      readLock();
      try {
        synchronized (localResolveLock) {
          applyPendingChanges();
          return values.containsAll(c);
        }
      } finally {
        readUnlock();
      }
    }

    @Override
    public boolean addAll(Collection<? extends V> c) {
      throw new UnsupportedOperationException();

    }

    @Override
    public boolean removeAll(Collection<?> c) {
      writeLock();
      try {
        synchronized (localResolveLock) {
          applyPendingChanges();
          boolean change = false;
          Set s = new ToolkitKeySet(localMap.keySet());
            for (Iterator iter = s.iterator(); iter.hasNext();) {
            if (c.contains(localMap.get(iter.next()))) {
                iter.remove();
                change = true;
            }
          }
          return change;
        }
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
          boolean change = false;
          Set s = new ToolkitKeySet(localMap.keySet());
          for (Iterator iter = s.iterator(); iter.hasNext();) {
            if (!c.contains(localMap.get(iter.next()))) {
              iter.remove();
              change = true;
            }
          }
          return change;
        }
      } finally {
        writeUnlock();
      }
    }

    @Override
    public void clear() {
      writeLock();
      try {
        synchronized (localResolveLock) {
          applyPendingChanges();
          Set s = new ToolkitKeySet(localMap.keySet());
          for (Iterator iter = s.iterator(); iter.hasNext();) {
            iter.next();
            iter.remove();
          }
        }
      } finally {
        writeUnlock();
      }
    }

  }

  private class ValuesIterator implements Iterator<V> {
    private final Iterator<V> iterator;

    public ValuesIterator(Iterator<V> iterator) {
      this.iterator = iterator;
    }

    @Override
    public boolean hasNext() {
      readLock();
      try {
        synchronized (localResolveLock) {
          applyPendingChanges();
          return iterator.hasNext();
        }
      } finally {
        readUnlock();
      }
    }

    @Override
    public V next() {
      readLock();
      try {
        synchronized (localResolveLock) {
          applyPendingChanges();
          return iterator.next();
        }
      } finally {
        readUnlock();
      }
    }

    @Override
    public void remove() {
      throw new UnsupportedOperationException();
    }
  }

  class ToolkitMapEntrySet implements Set<Entry<K, V>> {
    private final Set<Entry<K, V>> entrySet;

    public ToolkitMapEntrySet(Set<Entry<K, V>> entrySet) {
      this.entrySet = entrySet;
    }

    @Override
    public int size() {
      readLock();
      try {
        synchronized (localResolveLock) {
          applyPendingChanges();
          return entrySet.size();
        }
      } finally {
        readUnlock();
      }
    }

    @Override
    public boolean isEmpty() {
      return size() == 0;
    }

    @Override
    public boolean contains(Object o) {
      readLock();
      try {
        synchronized (localResolveLock) {
          applyPendingChanges();
          return entrySet.contains(o);
        }
      } finally {
        readUnlock();
      }
    }

    @Override
    public Iterator<java.util.Map.Entry<K, V>> iterator() {
      readLock();
      try {
        synchronized (localResolveLock) {
          applyPendingChanges();
          return new EntrySetIterator(entrySet.iterator());
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
          return entrySet.toArray();
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
          return entrySet.toArray(a);
        }
      } finally {
        readUnlock();
      }
    }

    @Override
    public String toString() {
      readLock();
      try {
        synchronized (localResolveLock) {
          applyPendingChanges();
          return entrySet.toString();
        }
      } finally {
        readUnlock();
      }
    }

    @Override
    public boolean add(Entry<K, V> e) {
      writeLock();
      try {
        synchronized (localResolveLock) {
          applyPendingChanges();
          if (entrySet.contains(e)) { return false; }

          unlockedPut(e.getKey(), e.getValue());
          return true;
        }
      } finally {
        writeUnlock();
      }
    }

    @Override
    public boolean remove(Object o) {
      writeLock();
      try {
        synchronized (localResolveLock) {
          applyPendingChanges();
          if (!entrySet.contains(o)) { return false; }

          Entry e = (Entry) o;
          unlockedRemove(e.getKey());
          return true;
        }
      } finally {
        writeUnlock();
      }
    }

    @Override
    public boolean containsAll(Collection<?> c) {
      readLock();
      try {
        synchronized (localResolveLock) {
          applyPendingChanges();
          return entrySet.containsAll(c);
        }
      } finally {
        readUnlock();
      }
    }

    @Override
    public boolean addAll(Collection<? extends Entry<K, V>> c) {
      writeLock();
      try {
        synchronized (localResolveLock) {
          applyPendingChanges();
          boolean containsAll = containsAll(c);
          if (c.size() == 0 || containsAll) { return false; }

          for (Entry<K, V> entry : c) {
            unlockedPut(entry.getKey(), entry.getValue());
          }
          return true;
        }
      } finally {
        writeUnlock();
      }
    }

    @Override
    public boolean retainAll(Collection<?> c) {
      writeLock();
      try {
        synchronized (localResolveLock) {
          Set<Entry> toRemoved = new HashSet<Entry>();
          applyPendingChanges();
          for (Entry entry : entrySet) {
            if (!c.contains(entry)) {
              toRemoved.add(entry);
            }
          }

          return removeAll(toRemoved);
        }
      } finally {
        writeUnlock();
      }
    }

    @Override
    public boolean removeAll(Collection<?> c) {
      writeLock();
      try {
        synchronized (localResolveLock) {
          boolean changed = false;
          for (Object obj : c) {
            Entry entry = (Entry) obj;
            if (unlockedRemove(entry.getKey()) != null && !changed) {
              changed = true;
            }
          }
          return changed;
        }
      } finally {
        writeUnlock();
      }
    }

    @Override
    public void clear() {
      ToolkitMapImpl.this.clear();
    }
  }

  private class EntrySetIterator implements Iterator<Entry<K, V>> {
    private final Iterator<Entry<K, V>> iterator;
    private Entry<K, V>                 entry;

    public EntrySetIterator(Iterator<Entry<K, V>> iterator) {
      this.iterator = iterator;
    }

    @Override
    public boolean hasNext() {
      readLock();
      try {
        synchronized (localResolveLock) {
          applyPendingChanges();
          return iterator.hasNext();
        }
      } finally {
        readUnlock();
      }
    }

    @Override
    public Entry<K, V> next() {
      readLock();
      try {
        synchronized (localResolveLock) {
          applyPendingChanges();
          entry = iterator.next();
          return entry;
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
          iterator.remove();
          Object tcKey = getOrCreateObjectForKey(entry.getKey());
          logicalInvoke(LogicalOperation.REMOVE, new Object[] { tcKey });
          keyValueHolder.removeObjectIDForKey(entry.getKey());
        }
      } finally {
        writeUnlock();
      }
    }
  }

  public void internalPut(Object key, Object value) {
    pendingChanges.add(new MutateOperation(MutateOperation.METHOD.PUT, key, value));
  }

  public void internalRemove(Object key) {
    MutateOperation mutateOperation = new MutateOperation(MutateOperation.METHOD.REMOVE, key, null);
    pendingChanges.add(mutateOperation);
  }

  public void internalClear() {
    pendingChanges.clear();
    keyValueHolder.clear();
  }
}

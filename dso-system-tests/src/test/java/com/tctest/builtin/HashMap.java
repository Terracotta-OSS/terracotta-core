/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tctest.builtin;

import com.tc.object.SerializationUtil;
import com.tc.object.bytecode.ManagerUtil;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class HashMap<K, V> implements Map<K, V> {

  private final Map<K, V> data = new java.util.concurrent.ConcurrentHashMap<K, V>(16, 0.75F, 1);

  public int size() {
    return data.size();
  }

  public boolean isEmpty() {
    return data.isEmpty();
  }

  public boolean containsKey(Object key) {
    return data.containsKey(key);
  }

  public boolean containsValue(Object value) {
    return data.containsValue(value);
  }

  public V get(Object key) {
    return data.get(key);
  }

  public V put(K key, V value) {
    ManagerUtil.checkWriteAccess(this);
    ManagerUtil.logicalInvoke(this, SerializationUtil.PUT_SIGNATURE, new Object[] { key, value });
    return data.put(key, value);
  }

  public V remove(Object key) {
    ManagerUtil.checkWriteAccess(this);
    ManagerUtil.logicalInvoke(this, SerializationUtil.REMOVE_KEY_SIGNATURE, new Object[] { key });
    return data.remove(key);
  }

  public void clear() {
    ManagerUtil.checkWriteAccess(this);
    ManagerUtil.logicalInvoke(this, SerializationUtil.CLEAR_SIGNATURE, new Object[] {});
    data.clear();
  }

  public void putAll(Map<? extends K, ? extends V> m) {
    for (java.util.Map.Entry<? extends K, ? extends V> entry : m.entrySet()) {
      put(entry.getKey(), entry.getValue());
    }
  }

  public Set<K> keySet() {
    return new KeySet(data.keySet());
  }

  public Collection<V> values() {
    return Collections.unmodifiableCollection(data.values());
  }

  public Set<java.util.Map.Entry<K, V>> entrySet() {
    return new UnmodifiableEntrySet(data.entrySet());
  }

  @Override
  public boolean equals(Object o) {
    return data.equals(o);
  }

  @Override
  public int hashCode() {
    return data.hashCode();
  }

  @Override
  public String toString() {
    return data.toString();
  }

  private class UnmodifiableEntrySet implements Set<java.util.Map.Entry<K, V>> {

    private final Set<java.util.Map.Entry<K, V>> entries;

    public UnmodifiableEntrySet(Set<java.util.Map.Entry<K, V>> entries) {
      this.entries = entries;
    }

    public int size() {
      return entries.size();
    }

    public boolean isEmpty() {
      return entries.isEmpty();
    }

    public boolean contains(Object o) {
      return entries.contains(o);
    }

    public Iterator<java.util.Map.Entry<K, V>> iterator() {
      return new UnmodifiableEntrySetIterator(entries.iterator());
    }

    public Object[] toArray() {
      return entries.toArray();
    }

    public <T> T[] toArray(T[] a) {
      return entries.toArray(a);
    }

    public boolean add(java.util.Map.Entry<K, V> e) {
      throw new UnsupportedOperationException();
    }

    public boolean remove(Object o) {
      throw new UnsupportedOperationException();
    }

    public boolean containsAll(Collection<?> c) {
      return entries.containsAll(c);
    }

    public boolean addAll(Collection<? extends java.util.Map.Entry<K, V>> c) {
      throw new UnsupportedOperationException();
    }

    public boolean retainAll(Collection<?> c) {
      throw new UnsupportedOperationException();
    }

    public boolean removeAll(Collection<?> c) {
      throw new UnsupportedOperationException();
    }

    public void clear() {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean equals(Object o) {
      return entries.equals(o);
    }

    @Override
    public int hashCode() {
      return entries.hashCode();
    }

    @Override
    public String toString() {
      return entries.toString();
    }
  }

  private class UnmodifiableEntrySetIterator implements Iterator<java.util.Map.Entry<K, V>> {

    private final Iterator<java.util.Map.Entry<K, V>> iterator;

    public UnmodifiableEntrySetIterator(Iterator<java.util.Map.Entry<K, V>> iterator) {
      this.iterator = iterator;
    }

    @Override
    public boolean hasNext() {
      return iterator.hasNext();
    }

    @Override
    public java.util.Map.Entry<K, V> next() {
      final java.util.Map.Entry<K, V> entry = iterator.next();

      return new java.util.Map.Entry<K, V>() {
        @Override
        public K getKey() {
          return entry.getKey();
        }

        @Override
        public V getValue() {
          return entry.getValue();
        }

        @Override
        public V setValue(V value) {
          throw new UnsupportedOperationException();
        }
      };
    }

    @Override
    public void remove() {
      throw new UnsupportedOperationException();
    }
  }

  private class KeySet implements Set<K> {

    private final Set<K> keySet;

    public KeySet(Set<K> keySet) {
      this.keySet = keySet;
    }

    public int size() {
      return keySet.size();
    }

    public boolean isEmpty() {
      return keySet.isEmpty();
    }

    public boolean contains(Object o) {
      return keySet.contains(o);
    }

    public Iterator<K> iterator() {
      final Iterator<K> iterator = keySet.iterator();
      return new Iterator<K>() {

        private K last;

        @Override
        public boolean hasNext() {
          return iterator.hasNext();
        }

        @Override
        public K next() {
          last = iterator.next();
          return last;
        }

        @Override
        public void remove() {
          ManagerUtil.checkWriteAccess(HashMap.this);
          iterator.remove();
          ManagerUtil.logicalInvoke(HashMap.this, SerializationUtil.REMOVE_SIGNATURE, new Object[] { last });
        }
      };
    }

    public Object[] toArray() {
      return keySet.toArray();
    }

    public <T> T[] toArray(T[] a) {
      return keySet.toArray(a);
    }

    public boolean add(K e) {
      throw new UnsupportedOperationException();
    }

    public boolean remove(Object o) {
      throw new UnsupportedOperationException();
    }

    public boolean containsAll(Collection<?> c) {
      return keySet.containsAll(c);
    }

    public boolean addAll(Collection<? extends K> c) {
      throw new UnsupportedOperationException();
    }

    public boolean retainAll(Collection<?> c) {
      throw new UnsupportedOperationException();
    }

    public boolean removeAll(Collection<?> c) {
      throw new UnsupportedOperationException();
    }

    public void clear() {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean equals(Object o) {
      return keySet.equals(o);
    }

    @Override
    public int hashCode() {
      return keySet.hashCode();
    }

    @Override
    public String toString() {
      return keySet.toString();
    }

  }

}

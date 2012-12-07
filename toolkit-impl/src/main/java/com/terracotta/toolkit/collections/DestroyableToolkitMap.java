/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.collections;

import org.terracotta.toolkit.collections.ToolkitMap;
import org.terracotta.toolkit.concurrent.locks.ToolkitReadWriteLock;

import com.terracotta.toolkit.collections.map.ToolkitMapImpl;
import com.terracotta.toolkit.factory.ToolkitObjectFactory;
import com.terracotta.toolkit.object.AbstractDestroyableToolkitObject;
import com.terracotta.toolkit.rejoin.RejoinAwareToolkitMap;
import com.terracotta.toolkit.rejoin.RejoinCallback;
import com.terracotta.toolkit.type.IsolatedClusteredObjectLookup;
import com.terracotta.toolkit.util.ToolkitInstanceProxy;

import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

public class DestroyableToolkitMap<K, V> extends AbstractDestroyableToolkitObject<ToolkitMap> implements
    ToolkitMap<K, V>, RejoinAwareToolkitMap<K, V> {

  private final String                                        name;
  private volatile ToolkitMap<K, V>                           map;
  private final IsolatedClusteredObjectLookup<ToolkitMapImpl> lookup;
  private final List<WeakReference<DestroyableCollection>>    colKeySet   = new CopyOnWriteArrayList<WeakReference<DestroyableCollection>>();
  private final List<WeakReference<DestroyableCollection>>    colEntrySet = new CopyOnWriteArrayList<WeakReference<DestroyableCollection>>();

  public DestroyableToolkitMap(ToolkitObjectFactory<ToolkitMap> factory,
                               IsolatedClusteredObjectLookup<ToolkitMapImpl> lookup, ToolkitMapImpl<K, V> map,
                               String name) {
    super(factory);
    this.lookup = lookup;
    this.map = map;
    this.name = name;
    map.setApplyDestroyCallback(getDestroyApplicator());
  }

  @Override
  public void rejoinStarted() {
    this.map = ToolkitInstanceProxy.newRejoinInProgressProxy(name, ToolkitMap.class);
    markRejoin(colKeySet);
    markRejoin(colEntrySet);

  }

  private void markRejoin(List<WeakReference<DestroyableCollection>> listOfWeakReferences) {
    for (WeakReference<DestroyableCollection> entrySetWeakRef : listOfWeakReferences) {
      DestroyableCollection entrySet = entrySetWeakRef.get();
      if (entrySet != null) {
        entrySet.rejoinStarted();
      }
      listOfWeakReferences.remove(entrySetWeakRef);
    }
  }

  @Override
  public void rejoinCompleted() {
    ToolkitMapImpl afterRejoin = lookup.lookupClusteredObject(name);
    if (afterRejoin != null) {
      this.map = afterRejoin;
    } else {
      // didn't find backing clustered object after rejoin - must have been destroyed
      // so apply destroy locally
      applyDestroy();
    }
  }

  @Override
  public void applyDestroy() {
    this.map = ToolkitInstanceProxy.newDestroyedInstanceProxy(name, ToolkitMap.class);
  }

  @Override
  public void doDestroy() {
    map.destroy();
  }

  @Override
  public ToolkitReadWriteLock getReadWriteLock() {
    return map.getReadWriteLock();
  }

  @Override
  public int size() {
    return map.size();
  }

  @Override
  public boolean isEmpty() {
    return map.isEmpty();
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public boolean containsKey(Object key) {
    return map.containsKey(key);
  }

  @Override
  public boolean containsValue(Object value) {
    return map.containsValue(value);
  }

  @Override
  public V get(Object key) {
    return map.get(key);
  }

  @Override
  public V put(K key, V value) {
    return map.put(key, value);
  }

  @Override
  public V remove(Object key) {
    return map.remove(key);
  }

  @Override
  public void putAll(Map<? extends K, ? extends V> m) {
    map.putAll(m);
  }

  @Override
  public void clear() {
    map.clear();
  }

  @Override
  public Set<K> keySet() {
    DestroyableSet destroyableSet = new DestroyableSet(map.keySet());
    colKeySet.add(new WeakReference<DestroyableCollection>(destroyableSet));
    return destroyableSet;
  }

  @Override
  public Collection<V> values() {
    return new DestroyableCollection(map.values());
  }

  @Override
  public Set<Entry<K, V>> entrySet() {
    DestroyableSet destroyableSet = new DestroyableSet(map.entrySet());
    colEntrySet.add(new WeakReference<DestroyableCollection>(destroyableSet));
    return destroyableSet;
  }

  private void exceptionIfDestroyed() {
    if (isDestroyed()) { throw new IllegalStateException("This object has already been destroyed"); }
  }

  private class DestroyableSet extends DestroyableCollection implements Set {
    public DestroyableSet(Set set) {
      super(set);
    }
  }

  private class DestroyableCollection implements Collection, RejoinCallback {
    private Collection collection;

    public DestroyableCollection(Collection collection) {
      this.collection = collection;
    }

    @Override
    public int size() {
      exceptionIfDestroyed();
      return collection.size();
    }

    @Override
    public boolean isEmpty() {
      exceptionIfDestroyed();
      return collection.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
      exceptionIfDestroyed();
      return collection.contains(o);
    }

    @Override
    public Iterator iterator() {
      exceptionIfDestroyed();
      return new DestroyableIterator(collection.iterator(), DestroyableToolkitMap.this);
    }

    @Override
    public Object[] toArray() {
      exceptionIfDestroyed();
      return collection.toArray();
    }

    @Override
    public Object[] toArray(Object[] a) {
      exceptionIfDestroyed();
      return collection.toArray(a);
    }

    @Override
    public boolean add(Object e) {
      exceptionIfDestroyed();
      return collection.add(e);
    }

    @Override
    public boolean remove(Object o) {
      exceptionIfDestroyed();
      return collection.remove(o);
    }

    @Override
    public boolean containsAll(Collection c) {
      exceptionIfDestroyed();
      return collection.containsAll(c);
    }

    @Override
    public boolean addAll(Collection c) {
      exceptionIfDestroyed();
      return collection.addAll(c);
    }

    @Override
    public boolean removeAll(Collection c) {
      exceptionIfDestroyed();
      return collection.removeAll(c);
    }

    @Override
    public boolean retainAll(Collection c) {
      exceptionIfDestroyed();
      return collection.retainAll(c);
    }

    @Override
    public void clear() {
      exceptionIfDestroyed();
      collection.clear();
    }

    @Override
    public void rejoinStarted() {
      this.collection = ToolkitInstanceProxy.newRejoinInProgressProxy(name, Set.class);
    }

    @Override
    public void rejoinCompleted() {
      // no-op
    }
  }

  @Override
  public V putIfAbsent(K key, V value) {
    return map.putIfAbsent(key, value);
  }

  @Override
  public boolean remove(Object arg0, Object arg1) {
    return map.remove(arg0, arg1);
  }

  @Override
  public V replace(K key, V value) {
    return map.replace(key, value);
  }

  @Override
  public boolean replace(K key, V oldValue, V newValue) {
    return map.replace(key, oldValue, newValue);
  }

}

/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tcclient.ehcache;

import com.tc.util.Assert;
import com.tcclient.cache.CacheData;
import com.tcclient.cache.CacheDataStore;
import com.tcclient.cache.Expirable;

import java.io.Serializable;
import java.util.AbstractCollection;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Up to now, TimeExpiryMap is only used in the context of Ehcache which is synchronized and auto-locked in the
 * MemoryStore class; therefore, no synchronization is needed here. TimeExpiryMap will be shared when a Ehcache is
 * clustered. That is why we do not need to make store and dtmStore as root like what Session does.
 */
public class TimeExpiryMap implements Map, Expirable, Cloneable, Serializable {
  protected final CacheDataStore timeExpiryDataStore;

  public TimeExpiryMap(long invalidatorSleepSeconds, long maxIdleTimeoutSeconds, long maxTTLSeconds, String cacheName) {
    timeExpiryDataStore = new CacheDataStore(invalidatorSleepSeconds, maxIdleTimeoutSeconds, maxTTLSeconds,
                                             "CacheInvalidator - " + cacheName, this);
  }

  // For test only
  public TimeExpiryMap(long invalidatorSleepSeconds, long maxIdleTimeoutSeconds, long maxTTLSeconds, String cacheName,
                       boolean globalEvictionEnabled, int globalEvictionDuration, int concurrency, int evictorPoolSize) {
    timeExpiryDataStore = new CacheDataStore(invalidatorSleepSeconds, maxIdleTimeoutSeconds, maxTTLSeconds,
                                             "CacheInvalidator - " + cacheName, this, globalEvictionEnabled,
                                             globalEvictionDuration, concurrency, evictorPoolSize);
  }

  public void initialize() {
    timeExpiryDataStore.initialize();
  }
  
  public Object put(Object key, Object value) {
    return timeExpiryDataStore.put(key, value);
  }

  public void expire(Object key) {
    processExpired(key);
  }

  protected void processExpired(Object key) {
    //
  }

  public void clear() {
    timeExpiryDataStore.clear();
  }

  public boolean containsKey(Object key) {
    return timeExpiryDataStore.getStore(key).containsKey(key);
  }

  public boolean containsValue(Object value) {
    return timeExpiryDataStore.containsValue(value);
  }

  public Set entrySet() {
    return new EntrySetWrapper(timeExpiryDataStore.entrySet());
  }

  Set nativeEntrySet() {
    return timeExpiryDataStore.entrySet();
  }

  public Object get(Object key) {
    return timeExpiryDataStore.get(key);
  }

  public boolean isEmpty() {
    return timeExpiryDataStore.isEmpty();
  }

  public Set keySet() {
    return new KeySetWrapper(timeExpiryDataStore.keySet());
  }

  public void putAll(Map map) {
    for (Iterator i = map.entrySet().iterator(); i.hasNext();) {
      Map.Entry entry = (Map.Entry) i.next();
      timeExpiryDataStore.put(entry.getKey(), entry.getValue());
    }
  }

  public Object remove(Object key) {
    return timeExpiryDataStore.remove(key);
  }

  public int size() {
    return timeExpiryDataStore.size();
  }

  public Collection values() {
    return new ValuesCollectionWrapper(timeExpiryDataStore.values());
  }

  public int getHitCount() {
    return timeExpiryDataStore.getHitCount();
  }

  public int getMissCountExpired() {
    return timeExpiryDataStore.getMissCountExpired();
  }

  public int getMissCountNotFound() {
    return timeExpiryDataStore.getMissCountNotFound();
  }

  public boolean isExpired(final Object key) {
    return timeExpiryDataStore.isExpired(key);
  }

  public final void stopTimeMonitoring() {
    timeExpiryDataStore.stopInvalidatorThread();
  }

  public void clearStatistics() {
    timeExpiryDataStore.clearStatistics();
  }

  private class EntrySetWrapper extends AbstractSet {

    private final Set entries;

    public EntrySetWrapper(Set entries) {
      this.entries = entries;
    }

    public void clear() {
      TimeExpiryMap.this.clear();
      entries.clear();
    }

    public boolean contains(Object o) {
      return entries.contains(o);
    }

    public Iterator iterator() {
      return new EntriesIterator(entries.iterator());
    }

    public boolean remove(Object o) {
      Object key = ((Map.Entry) o).getKey();
      TimeExpiryMap.this.remove(key);
      return entries.remove(key);
    }

    public int size() {
      return entries.size();
    }
  }

  private class ValuesCollectionWrapper extends AbstractCollection {

    private final Collection values;

    public ValuesCollectionWrapper(Collection values) {
      this.values = values;
    }

    public void clear() {
      TimeExpiryMap.this.clear();
      values.clear();
    }

    public boolean contains(Object o) {
      if (!(o instanceof CacheData)) {
        o = new CacheData(o, timeExpiryDataStore.getMaxIdleTimeoutSeconds(), timeExpiryDataStore.getMaxTTLSeconds());
      }
      return values.contains(o);
    }

    public Iterator iterator() {
      return new ValuesIterator(nativeEntrySet().iterator());
    }

    public int size() {
      return values.size();
    }
  }

  private class ValuesIterator extends EntriesIterator {

    public ValuesIterator(Iterator iterator) {
      super(iterator);
    }

    public Object next() {
      Map.Entry e = (Map.Entry) super.next();
      return e.getValue();
    }
  }

  private class EntryWrapper implements Map.Entry {

    private final Map.Entry entry;

    public EntryWrapper(Map.Entry entry) {
      this.entry = entry;
    }

    public Object getKey() {
      return entry.getKey();
    }

    public Object getValue() {
      Object rv = entry.getValue();
      Assert.pre(rv instanceof CacheData);
      return ((CacheData) rv).getValue();
    }

    public Object setValue(Object value) {
      if (!(value instanceof CacheData)) {
        value = new CacheData(value, timeExpiryDataStore.getMaxIdleTimeoutSeconds(), timeExpiryDataStore
            .getMaxTTLSeconds());
      }
      CacheData cd = (CacheData) entry.setValue(value);
      return cd.getValue();
    }

    public boolean equals(Object o) {
      if (!(o instanceof Map.Entry)) { return false; }

      Map.Entry e = (Map.Entry) o;
      return getKey().equals(e.getKey()) && getValue().equals(e.getValue());
    }

    public int hashCode() {
      return entry.hashCode();
    }
  }

  private class EntriesIterator implements Iterator {

    private final Iterator iterator;
    private Map.Entry      currentEntry;

    public EntriesIterator(Iterator iterator) {
      this.iterator = iterator;
    }

    public boolean hasNext() {
      return iterator.hasNext();
    }

    public Object next() {
      currentEntry = nextEntry();
      return new EntryWrapper(currentEntry);
    }

    protected Map.Entry nextEntry() {
      return (Map.Entry) iterator.next();
    }

    public void remove() {
      TimeExpiryMap.this.remove(currentEntry.getKey());
      iterator.remove();
    }
  }

  private class KeySetWrapper extends AbstractSet {

    private final Set keySet;

    public KeySetWrapper(Set keySet) {
      this.keySet = keySet;
    }

    public void clear() {
      TimeExpiryMap.this.clear();
      keySet.clear();
    }

    public boolean contains(Object o) {
      return keySet.contains(o);
    }

    public Iterator iterator() {
      return new KeysIterator(nativeEntrySet().iterator());
    }

    public boolean remove(Object o) {
      TimeExpiryMap.this.remove(o);
      return keySet.remove(o);
    }

    public int size() {
      return keySet.size();
    }
  }
  
  private class KeysIterator extends EntriesIterator {

    public KeysIterator(Iterator iterator) {
      super(iterator);
    }

    public Object next() {
      Map.Entry e = (Map.Entry) super.next();
      return e.getKey();
    }
  }

}

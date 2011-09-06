/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.persistence.db;

import com.tc.object.ObjectID;
import com.tc.objectserver.storage.api.PersistenceTransaction;
import com.tc.objectserver.storage.api.TCMapsDatabase;
import com.tc.util.Assert;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

class TCPersistableMap implements Map, PersistableCollection {

  // This is a carefully selected ObjectID that will never be assigned to any object.
  // TODO:: Move this Object ID to ObjectID class
  static final ObjectID REMOVED = new ObjectID(-2);

  /*
   * This map contains the mappings already in the database
   */
  private final Map     map;

  /*
   * This map contains the newly added mappings or removed mapping that are not in the database yet
   */
  private final Map     delta;

  private final long    id;

  private int           size    = 0;
  private boolean       clear   = false;

  /**
   * This constructor is used when in permanent store mode
   */
  TCPersistableMap(final ObjectID id, final Map backingMap) {
    this(id, backingMap, new HashMap(0));
  }

  /**
   * This constructor is used when in temporary swap (useful when using off-heap to store both delta and map entries off
   * heap).
   */
  TCPersistableMap(final ObjectID id, final Map backingMap, final Map deltaMap) {
    this.map = backingMap;
    this.delta = deltaMap;
    this.id = id.toLong();
    this.size = backingMap.size();
    if (deltaMap.size() != 0) { throw new AssertionError("Delta Map is not empty ! : size " + deltaMap.size()); }
  }

  public int size() {
    return size;
  }

  public boolean isEmpty() {
    return size() == 0;
  }

  public boolean containsKey(final Object key) {
    // NOTE:: map can't have mapping to null, it is always mapped to ObjectID.NULL_ID
    final Object value = this.delta.get(key);
    if (REMOVED.equals(value)) {
      return false;
    } else if (value != null) {
      return true;
    } else {
      return this.map.containsKey(key);
    }
  }

  public boolean containsValue(final Object value) {
    return this.delta.containsValue(value) || this.map.containsValue(value);
  }

  public Object get(final Object key) {
    final Object value = this.delta.get(key);
    if (REMOVED.equals(value)) {
      return null;
    } else if (value != null) {
      return value;
    } else {
      return this.map.get(key);
    }
  }

  public Object put(final Object key, final Object value) {
    Object old = this.delta.put(key, value);
    if (REMOVED.equals(old)) {
      this.size++;
      return null;
    } else if (old != null) {
      return old;
    } else {
      old = this.map.get(key);
      if (old == null) {
        size++;
      }
      return old;
    }
  }

  public Object remove(final Object key) {
    final Object oldInMap = map.get(key);
    if (oldInMap != null) {
      // Entry in map
      final Object old = this.delta.put(key, REMOVED);
      if (REMOVED.equals(old)) {
        // Already removed
        return null;
      } else if (old != null) {
        this.size--;
        return old;
      } else {
        this.size--;
        return oldInMap;
      }
    } else {
      // Entry not in map
      final Object old = this.delta.remove(key);
      if (REMOVED.equals(old)) {
        throw new AssertionError("Map doesn't contain : " + key + " but delta has REMOVED ");
      } else if (old != null) {
        this.size--;
        return old;
      } else {
        return null;
      }
    }
  }

  public void putAll(final Map m) {
    for (final Iterator i = m.entrySet().iterator(); i.hasNext();) {
      final Map.Entry entry = (Map.Entry) i.next();
      put(entry.getKey(), entry.getValue());
    }
  }

  public void clear() {
    this.clear = true;
    this.delta.clear();
    this.map.clear();
    this.size = 0;
  }

  public Set keySet() {
    return new KeyView();
  }

  public Collection values() {
    return new ValuesView();
  }

  public Set entrySet() {
    return new EntryView();
  }

  public int commit(final TCCollectionsSerializer serializer, final PersistenceTransaction tx, final TCMapsDatabase db)
      throws IOException, TCDatabaseException {

    int written = 0;
    // Clear the map first if necessary
    if (this.clear) {
      // map is already cleared, just clear from the DB
      db.deleteCollection(this.id, tx);
      this.clear = false;
    }

    // Apply delta changes to the DB and the backing map
    if (this.delta.size() > 0) {
      for (final Iterator i = this.delta.entrySet().iterator(); i.hasNext();) {
        final Map.Entry e = (Entry) i.next();
        final Object key = e.getKey();
        final Object value = e.getValue();
        if (REMOVED.equals(value)) {
          written += db.delete(tx, this.id, key, serializer);
          this.map.remove(key);
        } else {
          Object old = this.map.put(key, value);
          if (old == null) {
            written += db.insert(tx, this.id, key, value, serializer);
          } else {
            written += db.update(tx, this.id, key, value, serializer);
          }
        }
      }
      this.delta.clear();
      if (this.size != map.size()) { throw new AssertionError("Size ( " + size + " ) is not equal to map.size ( "
                                                              + map.size() + " )"); }
    }
    return written;
  }

  @Override
  public boolean equals(final Object other) {
    if (!(other instanceof Map)) { return false; }
    final Map that = (Map) other;
    if (that.size() != this.size()) { return false; }
    return entrySet().containsAll(that.entrySet());
  }

  @Override
  public int hashCode() {
    int h = 0;
    for (final Iterator i = entrySet().iterator(); i.hasNext();) {
      h += i.next().hashCode();
    }
    return h;
  }

  @Override
  public String toString() {
    return "TCPersistableMap(" + this.id + ")={ Map.size() = " + this.map.size() + ", delta.size() = "
           + this.delta.size() + ", size = " + this.size + " }";
  }

  public void load(final TCCollectionsSerializer serializer, final PersistenceTransaction tx, final TCMapsDatabase db)
      throws TCDatabaseException {
    Assert.assertTrue(this.map.isEmpty());
    db.loadMap(tx, this.id, this.map, serializer);
    this.size = map.size();
  }

  private abstract class BaseView implements Set {

    public int size() {
      return TCPersistableMap.this.size();
    }

    public boolean isEmpty() {
      return TCPersistableMap.this.isEmpty();
    }

    public Object[] toArray() {
      final Object[] result = new Object[size()];
      final Iterator e = iterator();
      for (int i = 0; e.hasNext(); i++) {
        result[i] = e.next();
      }
      return result;
    }

    public Object[] toArray(Object[] a) {
      if (a.length < size) {
        a = (Object[]) java.lang.reflect.Array.newInstance(a.getClass().getComponentType(), size);
      }

      final Iterator it = iterator();
      for (int i = 0; i < size; i++) {
        a[i] = it.next();
      }

      if (a.length > size) {
        a[size] = null;
      }

      return a;
    }

    public boolean add(final Object arg0) {
      throw new UnsupportedOperationException();
    }

    public boolean remove(final Object o) {
      throw new UnsupportedOperationException();
    }

    public boolean containsAll(final Collection collection) {
      for (final Iterator i = collection.iterator(); i.hasNext();) {
        if (!contains(i.next())) { return false; }
      }
      return true;
    }

    public boolean addAll(final Collection arg0) {
      throw new UnsupportedOperationException();
    }

    public boolean retainAll(final Collection arg0) {
      throw new UnsupportedOperationException();
    }

    public boolean removeAll(final Collection arg0) {
      throw new UnsupportedOperationException();
    }

    public void clear() {
      throw new UnsupportedOperationException();
    }
  }

  private class KeyView extends BaseView {

    public boolean contains(final Object key) {
      return TCPersistableMap.this.containsKey(key);
    }

    public Iterator iterator() {
      return new KeyIterator();
    }
  }

  private class ValuesView extends BaseView {

    public boolean contains(final Object value) {
      return TCPersistableMap.this.containsValue(value);
    }

    public Iterator iterator() {
      return new ValuesIterator();
    }
  }

  private class EntryView extends BaseView {

    public boolean contains(final Object o) {
      final Map.Entry entry = (Entry) o;
      final Object val = get(entry.getKey());
      final Object entryValue = entry.getValue();
      return entryValue == val || (null != val && val.equals(entryValue));
    }

    public Iterator iterator() {
      return new EntryIterator();
    }
  }

  private abstract class BaseIterator implements Iterator {

    boolean   isDelta = false;
    Iterator  current = TCPersistableMap.this.map.entrySet().iterator();
    Map.Entry next;

    BaseIterator() {
      moveToNext();
    }

    private void moveToNext() {
      while (this.current.hasNext()) {
        this.next = (Entry) this.current.next();
        if ((!this.isDelta && !delta.containsKey(this.next.getKey()))
            || (this.isDelta && !REMOVED.equals(this.next.getValue()))) { return; }
      }
      if (this.isDelta) {
        this.next = null;
      } else {
        this.current = TCPersistableMap.this.delta.entrySet().iterator();
        this.isDelta = true;
        moveToNext();
      }
    }

    public boolean hasNext() {
      return (this.next != null);
    }

    public Object next() {
      final Object key = getNext();
      moveToNext();
      return key;
    }

    public void remove() {
      throw new UnsupportedOperationException();
    }

    protected abstract Object getNext();

  }

  private class KeyIterator extends BaseIterator {
    @Override
    protected Object getNext() {
      return this.next.getKey();
    }
  }

  private class ValuesIterator extends BaseIterator {
    @Override
    protected Object getNext() {
      return this.next.getValue();
    }
  }

  private class EntryIterator extends BaseIterator {
    @Override
    protected Object getNext() {
      return this.next;
    }
  }
}

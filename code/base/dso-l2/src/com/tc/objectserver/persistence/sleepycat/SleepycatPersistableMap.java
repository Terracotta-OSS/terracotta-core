/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.persistence.sleepycat;

import com.sleepycat.je.Cursor;
import com.sleepycat.je.CursorConfig;
import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.LockMode;
import com.sleepycat.je.OperationStatus;
import com.tc.object.ObjectID;
import com.tc.objectserver.persistence.api.PersistenceTransaction;
import com.tc.util.Conversion;

import gnu.trove.THashMap;

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class SleepycatPersistableMap implements Map {

  private static final Object   REMOVED     = new Object();

  /*
   * This map contains the mappings already in the database
   */
  private final Map             map         = new THashMap(0);

  /*
   * This map contains the newly added mappings that are not in the database yet
   */
  private final Map             delta       = new THashMap(0);

  private final long            id;
  private int                   removeCount = 0;
  private boolean               clear       = false;

  public SleepycatPersistableMap(ObjectID id) {
    this.id = id.toLong();
  }

  public int size() {
    return map.size() + delta.size() - removeCount;
  }

  public boolean isEmpty() {
    return size() == 0;
  }

  public boolean containsKey(Object key) {
    Object value;
    // NOTE:: map cant have mapping to null, it is always mapped to ObjectID.NULL_ID
    return delta.containsKey(key) || ((value = map.get(key)) != null && value != REMOVED);
  }

  public boolean containsValue(Object value) {
    return delta.containsValue(value) || map.containsValue(value);
  }

  public Object get(Object key) {
    Object value = delta.get(key);
    if (value == null) {
      value = map.get(key);
      if (value == REMOVED) value = null;
    }
    return value;
  }

  public Object put(Object key, Object value) {
    Object returnVal = delta.put(key, value);
    if (returnVal != null) { return returnVal; }
    if (map.containsKey(key)) {
      returnVal = map.put(key, REMOVED);
      if (returnVal == REMOVED) { return null; }
      removeCount++;
    }
    return returnVal;
  }

  public Object remove(Object key) {
    Object returnVal = delta.remove(key);
    if (returnVal != null) { return returnVal; }
    if (map.containsKey(key)) {
      returnVal = map.put(key, REMOVED);
      if (returnVal == REMOVED) { return null; }
      removeCount++;
    }
    return returnVal;
  }

  public void putAll(Map m) {
    for (Iterator i = m.entrySet().iterator(); i.hasNext();) {
      Map.Entry entry = (Map.Entry) i.next();
      put(entry.getKey(), entry.getValue());
    }
  }

  public void clear() {
    clear = true;
    // XXX:: May be saving the keys to remove will be faster as sleepycat has to read/fault all records on clear. But
    // then it is memory to performance trade off.
    delta.clear();
    map.clear();
    removeCount = 0;
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

  public void commit(SleepycatCollectionsPersistor persistor, PersistenceTransaction tx, Database db)
      throws IOException, DatabaseException {
    // long t1 = System.currentTimeMillis();
    // StringBuffer sb = new StringBuffer("Time to commit : ");

    // First :: clear the map if necessary
    if (clear) {
      basicClear(persistor, tx, db);
      clear = false;
      // sb.append(" clear = ").append((System.currentTimeMillis() - t1)).append(" ms : ");
      // t1 = System.currentTimeMillis();
    }

    // Second :: put new or changed objects
    if (delta.size() > 0) {
      basicPut(persistor, tx, db);
      // sb.append(" put(").append(delta.size()).append(") = ").append((System.currentTimeMillis() - t1)).append(" ms :
      // ");
      // t1 = System.currentTimeMillis();
      delta.clear();
    }

    // Third :: remove old mappings :: This is slightly inefficient for huge maps. Keeping track of removed records is
    // again a trade off between memory and performance
    if (removeCount > 0) {
      basicRemove(persistor, tx, db);
      // sb.append(" remove(").append(removeCount).append(") = ").append((System.currentTimeMillis() - t1))
      // .append(" ms : ");
      removeCount = 0;
    }
    // flakyLogger(sb.toString(), t1);
  }

  private void basicRemove(SleepycatCollectionsPersistor persistor, PersistenceTransaction tx, Database db)
      throws IOException, DatabaseException {
    for (Iterator i = map.entrySet().iterator(); i.hasNext();) {
      Map.Entry e = (Entry) i.next();
      Object k = e.getKey();
      Object v = e.getValue();
      if (v == REMOVED) {
        DatabaseEntry key = new DatabaseEntry();
        key.setData(persistor.serialize(id, k));
        OperationStatus status = db.delete(persistor.pt2nt(tx), key);
        if (!(OperationStatus.NOTFOUND.equals(status) || OperationStatus.SUCCESS.equals(status))) {
          // make the formatter happy
          throw new DBException("Unable to remove Map Entry for object id: " + id + ", status: " + status + ", key: "
                                + key);
        }
        i.remove();
      }
    }

  }

  private void basicPut(SleepycatCollectionsPersistor persistor, PersistenceTransaction tx, Database db)
      throws IOException, DatabaseException {
    for (Iterator i = delta.entrySet().iterator(); i.hasNext();) {
      Map.Entry e = (Entry) i.next();
      Object k = e.getKey();
      Object v = e.getValue();
      DatabaseEntry key = new DatabaseEntry();
      key.setData(persistor.serialize(id, k));
      DatabaseEntry value = new DatabaseEntry();
      value.setData(persistor.serialize(v));
      OperationStatus status = db.put(persistor.pt2nt(tx), key, value);
      if (!OperationStatus.SUCCESS.equals(status)) { throw new DBException("Unable to update Map table : " + id
                                                                           + " status : " + status); }
      map.put(k, v);
    }
  }

  private void basicClear(SleepycatCollectionsPersistor persistor, PersistenceTransaction tx, Database db)
      throws DatabaseException {
    // XXX::Sleepycat has the most inefficent way to delete objects. Another way would be to delete all records
    // explicitly.
    // XXX:: Since we read in one direction and since we have to read the first record of the next map to break out, we
    // need READ_COMMITTED to avoid deadlocks between commit thread and GC thread.
    Cursor c = db.openCursor(persistor.pt2nt(tx), CursorConfig.READ_COMMITTED);
    byte idb[] = Conversion.long2Bytes(id);
    DatabaseEntry key = new DatabaseEntry();
    key.setData(idb);
    DatabaseEntry value = new DatabaseEntry();
    value.setPartial(0, 0, true);
    if (c.getSearchKeyRange(key, value, LockMode.DEFAULT) == OperationStatus.SUCCESS) {
      do {
        if (partialMatch(idb, key.getData())) {
          c.delete();
        } else {
          break;
        }
      } while (c.getNext(key, value, LockMode.DEFAULT) == OperationStatus.SUCCESS);
    }
    c.close();
  }

  // long lastlog;
  // private void flakyLogger(String message, long start, long end) {
  // if (lastlog + 1000 < end) {
  // lastlog = end;
  // System.err.println(this + " : " + message + " " + (end - start) + " ms");
  // }
  // }
  //
  // private void flakyLogger(String message, long recent) {
  // if (lastlog + 1000 < recent) {
  // System.err.println(this + " : " + message);
  // }
  // }

  private boolean partialMatch(byte[] idbytes, byte[] key) {
    if (key.length < idbytes.length) return false;
    for (int i = 0; i < idbytes.length; i++) {
      if (idbytes[i] != key[i]) return false;
    }
    return true;
  }

  public boolean equals(Object other) {
    if (!(other instanceof Map)) { return false; }
    Map that = (Map) other;
    if (that.size() != this.size()) { return false; }
    return entrySet().containsAll(that.entrySet());
  }

  public int hashCode() {
    int h = 0;
    for (Iterator i = entrySet().iterator(); i.hasNext();) {
      h += i.next().hashCode();
    }
    return h;
  }

  public String toString() {
    return "SleepycatPersistableMap(" + id + ")={ Map.size() = " + map.size() + ", delta.size() = " + delta.size()
           + ", removeCount = " + removeCount + " }";
  }

  public void load(SleepycatCollectionsPersistor persistor, PersistenceTransaction tx, Database db) throws IOException,
      ClassNotFoundException, DatabaseException {
    // XXX:: Since we read in one direction and since we have to read the first record of the next map to break out, we
    // need READ_COMMITTED to avoid deadlocks between commit thread and GC thread.
    Cursor c = db.openCursor(persistor.pt2nt(tx), CursorConfig.READ_COMMITTED);
    byte idb[] = Conversion.long2Bytes(id);
    DatabaseEntry key = new DatabaseEntry();
    key.setData(idb);
    DatabaseEntry value = new DatabaseEntry();
    if (c.getSearchKeyRange(key, value, LockMode.DEFAULT) == OperationStatus.SUCCESS) {
      do {
        if (false) System.err.println("MapDB " + toString(key) + " , " + toString(value));
        if (partialMatch(idb, key.getData())) {
          Object mkey = persistor.deserialize(idb.length, key.getData());
          Object mvalue = persistor.deserialize(value.getData());
          map.put(mkey, mvalue);
          // System.err.println("map.put() = " + mkey + " , " + mvalue);
        } else {
          break;
        }
      } while (c.getNext(key, value, LockMode.DEFAULT) == OperationStatus.SUCCESS);
    }
    c.close();
  }

  private String toString(DatabaseEntry entry) {
    StringBuffer sb = new StringBuffer();
    sb.append("<DatabaseEntry ");
    byte b[] = entry.getData();
    if (b == null) {
      sb.append(" NULL Data>");
    } else if (b.length == 0) {
      sb.append(" ZERO bytes>");
    } else {
      for (int i = 0; i < b.length; i++) {
        sb.append(b[i]).append(' ');
      }
      sb.append(">");
    }
    return sb.toString();
  }

  private abstract class BaseView implements Set {

    public int size() {
      return SleepycatPersistableMap.this.size();
    }

    public boolean isEmpty() {
      return SleepycatPersistableMap.this.isEmpty();
    }

    public Object[] toArray() {
      Object[] result = new Object[size()];
      Iterator e = iterator();
      for (int i = 0; e.hasNext(); i++)
        result[i] = e.next();
      return result;
    }

    public Object[] toArray(Object[] a) {
      int size = size();
      if (a.length < size) a = (Object[]) java.lang.reflect.Array.newInstance(a.getClass().getComponentType(), size);

      Iterator it = iterator();
      for (int i = 0; i < size; i++) {
        a[i] = it.next();
      }

      if (a.length > size) {
        a[size] = null;
      }

      return a;
    }

    public boolean add(Object arg0) {
      throw new UnsupportedOperationException();
    }

    public boolean remove(Object o) {
      throw new UnsupportedOperationException();
    }

    public boolean containsAll(Collection collection) {
      for (Iterator i = collection.iterator(); i.hasNext();) {
        if (!contains(i.next())) { return false; }
      }
      return true;
    }

    public boolean addAll(Collection arg0) {
      throw new UnsupportedOperationException();
    }

    public boolean retainAll(Collection arg0) {
      throw new UnsupportedOperationException();
    }

    public boolean removeAll(Collection arg0) {
      throw new UnsupportedOperationException();
    }

    public void clear() {
      throw new UnsupportedOperationException();
    }
  }

  private class KeyView extends BaseView {

    public boolean contains(Object key) {
      return SleepycatPersistableMap.this.containsKey(key);
    }

    public Iterator iterator() {
      return new KeyIterator();
    }
  }

  private class ValuesView extends BaseView {

    public boolean contains(Object value) {
      return SleepycatPersistableMap.this.containsValue(value);
    }

    public Iterator iterator() {
      return new ValuesIterator();
    }
  }

  private class EntryView extends BaseView {

    public boolean contains(Object o) {
      Map.Entry entry = (Entry) o;
      Object val = get(entry.getKey());
      Object entryValue = entry.getValue();
      return entryValue == val || (null != val && val.equals(entryValue));
    }

    public Iterator iterator() {
      return new EntryIterator();
    }
  }

  private abstract class BaseIterator implements Iterator {

    boolean   isDelta = false;
    Iterator  current = map.entrySet().iterator();
    Map.Entry next;

    BaseIterator() {
      moveToNext();
    }

    private void moveToNext() {
      while (current.hasNext()) {
        next = (Entry) current.next();
        if (next.getValue() != REMOVED) { return; }
      }
      if (isDelta) {
        next = null;
      } else {
        current = delta.entrySet().iterator();
        isDelta = true;
        moveToNext();
      }
    }

    public boolean hasNext() {
      return (next != null);
    }

    public Object next() {
      Object key = getNext();
      moveToNext();
      return key;
    }

    public void remove() {
      throw new UnsupportedOperationException();
    }

    protected abstract Object getNext();

  }

  private class KeyIterator extends BaseIterator {
    protected Object getNext() {
      return next.getKey();
    }
  }

  private class ValuesIterator extends BaseIterator {
    protected Object getNext() {
      return next.getValue();
    }
  }

  private class EntryIterator extends BaseIterator {
    protected Object getNext() {
      return next;
    }
  }
}

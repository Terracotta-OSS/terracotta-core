/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.persistence.db;

import com.tc.object.ObjectID;
import com.tc.objectserver.storage.api.PersistenceTransaction;
import com.tc.objectserver.storage.api.TCDatabaseEntry;
import com.tc.objectserver.storage.api.TCMapsDatabase;
import com.tc.objectserver.storage.api.TCMapsDatabaseCursor;
import com.tc.objectserver.storage.api.TCDatabaseReturnConstants.Status;
import com.tc.util.Conversion;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class TCPersistableMap implements Map, PersistableCollection {

  private static final Object REMOVED     = new Object();

  /*
   * This map contains the mappings already in the database
   */
  private final Map           map         = new HashMap(0);

  /*
   * This map contains the newly added mappings that are not in the database yet
   */
  private final Map           delta       = new HashMap(0);

  private final long          id;
  private int                 removeCount = 0;
  private boolean             clear       = false;

  public TCPersistableMap(ObjectID id) {
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

  public int commit(TCCollectionsPersistor persistor, PersistenceTransaction tx, TCMapsDatabase db)
      throws IOException, TCDatabaseException {
    // long t1 = System.currentTimeMillis();
    // StringBuffer sb = new StringBuffer("Time to commit : ");

    int written = 0;
    // First :: clear the map if necessary
    if (clear) {
      written += basicClear(persistor, tx, db);
      clear = false;
      // sb.append(" clear = ").append((System.currentTimeMillis() - t1)).append(" ms : ");
      // t1 = System.currentTimeMillis();
    }

    // Second :: put new or changed objects
    if (delta.size() > 0) {
      written += basicPut(persistor, tx, db);
      // sb.append(" put(").append(delta.size()).append(") = ").append((System.currentTimeMillis() - t1)).append(" ms :
      // ");
      // t1 = System.currentTimeMillis();
      delta.clear();
    }

    // Third :: remove old mappings :: This is slightly inefficient for huge maps. Keeping track of removed records is
    // again a trade off between memory and performance
    if (removeCount > 0) {
      written += basicRemove(persistor, tx, db);
      // sb.append(" remove(").append(removeCount).append(") = ").append((System.currentTimeMillis() - t1))
      // .append(" ms : ");
      removeCount = 0;
    }
    // flakyLogger(sb.toString(), t1);
    return written;
  }

  private int basicRemove(TCCollectionsPersistor persistor, PersistenceTransaction tx, TCMapsDatabase db)
      throws IOException, TCDatabaseException {
    int written = 0;
    for (Iterator i = map.entrySet().iterator(); i.hasNext();) {
      Map.Entry e = (Entry) i.next();
      Object k = e.getKey();
      Object v = e.getValue();
      if (v == REMOVED) {
        byte[] key = persistor.serialize(id, k);
        written += key.length;
        try {
          boolean status = db.delete(id, key, tx) == Status.SUCCESS;
          if (!status) {
            // make the formatter happy
            throw new DBException("Unable to remove Map Entry for object id: " + id + ", status: " + status + ", key: "
                                  + k);
          }
        } catch (Exception t) {
          throw new TCDatabaseException(t.getMessage());
        }
        i.remove();
      }
    }
    return written;

  }

  private int basicPut(TCCollectionsPersistor persistor, PersistenceTransaction tx, TCMapsDatabase db)
      throws IOException, TCDatabaseException {
    int written = 0;
    for (Iterator i = delta.entrySet().iterator(); i.hasNext();) {
      Map.Entry e = (Entry) i.next();
      Object k = e.getKey();
      Object v = e.getValue();
      byte[] key = persistor.serialize(id, k);
      byte[] value = persistor.serialize(v);
      written += value.length;
      written += key.length;
      try {
        boolean status = db.put(id, key, value, tx) == Status.SUCCESS;
        if (!status) { throw new DBException("Unable to update Map table : " + id + " status : " + status); }
      } catch (Exception t) {
        throw new TCDatabaseException(t.getMessage());
      }
      map.put(k, v);
    }
    return written;
  }

  private int basicClear(TCCollectionsPersistor persistor, PersistenceTransaction tx, TCMapsDatabase db)
      throws TCDatabaseException {
    // XXX::Sleepycat has the most inefficent way to delete objects. Another way would be to delete all records
    // explicitly.
    // These are the possible ways for isolation
    // CursorConfig.DEFAULT : Default configuration used if null is passed to methods that create a cursor.
    // CursorConfig.READ_COMMITTED : This ensures the stability of the current data item read by the cursor but permits
    // data read by this cursor to be modified or deleted prior to the commit of the transaction.
    // CursorConfig.READ_UNCOMMITTED : A convenience instance to configure read operations performed by the cursor to
    // return modified but not yet committed data.
    // During our testing we found that READ_UNCOMMITTED does not raise any problem and gives a performance enhancement
    // over READ_COMMITTED. Since we never read the map which has been marked for deletion by the DGC the deadlocks are
    // avoided
    return db.deleteCollection(id, tx);
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

  @Override
  public boolean equals(Object other) {
    if (!(other instanceof Map)) { return false; }
    Map that = (Map) other;
    if (that.size() != this.size()) { return false; }
    return entrySet().containsAll(that.entrySet());
  }

  @Override
  public int hashCode() {
    int h = 0;
    for (Iterator i = entrySet().iterator(); i.hasNext();) {
      h += i.next().hashCode();
    }
    return h;
  }

  @Override
  public String toString() {
    return "SleepycatPersistableMap(" + id + ")={ Map.size() = " + map.size() + ", delta.size() = " + delta.size()
           + ", removeCount = " + removeCount + " }";
  }

  public void load(TCCollectionsPersistor persistor, PersistenceTransaction tx, TCMapsDatabase db)
      throws TCDatabaseException {
    // XXX:: Since we read in one direction and since we have to read the first record of the next map to break out, we
    // need READ_COMMITTED to avoid deadlocks between commit thread and DGC thread.
    byte idb[] = Conversion.long2Bytes(id);
    TCMapsDatabaseCursor c = db.openCursor(tx, id);
    try {
      while (c.hasNext()) {
        TCDatabaseEntry<byte[], byte[]> entry = c.next();
        Object mkey = persistor.deserialize(idb.length, entry.getKey());
        Object mvalue = persistor.deserialize(entry.getValue());
        map.put(mkey, mvalue);
      }
      c.close();
    } catch (Exception t) {
      throw new TCDatabaseException(t.getMessage());
    }
  }

  private abstract class BaseView implements Set {

    public int size() {
      return TCPersistableMap.this.size();
    }

    public boolean isEmpty() {
      return TCPersistableMap.this.isEmpty();
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
      return TCPersistableMap.this.containsKey(key);
    }

    public Iterator iterator() {
      return new KeyIterator();
    }
  }

  private class ValuesView extends BaseView {

    public boolean contains(Object value) {
      return TCPersistableMap.this.containsValue(value);
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
    @Override
    protected Object getNext() {
      return next.getKey();
    }
  }

  private class ValuesIterator extends BaseIterator {
    @Override
    protected Object getNext() {
      return next.getValue();
    }
  }

  private class EntryIterator extends BaseIterator {
    @Override
    protected Object getNext() {
      return next;
    }
  }
}

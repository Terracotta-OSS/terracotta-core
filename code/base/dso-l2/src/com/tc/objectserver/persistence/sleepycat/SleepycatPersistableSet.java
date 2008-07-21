/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.persistence.sleepycat;

import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseException;
import com.tc.object.ObjectID;
import com.tc.objectserver.persistence.api.PersistenceTransaction;

import java.io.IOException;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

public class SleepycatPersistableSet extends AbstractSet implements PersistableCollection {

  private final SleepycatPersistableMap map;
  private static final Boolean          VALUE = true;

  public SleepycatPersistableSet(ObjectID id) {
    map = new SleepycatPersistableMap(id);
  }

  public boolean add(Object obj) {
    return map.put(obj, VALUE) == null;
  }

  public void clear() {
    map.clear();
  }

  public boolean contains(Object obj) {
    return map.containsKey(obj);
  }

  public boolean isEmpty() {
    return map.isEmpty();
  }

  public boolean retainAll(Collection c) {
    int initialSize = size();
    ArrayList list = new ArrayList();
    for (Iterator iter = iterator(); iter.hasNext();) {
      Object next = iter.next();
      if (!c.contains(next)) list.add(next);
    }
    removeAll(list);

    return size() != initialSize;
  }

  public Iterator iterator() {
    return map.keySet().iterator();
  }

  @Override
  public boolean removeAll(Collection c) {
    boolean modified = false;
    for (Iterator i = c.iterator(); i.hasNext();)
      modified |= remove(i.next());
    return modified;
  }

  public boolean remove(Object obj) {
    return map.remove(obj) != null;
  }

  public int size() {
    return map.size();
  }

  public Object[] toArray() {
    return map.keySet().toArray();
  }

  public Object[] toArray(Object[] objArr) {
    return map.keySet().toArray(objArr);
  }

  public int commit(SleepycatCollectionsPersistor persistor, PersistenceTransaction tx, Database db)
      throws IOException, DatabaseException {
    return map.commit(persistor, tx, db);
  }

  public void load(SleepycatCollectionsPersistor persistor, PersistenceTransaction tx, Database db) throws IOException,
      ClassNotFoundException, DatabaseException {
    map.load(persistor, tx, db);
  }
}

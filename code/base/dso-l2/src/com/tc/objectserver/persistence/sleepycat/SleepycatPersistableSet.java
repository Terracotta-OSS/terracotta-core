/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.persistence.sleepycat;

import com.sleepycat.je.Database;
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

  @Override
  public boolean add(Object obj) {
    return map.put(obj, VALUE) == null;
  }

  @Override
  public void clear() {
    map.clear();
  }

  @Override
  public boolean contains(Object obj) {
    return map.containsKey(obj);
  }

  @Override
  public boolean isEmpty() {
    return map.isEmpty();
  }

  @Override
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

  @Override
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

  @Override
  public boolean remove(Object obj) {
    return map.remove(obj) != null;
  }

  @Override
  public int size() {
    return map.size();
  }

  @Override
  public Object[] toArray() {
    return map.keySet().toArray();
  }

  @Override
  public Object[] toArray(Object[] objArr) {
    return map.keySet().toArray(objArr);
  }

  public int commit(SleepycatCollectionsPersistor persistor, PersistenceTransaction tx, Database db)
      throws IOException, TCDatabaseException {
    return map.commit(persistor, tx, db);
  }

  public void load(SleepycatCollectionsPersistor persistor, PersistenceTransaction tx, Database db)
      throws TCDatabaseException {
    map.load(persistor, tx, db);
  }
}

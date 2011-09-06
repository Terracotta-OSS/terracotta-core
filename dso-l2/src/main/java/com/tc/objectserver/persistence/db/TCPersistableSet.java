/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.persistence.db;

import com.tc.object.ObjectID;
import com.tc.objectserver.storage.api.PersistenceTransaction;
import com.tc.objectserver.storage.api.TCMapsDatabase;

import java.io.IOException;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

class TCPersistableSet extends AbstractSet implements PersistableCollection {

  private final TCPersistableMap map;
  private static final Boolean   VALUE = true;

  TCPersistableSet(final ObjectID id, final Map backingMap) {
    this.map = new TCPersistableMap(id, backingMap);
  }

  TCPersistableSet(final ObjectID id, final Map backingMap, final Map deltaMap) {
    this.map = new TCPersistableMap(id, backingMap, deltaMap);
  }

  @Override
  public boolean add(final Object obj) {
    return this.map.put(obj, VALUE) == null;
  }

  @Override
  public void clear() {
    this.map.clear();
  }

  @Override
  public boolean contains(final Object obj) {
    return this.map.containsKey(obj);
  }

  @Override
  public boolean isEmpty() {
    return this.map.isEmpty();
  }

  @Override
  public boolean retainAll(final Collection c) {
    final int initialSize = size();
    final ArrayList list = new ArrayList();
    for (final Iterator iter = iterator(); iter.hasNext();) {
      final Object next = iter.next();
      if (!c.contains(next)) {
        list.add(next);
      }
    }
    removeAll(list);

    return size() != initialSize;
  }

  @Override
  public Iterator iterator() {
    return this.map.keySet().iterator();
  }

  @Override
  public boolean removeAll(final Collection c) {
    boolean modified = false;
    for (final Iterator i = c.iterator(); i.hasNext();) {
      modified |= remove(i.next());
    }
    return modified;
  }

  @Override
  public boolean remove(final Object obj) {
    return this.map.remove(obj) != null;
  }

  @Override
  public int size() {
    return this.map.size();
  }

  @Override
  public Object[] toArray() {
    return this.map.keySet().toArray();
  }

  @Override
  public Object[] toArray(final Object[] objArr) {
    return this.map.keySet().toArray(objArr);
  }

  public int commit(final TCCollectionsSerializer serializer, final PersistenceTransaction tx, final TCMapsDatabase db)
  throws IOException, TCDatabaseException {
    return this.map.commit(serializer, tx, db);
  }

  public void load(final TCCollectionsSerializer serializer, final PersistenceTransaction tx, final TCMapsDatabase db)
  throws TCDatabaseException {
    this.map.load(serializer, tx, db);
  }
}

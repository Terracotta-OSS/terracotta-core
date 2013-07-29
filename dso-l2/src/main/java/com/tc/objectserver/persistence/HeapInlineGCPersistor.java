package com.tc.objectserver.persistence;

import com.tc.object.ObjectID;
import com.tc.util.ObjectIDSet;

import java.util.Collection;
import java.util.Set;

/**
 * @author tim
 */
public class HeapInlineGCPersistor implements InlineGCPersistor {
  private final Set<ObjectID> set = new ObjectIDSet();

  @Override
  public synchronized int size() {
    return set.size();
  }

  @Override
  public synchronized void addObjectIDs(final Collection<ObjectID> oids) {
    set.addAll(oids);
  }

  @Override
  public synchronized void removeObjectIDs(final Collection<ObjectID> objectIDs) {
    set.removeAll(objectIDs);
  }

  @Override
  public synchronized Set<ObjectID> allObjectIDs() {
    return new ObjectIDSet(set);
  }
}

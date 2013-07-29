package com.tc.objectserver.persistence;

import com.tc.object.ObjectID;

import java.util.Collection;
import java.util.Set;

/**
 * @author tim
 */
public interface InlineGCPersistor {
  int size();

  void addObjectIDs(Collection<ObjectID> oids);

  void removeObjectIDs(Collection<ObjectID> objectIDs);

  Set<ObjectID> allObjectIDs();
}

/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.persistence.api;

import com.tc.object.ObjectID;
import com.tc.objectserver.api.ManagedObjectProvider;
import com.tc.objectserver.context.GCResultContext;
import com.tc.objectserver.core.api.ManagedObject;
import com.tc.objectserver.storage.api.PersistenceTransaction;
import com.tc.text.PrettyPrintable;
import com.tc.util.ObjectIDSet;
import com.tc.util.sequence.ObjectIDSequence;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;

public interface ManagedObjectStore extends ManagedObjectStoreStats, ManagedObjectProvider, ObjectIDSequence,
    PrettyPrintable {

  public void addNewObject(ManagedObject managed);

  public void commitObject(PersistenceTransaction tx, ManagedObject object);

  public void commitAllObjects(PersistenceTransaction tx, Collection collection);

  /**
   * synchronous
   */
  public void removeAllObjectsByIDNow(SortedSet<ObjectID> objectIds);

  /**
   * Returns a copy of the set of all object ids, hence a relatively costly operation
   */
  public ObjectIDSet getAllObjectIDs();

  public ObjectIDSet getAllEvictableObjectIDs();

  public ObjectIDSet getAllMapTypeObjectIDs();

  public boolean containsObject(ObjectID id);

  public ObjectID getRootID(String name);

  public Set getRoots();

  public Set getRootNames();

  public void addNewRoot(PersistenceTransaction tx, String rootName, ObjectID id);

  public void shutdown();

  public boolean inShutdown();

  public Map getRootNamesToIDsMap();

  /**
   * This method is used by the DGC to trigger removing Garbage.
   */
  public void removeAllObjectsByID(GCResultContext gcResult);
}

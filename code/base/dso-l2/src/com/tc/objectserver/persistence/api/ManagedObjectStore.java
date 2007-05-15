/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.objectserver.persistence.api;

import com.tc.object.ObjectID;
import com.tc.objectserver.api.ManagedObjectProvider;
import com.tc.objectserver.core.api.ManagedObject;
import com.tc.text.PrettyPrintable;
import com.tc.util.ObjectIDSet2;
import com.tc.util.sequence.ObjectIDSequence;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

public interface ManagedObjectStore extends ManagedObjectProvider, ObjectIDSequence, PrettyPrintable {

  public void addNewObject(ManagedObject managed);

  public void commitObject(PersistenceTransaction tx, ManagedObject object);

  public void commitAllObjects(PersistenceTransaction tx, Collection collection);

  /**
   * synchronous
   */
  public void removeAllObjectsByIDNow(PersistenceTransaction tx, Collection objectIds);

  /**
   * Returns the set of object ids.
   */
  public ObjectIDSet2 getAllObjectIDs();

  public boolean containsObject(ObjectID id);

  public ObjectID getRootID(String name);

  public Set getRoots();

  public Set getRootNames();

  public void addNewRoot(PersistenceTransaction tx, String rootName, ObjectID id);

  public void shutdown();

  public boolean inShutdown();

  public Map getRootNamesToIDsMap();

}

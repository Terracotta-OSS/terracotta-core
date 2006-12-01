/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object;

import com.tc.object.dna.api.DNA;
import com.tc.object.session.SessionID;

import java.util.Collection;

/**
 * @author steve local representation of the remote object manager
 */
public interface RemoteObjectManager {

  public DNA retrieve(ObjectID id);

  public DNA retrieve(ObjectID id, int depth);

  public ObjectID retrieveRootID(String name);

  public void addRoot(String name, ObjectID id);

  public void addAllObjects(SessionID sessionID, long batchID, Collection dnas);

  public void removed(ObjectID id);

  /**
   * Causes outstanding object and root requests to be re-sent.
   */
  public void requestOutstanding();

  public void pause();

  public void clear();

  public void starting();

  public void unpause();

}
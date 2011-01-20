/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object;

import com.tc.net.GroupID;
import com.tc.net.NodeID;
import com.tc.object.dna.api.DNA;
import com.tc.object.handshakemanager.ClientHandshakeCallback;
import com.tc.object.session.SessionID;
import com.tc.text.PrettyPrintable;

import java.util.Collection;
import java.util.Set;

/**
 * Local representation of the remote object manager
 */
public interface RemoteObjectManager extends ClientHandshakeCallback, PrettyPrintable {

  public DNA retrieve(ObjectID id);

  public DNA retrieve(ObjectID id, int depth);

  public DNA retrieveWithParentContext(ObjectID id, ObjectID parentContext);

  public ObjectID retrieveRootID(String name);

  public void addRoot(String name, ObjectID id, NodeID nodeID);

  public void addAllObjects(SessionID sessionID, long batchID, Collection dnas, NodeID nodeID);

  public void objectsNotFoundFor(SessionID sessionID, long batchID, Set missingObjectIDs, NodeID nodeID);

  public void removed(ObjectID id);

  public void clear(GroupID gid);

  public boolean isInDNACache(ObjectID id);

  public void preFetchObject(ObjectID id);

}
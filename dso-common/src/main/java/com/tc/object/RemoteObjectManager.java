/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
 */
package com.tc.object;

import com.tc.abortable.AbortedOperationException;
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

  public DNA retrieve(ObjectID id) throws AbortedOperationException;

  public DNA retrieve(ObjectID id, int depth) throws AbortedOperationException;

  public ObjectID retrieveRootID(String name, GroupID gid);

  public void addRoot(String name, ObjectID id, NodeID nodeID);

  public void addAllObjects(SessionID sessionID, long batchID, Collection dnas, NodeID nodeID);
  
  public void addObject(DNA dna);

  public void cleanOutObject(DNA dna);

  public void objectsNotFoundFor(SessionID sessionID, long batchID, Set missingObjectIDs, NodeID nodeID);

  public void removed(ObjectID id);

  public void clear(GroupID gid);

  public boolean isInDNACache(ObjectID id);

  public void preFetchObject(ObjectID id) throws AbortedOperationException;

}

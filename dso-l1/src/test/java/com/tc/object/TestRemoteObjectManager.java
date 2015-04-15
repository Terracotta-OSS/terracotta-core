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

import org.mockito.Mockito;

import com.tc.exception.ImplementMe;
import com.tc.exception.PlatformRejoinException;
import com.tc.exception.TCObjectNotFoundException;
import com.tc.net.GroupID;
import com.tc.net.NodeID;
import com.tc.object.dna.api.DNA;
import com.tc.object.msg.ClientHandshakeMessage;
import com.tc.object.session.SessionID;
import com.tc.text.PrettyPrinter;
import com.tc.util.BitSetObjectIDSet;
import com.tc.util.ObjectIDSet;
import com.tc.util.concurrent.NoExceptionLinkedQueue;

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;

public class TestRemoteObjectManager implements RemoteObjectManager {
  private final int               SIZE                  = 10000;

  /**
   * sine LinkedBlockingQueue is instrumented this is a workaround to get this test working TODO: change back to
   * {@link NoExceptionLinkedQueue} when instrumentation is removed
   */
  public final ArrayBlockingQueue retrieveCalls         = new ArrayBlockingQueue(SIZE);
  public final ArrayBlockingQueue retrieveResults       = new ArrayBlockingQueue(SIZE);

  public final ArrayBlockingQueue retrieveRootIDCalls   = new ArrayBlockingQueue(SIZE);
  public final ArrayBlockingQueue retrieveRootIDResults = new ArrayBlockingQueue(SIZE);

  public static final DNA         THROW_NOT_FOUND       = Mockito.mock(DNA.class);
  public final ObjectIDSet        removedObjects        = new BitSetObjectIDSet();
  public static final DNA         REJOIN_IN_PROGRESS    = Mockito.mock(DNA.class);

  @Override
  public void cleanup() {
    retrieveCalls.clear();
    retrieveResults.clear();
    retrieveResults.add(REJOIN_IN_PROGRESS);
    retrieveRootIDCalls.clear();
    retrieveRootIDResults.clear();
    removedObjects.clear();
  }

  @Override
  public DNA retrieve(final ObjectID id) {
    this.retrieveCalls.add(id);
    DNA dna;
    try {
      dna = (DNA) this.retrieveResults.take();
    } catch (InterruptedException e) {
      throw new AssertionError(e);
    }
    if (dna == THROW_NOT_FOUND) { throw new TCObjectNotFoundException("missing ID"); }
    if (dna == REJOIN_IN_PROGRESS) {
      // add for subsequent lookups..
      retrieveResults.add(REJOIN_IN_PROGRESS);
      throw new PlatformRejoinException("missing ID");
    }
    return dna;
  }

  @Override
  public ObjectID retrieveRootID(final String name, GroupID gid) {
    this.retrieveRootIDCalls.add(name);
    try {
      return (ObjectID) this.retrieveRootIDResults.take();
    } catch (InterruptedException e) {
      throw new AssertionError(e);
    }
  }

  @Override
  public void removed(final ObjectID id) {
    removedObjects.add(id);
  }

  @Override
  public DNA retrieve(final ObjectID id, final int depth) {
    throw new ImplementMe();
  }

  @Override
  public void addAllObjects(final SessionID sessionID, final long batchID, final Collection dnas, final NodeID nodeID) {
    throw new ImplementMe();
  }

  @Override
  public void addObject(DNA dna) {
    throw new ImplementMe();
  }

  @Override
  public void cleanOutObject(final DNA dna) {
    throw new UnsupportedOperationException("Implement me!");
  }

  @Override
  public void addRoot(final String name, final ObjectID id, final NodeID nodeID) {
    throw new ImplementMe();
  }

  @Override
  public void objectsNotFoundFor(final SessionID sessionID, final long batchID, final Set missingObjectIDs,
                                 final NodeID nodeID) {
    throw new ImplementMe();
  }

  @Override
  public void clear(GroupID gid) {
    throw new ImplementMe();
  }

  @Override
  public boolean isInDNACache(final ObjectID id) {
    throw new ImplementMe();
  }

  @Override
  public void initializeHandshake(final NodeID thisNode, final NodeID remoteNode,
                                  final ClientHandshakeMessage handshakeMessage) {
    retrieveResults.clear();
  }

  @Override
  public void pause(final NodeID remoteNode, final int disconnected) {
    throw new ImplementMe();

  }

  @Override
  public void unpause(final NodeID remoteNode, final int disconnected) {
    throw new ImplementMe();

  }

  @Override
  public void shutdown(boolean fromShutdownHook) {
    // NOP
  }

  @Override
  public void preFetchObject(final ObjectID id) {
    throw new ImplementMe();
  }

  public ObjectID getMappingForKey(final ObjectID oid, final Object portableKey) {
    throw new ImplementMe();
  }

  public void addResponseForKeyValueMapping(final SessionID localSessionID, final ObjectID mapID,
                                            final Object portableKey, final Object portableValue, final NodeID nodeID) {
    throw new ImplementMe();
  }

  @Override
  public PrettyPrinter prettyPrint(PrettyPrinter out) {
    throw new ImplementMe();
  }
}

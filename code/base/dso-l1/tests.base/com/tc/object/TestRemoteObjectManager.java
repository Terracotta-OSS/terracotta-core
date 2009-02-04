/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object;

import com.tc.exception.ImplementMe;
import com.tc.exception.TCObjectNotFoundException;
import com.tc.net.NodeID;
import com.tc.object.dna.api.DNA;
import com.tc.object.dna.api.DNACursor;
import com.tc.object.dna.api.DNAException;
import com.tc.object.session.SessionID;
import com.tc.util.concurrent.NoExceptionLinkedQueue;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

public class TestRemoteObjectManager implements RemoteObjectManager {

  public final NoExceptionLinkedQueue retrieveCalls         = new NoExceptionLinkedQueue();
  public final NoExceptionLinkedQueue retrieveResults       = new NoExceptionLinkedQueue();

  public final NoExceptionLinkedQueue retrieveRootIDCalls   = new NoExceptionLinkedQueue();
  public final NoExceptionLinkedQueue retrieveRootIDResults = new NoExceptionLinkedQueue();

  public static final DNA             THROW_NOT_FOUND       = new ThrowNotFound();

  public DNA retrieve(ObjectID id) {
    retrieveCalls.put(id);
    DNA dna = (DNA) retrieveResults.take();
    if (dna == THROW_NOT_FOUND) { throw new TCObjectNotFoundException("missing ID", Collections.EMPTY_LIST); }
    return dna;
  }

  public DNA retrieveWithParentContext(ObjectID id, ObjectID parentContext) {
    return retrieve(id);
  }

  public ObjectID retrieveRootID(String name) {
    retrieveRootIDCalls.put(name);
    return (ObjectID) retrieveRootIDResults.take();
  }

  public void removed(ObjectID id) {
    // do nothing
  }

  public DNA retrieve(ObjectID id, int depth) {
    throw new ImplementMe();
  }

  public void addAllObjects(SessionID sessionID, long batchID, Collection dnas, NodeID nodeID) {
    throw new ImplementMe();
  }

  public void addRoot(String name, ObjectID id, NodeID nodeID) {
    throw new ImplementMe();
  }

  public void objectsNotFoundFor(SessionID sessionID, long batchID, Set missingObjectIDs, NodeID nodeID) {
    throw new ImplementMe();
  }

  public static class ThrowNotFound implements DNA {

    private ThrowNotFound() {
      //
    }

    public int getArraySize() {
      throw new ImplementMe();
    }

    public DNACursor getCursor() {
      throw new ImplementMe();
    }

    public String getDefiningLoaderDescription() {
      throw new ImplementMe();
    }

    public ObjectID getObjectID() throws DNAException {
      throw new ImplementMe();
    }

    public ObjectID getParentObjectID() throws DNAException {
      throw new ImplementMe();
    }

    public String getTypeName() {
      throw new ImplementMe();
    }

    public long getVersion() {
      throw new ImplementMe();
    }

    public boolean hasLength() {
      throw new ImplementMe();
    }

    public boolean isDelta() {
      throw new ImplementMe();
    }
  }

  public void clear() {
    throw new ImplementMe();
  }

}

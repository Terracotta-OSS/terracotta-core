/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object;

import com.tc.exception.ImplementMe;
import com.tc.object.dna.api.DNA;
import com.tc.object.session.SessionID;
import com.tc.util.concurrent.NoExceptionLinkedQueue;

import java.util.Collection;

public class TestRemoteObjectManager implements RemoteObjectManager {

  public final NoExceptionLinkedQueue retrieveCalls         = new NoExceptionLinkedQueue();
  public final NoExceptionLinkedQueue retrieveResults       = new NoExceptionLinkedQueue();

  public final NoExceptionLinkedQueue retrieveRootIDCalls   = new NoExceptionLinkedQueue();
  public final NoExceptionLinkedQueue retrieveRootIDResults = new NoExceptionLinkedQueue();

  public DNA retrieve(ObjectID id) {
    retrieveCalls.put(id);
    return (DNA) retrieveResults.take();
  }

  public ObjectID retrieveRootID(String name) {
    retrieveRootIDCalls.put(name);
    return (ObjectID) retrieveRootIDResults.take();
  }

  public void addRoot(String name, ObjectID id) {
    throw new ImplementMe();
  }

  public void addAllObjects(SessionID sessionID, long batchID, Collection dnas) {
    throw new ImplementMe();
  }

  public void addObject(SessionID sessionID, DNA dna) {
    throw new ImplementMe();
  }

  public void removed(ObjectID id) {
    throw new ImplementMe();
  }

  public void requestOutstanding() {
    throw new ImplementMe();
  }

  public void pause() {
    throw new ImplementMe();

  }

  public void clear() {
    return;
  }

  public void unpause() {
    throw new ImplementMe();

  }

  public void starting() {
    throw new ImplementMe();
  }

  public DNA retrieve(ObjectID id, int depth) {
    throw new ImplementMe();
  }

}

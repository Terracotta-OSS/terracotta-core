/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object;

import com.tc.net.NodeID;
import com.tc.object.lockmanager.api.ThreadID;
import com.tc.object.msg.NodesWithObjectsMessage;
import com.tc.object.msg.NodesWithObjectsMessageFactory;
import com.tc.util.Assert;
import com.tc.util.runtime.ThreadIDManager;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class ClusterMetaDataManagerImpl implements ClusterMetaDataManager {

  private final ThreadIDManager                           threadIDManager;
  private final NodesWithObjectsMessageFactory            cmdmFactory;

  private final Map<ThreadID, Object>                     waitObjects = new HashMap<ThreadID, Object>();
  private final Map<ThreadID, Map<ObjectID, Set<NodeID>>> responses   = new HashMap<ThreadID, Map<ObjectID, Set<NodeID>>>();

  public ClusterMetaDataManagerImpl(final ThreadIDManager threadIDManager,
                                    final NodesWithObjectsMessageFactory cmdmFactory) {
    this.threadIDManager = threadIDManager;
    this.cmdmFactory = cmdmFactory;
  }

  public Set<NodeID> getNodesWithObject(final ObjectID objectID) {
    final ThreadID thisThread = threadIDManager.getThreadID();

    final NodesWithObjectsMessage m = cmdmFactory.newNodesWithObjectsMessage();
    m.setThreadID(thisThread);
    m.addObjectID(objectID);
    m.send();

    final Object waitObject = new Object();

    synchronized (this) {
      Assert.assertFalse(waitObjects.containsKey(thisThread));

      waitObjects.put(thisThread, waitObject);
    }

    final Map<ObjectID, Set<NodeID>> response;
    try {
      synchronized (waitObject) {
        waitObject.wait();
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      // todo: should we return something special here?
    } finally {
      synchronized (this) {
        waitObjects.remove(thisThread);
        response = responses.remove(thisThread);
      }
    }

    // no response arrived in time, returning an empty set
    if (null == response) {
      return Collections.emptySet();
    }
    return response.get(objectID);
  }

  public void setNodesWithObjectsResponse(final ThreadID threadID, final Map<ObjectID, Set<NodeID>> response) {
    final Object waitObject;
    synchronized (this) {
      waitObject = waitObjects.get(threadID);
      if (null == waitObject) {
        // check if there was actually a wait object, since the waiting
        // thread might have been interrupted in the meantime
        return;
      }
      responses.put(threadID, response);
    }

    synchronized (waitObject) {
      waitObject.notifyAll();
    }
  }
}

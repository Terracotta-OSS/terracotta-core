/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object;

import com.tc.net.NodeID;
import com.tc.object.dna.api.DNAEncoding;
import com.tc.object.lockmanager.api.ThreadID;
import com.tc.object.msg.ClusterMetaDataMessage;
import com.tc.object.msg.KeysForOrphanedValuesMessage;
import com.tc.object.msg.KeysForOrphanedValuesMessageFactory;
import com.tc.object.msg.NodesWithObjectsMessage;
import com.tc.object.msg.NodesWithObjectsMessageFactory;
import com.tc.util.Assert;
import com.tc.util.runtime.ThreadIDManager;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class ClusterMetaDataManagerImpl implements ClusterMetaDataManager {

  private final DNAEncoding                         encoding;
  private final ThreadIDManager                     threadIDManager;
  private final NodesWithObjectsMessageFactory      nwoFactory;
  private final KeysForOrphanedValuesMessageFactory kfovFactory;

  private final Map<ThreadID, WaitForResponse>      waitObjects = new HashMap<ThreadID, WaitForResponse>();
  private final Map<ThreadID, Object>               responses   = new HashMap<ThreadID, Object>();

  public ClusterMetaDataManagerImpl(final DNAEncoding encoding,
                                    final ThreadIDManager threadIDManager,
                                    final NodesWithObjectsMessageFactory nwoFactory,
                                    final KeysForOrphanedValuesMessageFactory kfovFactory) {
    this.encoding = encoding;
    this.threadIDManager = threadIDManager;
    this.nwoFactory = nwoFactory;
    this.kfovFactory = kfovFactory;
  }

  public DNAEncoding getEncoding() {
    return encoding;
  }

  public Set<NodeID> getNodesWithObject(final ObjectID objectID) {
    final NodesWithObjectsMessage message = nwoFactory.newNodesWithObjectsMessage();
    message.addObjectID(objectID);

    final Map<ObjectID, Set<NodeID>> response = sendMessageAndWait(message);

    // no response arrived in time, returning an empty set
    if (null == response) { return Collections.emptySet(); }

    return response.get(objectID);
  }

  public Map<ObjectID, Set<NodeID>> getNodesWithObjects(final Collection<ObjectID> objectIDs) {
    final NodesWithObjectsMessage message = nwoFactory.newNodesWithObjectsMessage();
    for (ObjectID objectID : objectIDs) {
      message.addObjectID(objectID);
    }

    final Map<ObjectID, Set<NodeID>> response = sendMessageAndWait(message);

    // no response arrived in time, returning an empty map
    if (null == response) { return Collections.emptyMap(); }

    return response;
  }

  public Set<?> getKeysForOrphanedValues(final ObjectID mapObjectID) {
    final KeysForOrphanedValuesMessage message = kfovFactory.newKeysForOrphanedValuesMessage();
    message.setMapObjectID(mapObjectID);

    final Set<?> response = sendMessageAndWait(message);

    // no response arrived in time, returning an empty set
    if (null == response) { return Collections.emptySet(); }

    return response;
  }

  private <R> R sendMessageAndWait(final ClusterMetaDataMessage message) {
    Assert.assertNotNull(message);

    final ThreadID thisThread = threadIDManager.getThreadID();

    Assert.assertFalse(waitObjects.containsKey(thisThread));

    final WaitForResponse waitObject = new WaitForResponse();

    synchronized (this) {
      Assert.assertFalse(waitObjects.containsKey(thisThread));

      waitObjects.put(thisThread, waitObject);
    }

    message.setThreadID(thisThread);
    message.send();

    final R response;
    try {
      synchronized (waitObject) {
        if (!waitObject.wasResponseReceived()) {
          waitObject.wait();
        }
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      // todo: should we return something special here?
    } finally {
      synchronized (this) {
        waitObjects.remove(thisThread);
        response = (R) responses.remove(thisThread);
      }
    }
    return response;
  }

  public void setResponse(final ThreadID threadID, final Object response) {
    final WaitForResponse waitObject;
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
      waitObject.markResponseReceived();
      waitObject.notifyAll();
    }
  }

  private class WaitForResponse {
    private boolean responseReceived = false;

    public boolean wasResponseReceived() {
      return responseReceived;
    }

    public void markResponseReceived() {
      this.responseReceived = true;
    }
  }
}

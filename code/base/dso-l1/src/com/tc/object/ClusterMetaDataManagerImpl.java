/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object;

import com.tc.exception.TCNotRunningException;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.ClientID;
import com.tc.net.GroupID;
import com.tc.net.NodeID;
import com.tc.net.protocol.tcm.ChannelID;
import com.tc.object.bytecode.Manageable;
import com.tc.object.bytecode.TCMap;
import com.tc.object.bytecode.TCServerMap;
import com.tc.object.dna.api.DNAEncoding;
import com.tc.object.locks.ThreadID;
import com.tc.object.msg.ClientHandshakeMessage;
import com.tc.object.msg.ClusterMetaDataMessage;
import com.tc.object.msg.KeysForOrphanedValuesMessage;
import com.tc.object.msg.KeysForOrphanedValuesMessageFactory;
import com.tc.object.msg.NodeMetaDataMessage;
import com.tc.object.msg.NodeMetaDataMessageFactory;
import com.tc.object.msg.NodesWithKeysMessage;
import com.tc.object.msg.NodesWithKeysMessageFactory;
import com.tc.object.msg.NodesWithObjectsMessage;
import com.tc.object.msg.NodesWithObjectsMessageFactory;
import com.tc.util.Assert;
import com.tc.util.State;
import com.tc.util.Util;
import com.tc.util.runtime.ThreadIDManager;
import com.tcclient.cluster.DsoNodeInternal;
import com.tcclient.cluster.DsoNodeMetaData;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ClusterMetaDataManagerImpl implements ClusterMetaDataManager {

  private static final TCLogger                             LOGGER                                   = TCLogging
                                                                                                         .getLogger(ClusterMetaDataManagerImpl.class);

  private static final long                                 RETRIEVE_WAIT_INTERVAL                   = 15000;

  private static final State                                PAUSED                                   = new State(
                                                                                                                 "PAUSED");
  private static final State                                RUNNING                                  = new State(
                                                                                                                 "RUNNING");
  private static final State                                STARTING                                 = new State(
                                                                                                                 "STARTING");

  private State                                             state                                    = RUNNING;

  private final GroupID                                     groupID;
  private final DNAEncoding                                 encoding;
  private final ThreadIDManager                             threadIDManager;
  private final NodesWithObjectsMessageFactory              nwoFactory;
  private final KeysForOrphanedValuesMessageFactory         kfovFactory;
  private final NodeMetaDataMessageFactory                  nmdmFactory;
  private final NodesWithKeysMessageFactory                 nwkmFactory;

  private final Map<ThreadID, NodesWithObjectsMessage>      outstandingNodesWithObjectsRequests      = new ConcurrentHashMap<ThreadID, NodesWithObjectsMessage>();
  private final Map<ThreadID, KeysForOrphanedValuesMessage> outstandingKeysForOrphanedValuesRequests = new ConcurrentHashMap<ThreadID, KeysForOrphanedValuesMessage>();
  private final Map<ThreadID, NodeMetaDataMessage>          outstandingNodeMetaDataRequests          = new ConcurrentHashMap<ThreadID, NodeMetaDataMessage>();
  private final Map<ThreadID, NodesWithKeysMessage>         outstandingNodesWithKeysRequests         = new ConcurrentHashMap<ThreadID, NodesWithKeysMessage>();

  private final Map<ThreadID, WaitForResponse>              waitObjects                              = new HashMap<ThreadID, WaitForResponse>();
  private final Map<ThreadID, Object>                       responses                                = new HashMap<ThreadID, Object>();
  private volatile boolean                                  isShutdown                               = false;

  public ClusterMetaDataManagerImpl(final GroupID groupID, final DNAEncoding encoding,
                                    final ThreadIDManager threadIDManager,
                                    final NodesWithObjectsMessageFactory nwoFactory,
                                    final KeysForOrphanedValuesMessageFactory kfovFactory,
                                    final NodeMetaDataMessageFactory nmdmFactory,
                                    final NodesWithKeysMessageFactory nwkmFactory) {
    this.groupID = groupID;
    this.encoding = encoding;
    this.threadIDManager = threadIDManager;
    this.nwoFactory = nwoFactory;
    this.kfovFactory = kfovFactory;
    this.nmdmFactory = nmdmFactory;
    this.nwkmFactory = nwkmFactory;
  }

  public DNAEncoding getEncoding() {
    return encoding;
  }

  public Set<NodeID> getNodesWithObject(final ObjectID objectID) {
    waitUntilRunning();

    final NodesWithObjectsMessage message = nwoFactory.newNodesWithObjectsMessage(groupID);
    message.addObjectID(objectID);

    final Map<ObjectID, Set<NodeID>> response = sendNodesWithObjectsMessageAndWait(message);

    // no response arrived in time, returning an empty set
    if (null == response) {
      LOGGER.warn("No response arrived in time for getNodesWithObject for object '" + objectID
                  + "', returning empty set");
      return Collections.emptySet();
    }

    return response.get(objectID);
  }

  public Map<ObjectID, Set<NodeID>> getNodesWithObjects(final Collection<ObjectID> objectIDs) {
    waitUntilRunning();

    final NodesWithObjectsMessage message = nwoFactory.newNodesWithObjectsMessage(groupID);
    for (ObjectID objectID : objectIDs) {
      message.addObjectID(objectID);
    }

    final Map<ObjectID, Set<NodeID>> response = sendNodesWithObjectsMessageAndWait(message);

    // no response arrived in time, returning an empty map
    if (null == response) {
      LOGGER.warn("No response arrived in time for getNodesWithObjects, returning empty map");
      return Collections.emptyMap();
    }

    return response;
  }

  public Set<?> getKeysForOrphanedValues(final TCMap tcMap) {
    waitUntilRunning();

    final ObjectID mapObjectID = ((Manageable) tcMap).__tc_managed().getObjectID();

    final KeysForOrphanedValuesMessage message = kfovFactory.newKeysForOrphanedValuesMessage(groupID);
    message.setMapObjectID(mapObjectID);

    final Set<?> response = sendKeysForOrphanedValuesMessageAndWait(message);

    // no response arrived in time, returning an empty set
    if (null == response) {
      LOGGER.warn("No response arrived in time for getKeysForOrphanedValues for map with object ID '" + mapObjectID
                  + "', returning empty set");
      return Collections.emptySet();
    }

    return response;
  }

  public DsoNodeMetaData retrieveMetaDataForDsoNode(final DsoNodeInternal node) {
    waitUntilRunning();

    final NodeMetaDataMessage message = nmdmFactory.newNodeMetaDataMessage();
    message.setNodeID(new ClientID(new ChannelID(node.getChannelId()).toLong()));
    DsoNodeMetaData metaData = sendNodeMetaDataMessageAndWait(message);
    node.setMetaData(metaData);
    return metaData;
  }

  private Map<ObjectID, Set<NodeID>> sendNodesWithObjectsMessageAndWait(final NodesWithObjectsMessage message) {
    final ThreadID thisThread = threadIDManager.getThreadID();
    outstandingNodesWithObjectsRequests.put(thisThread, message);
    try {
      return sendMessageAndWait(thisThread, message);
    } finally {
      outstandingNodesWithObjectsRequests.remove(thisThread);
    }
  }

  public Set sendKeysForOrphanedValuesMessageAndWait(final KeysForOrphanedValuesMessage message) {
    final ThreadID thisThread = threadIDManager.getThreadID();
    outstandingKeysForOrphanedValuesRequests.put(thisThread, message);
    try {
      return sendMessageAndWait(thisThread, message);
    } finally {
      outstandingKeysForOrphanedValuesRequests.remove(thisThread);
    }
  }

  public <K> Map<K, Set<NodeID>> sendNodesWithKeysMessageAndWait(final NodesWithKeysMessage message) {
    final ThreadID thisThread = threadIDManager.getThreadID();
    outstandingNodesWithKeysRequests.put(thisThread, message);
    try {
      return sendMessageAndWait(thisThread, message);
    } finally {
      outstandingNodesWithKeysRequests.remove(thisThread);
    }
  }

  private DsoNodeMetaData sendNodeMetaDataMessageAndWait(final NodeMetaDataMessage message) {
    final ThreadID thisThread = threadIDManager.getThreadID();
    outstandingNodeMetaDataRequests.put(thisThread, message);
    try {
      return sendMessageAndWait(thisThread, message);
    } finally {
      outstandingNodeMetaDataRequests.remove(thisThread);
    }
  }

  private <R> R sendMessageAndWait(final ThreadID thisThread, final ClusterMetaDataMessage message) {
    Assert.assertNotNull(thisThread);
    Assert.assertNotNull(message);

    Assert.assertFalse(waitObjects.containsKey(thisThread));

    final WaitForResponse waitObject = new WaitForResponse();

    synchronized (waitObjects) {
      Assert.assertFalse(waitObjects.containsKey(thisThread));

      waitObjects.put(thisThread, waitObject);
    }

    message.setThreadID(thisThread);
    message.send();

    final R response;
    try {
      synchronized (waitObject) {
        if (!waitObject.wasResponseReceived()) {
          waitObject.wait(RETRIEVE_WAIT_INTERVAL);
        }
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      // todo: should we return something special here?
    } finally {
      synchronized (waitObjects) {
        waitObjects.remove(thisThread);
        response = (R) responses.remove(thisThread);
      }
    }
    return response;
  }

  public void setResponse(final ThreadID threadID, final Object response) {
    final WaitForResponse waitObject;
    synchronized (waitObjects) {
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

  public <K> Map<K, Set<NodeID>> getNodesWithKeys(final TCMap tcMap, final Collection<? extends K> keys) {
    waitUntilRunning();

    final ObjectID mapObjectID = ((Manageable) tcMap).__tc_managed().getObjectID();
    return getNodesWithKeys(keys, mapObjectID);
  }

  private <K> Map<K, Set<NodeID>> getNodesWithKeys(final Collection<? extends K> keys, final ObjectID mapObjectID) {NodesWithKeysMessage message = nwkmFactory.newNodesWithKeysMessage(groupID);
    message.setMapObjectID(mapObjectID);
    message.setKeys((Set<Object>)keys);
    Map<K, Set<NodeID>> result = sendMessageAndWait(threadIDManager.getThreadID(), message);
    for (K key : keys) {
      if(!result.containsKey(key)) {
        result.put(key, Collections.<NodeID>emptySet());
      }
    }
    return result;
  }

  public <K> Map<K, Set<NodeID>> getNodesWithKeys(final TCServerMap tcMap, final Collection<? extends K> keys) {
    waitUntilRunning();

    final ObjectID mapObjectID = tcMap.__tc_managed().getObjectID();
    return getNodesWithKeys(keys, mapObjectID);
  }

  private void resendOutstanding() {
    synchronized (this) {
      for (NodesWithObjectsMessage oldMessage : outstandingNodesWithObjectsRequests.values()) {
        final NodesWithObjectsMessage newMessage = nwoFactory.newNodesWithObjectsMessage(groupID);
        for (ObjectID objectID : oldMessage.getObjectIDs()) {
          newMessage.addObjectID(objectID);
        }
        newMessage.setThreadID(oldMessage.getThreadID());
        newMessage.send();
      }

      for (KeysForOrphanedValuesMessage oldMessage : outstandingKeysForOrphanedValuesRequests.values()) {
        final KeysForOrphanedValuesMessage newMessage = kfovFactory.newKeysForOrphanedValuesMessage(groupID);
        newMessage.setMapObjectID(oldMessage.getMapObjectID());
        newMessage.setThreadID(oldMessage.getThreadID());
        newMessage.send();
      }

      for (NodeMetaDataMessage oldMessage : outstandingNodeMetaDataRequests.values()) {
        final NodeMetaDataMessage newMessage = nmdmFactory.newNodeMetaDataMessage();
        newMessage.setNodeID(oldMessage.getNodeID());
        newMessage.setThreadID(oldMessage.getThreadID());
        newMessage.send();
      }
    }
  }

  public void shutdown() {
    isShutdown = true;
    synchronized (this) {
      this.notifyAll();
    }
  }

  public void pause(final NodeID remote, final int disconnected) {
    if (isShutdown) return;
    synchronized (this) {
      assertNotPaused("Attempt to pause while PAUSED");
      this.state = PAUSED;
      this.notifyAll();
    }
  }

  public void initializeHandshake(final NodeID thisNode, final NodeID remoteNode,
                                  final ClientHandshakeMessage handshakeMessage) {
    if (isShutdown) return;
    synchronized (this) {
      assertPaused("Attempt to initializeHandshake while not PAUSED");
      this.state = STARTING;
    }
  }

  public void unpause(final NodeID remote, final int disconnected) {
    if (isShutdown) return;
    synchronized (this) {
      assertNotRunning("Attempt to unpause while RUNNING");
      this.state = RUNNING;
      resendOutstanding();
      this.notifyAll();
    }
  }

  private void waitUntilRunning() {
    boolean isInterrupted = false;
    try {
      synchronized (this) {
        while (this.state != RUNNING) {
          if (isShutdown) { throw new TCNotRunningException(); }
          try {
            this.wait();
          } catch (InterruptedException e) {
            isInterrupted = true;
          }
        }
      }
    } finally {
      Util.selfInterruptIfNeeded(isInterrupted);
    }
  }

  private void assertPaused(final Object message) {
    if (this.state != PAUSED) { throw new AssertionError(message + ": " + this.state); }
  }

  private void assertNotPaused(final Object message) {
    if (this.state == PAUSED) { throw new AssertionError(message + ": " + this.state); }
  }

  private void assertNotRunning(final Object message) {
    if (this.state == RUNNING) { throw new AssertionError(message + ": " + this.state); }
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

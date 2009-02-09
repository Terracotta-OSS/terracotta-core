/*
 * All content copyright (c) 2003-2009 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.objectserver.clustermetadata;

import com.tc.net.NodeID;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.object.ObjectID;
import com.tc.object.msg.NodesWithObjectsMessage;
import com.tc.object.msg.NodesWithObjectsResponseMessage;
import com.tc.objectserver.l1.api.ClientStateManager;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class ServerClusterMetaDataManagerImpl implements ServerClusterMetaDataManager {

  private final ClientStateManager clientStateManager;

  public ServerClusterMetaDataManagerImpl(final ClientStateManager clientStateManager) {
    this.clientStateManager = clientStateManager;
  }

  public void handleMessage(final NodesWithObjectsMessage message) {
    NodesWithObjectsResponseMessage responseMessage = (NodesWithObjectsResponseMessage)message.getChannel().createMessage(TCMessageType.NODES_WITH_OBJECTS_RESPONSE_MESSAGE);

    final Map<ObjectID, Set<NodeID>> response = new HashMap<ObjectID, Set<NodeID>>();

    Set<ObjectID> objectIDs = message.getObjectIDs();
    for (ObjectID objectID : objectIDs) {
      response.put(objectID, clientStateManager.getConnectedClientIDs());
    }
    responseMessage.initialize(message.getThreadID(), response);
    responseMessage.send();
  }
}

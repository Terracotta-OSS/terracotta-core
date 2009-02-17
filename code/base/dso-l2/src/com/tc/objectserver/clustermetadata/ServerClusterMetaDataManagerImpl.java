/*
 * All content copyright (c) 2003-2009 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.clustermetadata;

import com.tc.logging.TCLogger;
import com.tc.net.NodeID;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.object.ObjectID;
import com.tc.object.msg.KeysForOrphanedValuesMessage;
import com.tc.object.msg.KeysForOrphanedValuesResponseMessage;
import com.tc.object.msg.NodesWithObjectsMessage;
import com.tc.object.msg.NodesWithObjectsResponseMessage;
import com.tc.objectserver.api.ObjectManager;
import com.tc.objectserver.core.api.ManagedObject;
import com.tc.objectserver.core.api.ManagedObjectState;
import com.tc.objectserver.l1.api.ClientStateManager;
import com.tc.objectserver.managedobject.PartialMapManagedObjectState;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ServerClusterMetaDataManagerImpl implements ServerClusterMetaDataManager {

  private final TCLogger           logger;
  private final ClientStateManager clientStateManager;
  private final ObjectManager      objectManager;

  public ServerClusterMetaDataManagerImpl(final TCLogger logger, final ClientStateManager clientStateManager,
                                          final ObjectManager objectManager) {
    this.logger = logger;
    this.clientStateManager = clientStateManager;
    this.objectManager = objectManager;
  }

  public void handleMessage(final NodesWithObjectsMessage message) {
    NodesWithObjectsResponseMessage responseMessage = (NodesWithObjectsResponseMessage) message.getChannel()
        .createMessage(TCMessageType.NODES_WITH_OBJECTS_RESPONSE_MESSAGE);

    final Map<ObjectID, Set<NodeID>> response = new HashMap<ObjectID, Set<NodeID>>();

    Set<ObjectID> objectIDs = message.getObjectIDs();
    for (ObjectID objectID : objectIDs) {
      Set<NodeID> referencingNodeIDs = new HashSet<NodeID>();
      for (NodeID nodeID : clientStateManager.getConnectedClientIDs()) {
        if (clientStateManager.hasReference(nodeID, objectID)) {
          referencingNodeIDs.add(nodeID);
        }
      }
      response.put(objectID, referencingNodeIDs);
    }
    responseMessage.initialize(message.getThreadID(), response);
    responseMessage.send();
  }

  public void handleMessage(final KeysForOrphanedValuesMessage message) {
    KeysForOrphanedValuesResponseMessage responseMessage = (KeysForOrphanedValuesResponseMessage) message.getChannel()
        .createMessage(TCMessageType.KEYS_FOR_ORPHANED_VALUES_RESPONSE_MESSAGE);

    final Set<Object> response = new HashSet<Object>();

    final ManagedObject managedMap = objectManager.getObjectByIDOrNull(message.getMapObjectID());
    try {
      final ManagedObjectState state = managedMap.getManagedObjectState();
      if (state instanceof PartialMapManagedObjectState) {
        final Set<NodeID> connectedClients = clientStateManager.getConnectedClientIDs();

        Map realMap = ((PartialMapManagedObjectState) state).getMap();
        for (Map.Entry entry : (Set<Map.Entry>)realMap.entrySet()) {
          if (entry.getValue() instanceof ObjectID) {
            boolean isOrphan = true;
            for (NodeID nodeID : connectedClients) {
              if (clientStateManager.hasReference(nodeID, (ObjectID)entry.getValue())) {
                isOrphan = false;
                break;
              }
            }

            if (isOrphan) {
              response.add(entry.getKey());
            }
          }
        }
      } else {
        logger.error("Received keys for orphaned values message for object '" + message.getMapObjectID()
                    + "' whose managed state isn't a partial map, returning an empty set.");
      }
    } finally {
      objectManager.releaseReadOnly(managedMap);
    }

    responseMessage.initialize(message.getThreadID(), response);
    responseMessage.send();
  }
}

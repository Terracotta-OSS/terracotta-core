/*
 * All content copyright (c) 2003-2009 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.clustermetadata;

import com.tc.io.serializer.TCObjectOutputStream;
import com.tc.logging.TCLogger;
import com.tc.net.NodeID;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.object.ObjectID;
import com.tc.object.dna.impl.UTF8ByteDataHolder;
import com.tc.object.msg.KeysForOrphanedValuesMessage;
import com.tc.object.msg.KeysForOrphanedValuesResponseMessage;
import com.tc.object.msg.NodeMetaDataMessage;
import com.tc.object.msg.NodeMetaDataResponseMessage;
import com.tc.object.msg.NodesWithKeysMessage;
import com.tc.object.msg.NodesWithKeysResponseMessage;
import com.tc.object.msg.NodesWithObjectsMessage;
import com.tc.object.msg.NodesWithObjectsResponseMessage;
import com.tc.object.net.DSOChannelManager;
import com.tc.object.net.NoSuchChannelException;
import com.tc.objectserver.api.ObjectManager;
import com.tc.objectserver.core.api.ManagedObject;
import com.tc.objectserver.core.api.ManagedObjectState;
import com.tc.objectserver.l1.api.ClientStateManager;
import com.tc.objectserver.managedobject.PartialMapManagedObjectState;

import java.io.ByteArrayOutputStream;
import java.net.InetAddress;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ServerClusterMetaDataManagerImpl implements ServerClusterMetaDataManager {

  private final TCLogger           logger;
  private final ClientStateManager clientStateManager;
  private final ObjectManager      objectManager;
  private final DSOChannelManager  channelManager;

  public ServerClusterMetaDataManagerImpl(final TCLogger logger, final ClientStateManager clientStateManager,
                                          final ObjectManager objectManager, final DSOChannelManager channelManager) {
    this.logger = logger;
    this.clientStateManager = clientStateManager;
    this.objectManager = objectManager;
    this.channelManager = channelManager;
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

  public void handleMessage(final NodesWithKeysMessage message) {
    NodesWithKeysResponseMessage responseMessage = (NodesWithKeysResponseMessage) message.getChannel()
        .createMessage(TCMessageType.NODES_WITH_KEYS_RESPONSE_MESSAGE);

    Map<Object, Set<NodeID>> resultMap = new HashMap<Object, Set<NodeID>>();

    if(message.getMapObjectID() != null) {
      final ManagedObject managedMap = objectManager.getObjectByID(message.getMapObjectID());
      try {
        final ManagedObjectState state = managedMap.getManagedObjectState();
        if (state instanceof PartialMapManagedObjectState) {
          final Set<NodeID> connectedClients = clientStateManager.getConnectedClientIDs();

          Map realMap = ((PartialMapManagedObjectState) state).getMap();
          for (Object key : message.getKeys()) {
            UTF8ByteDataHolder holder = new UTF8ByteDataHolder(key.toString());
            if(realMap.containsKey(holder)) {
            Object value = realMap.get(holder);
              if(value instanceof ObjectID) {
                Set<NodeID> nodeIDSet = resultMap.get(key);
                if(nodeIDSet == null) {
                  nodeIDSet = new HashSet<NodeID>();
                  resultMap.put(holder, nodeIDSet);
                }
                for (NodeID nodeID : connectedClients) {
                  if (clientStateManager.hasReference(nodeID, (ObjectID) value)) {
                    nodeIDSet.add(nodeID);
                  }
                }
              }
            }
          }
          responseMessage.initialize(message.getThreadID(), resultMap);
        } else {
          logger.error("Received nodes for keys message for object '" + message.getMapObjectID()
                       + "' whose managed state isn't a partial map, returning an empty set.");
          responseMessage.initialize(message.getThreadID(), Collections.<Object, Set<NodeID>>emptyMap());
        }
      } finally {
        objectManager.releaseReadOnly(managedMap);
      }
    }

    responseMessage.send();
  }

  public void handleMessage(final KeysForOrphanedValuesMessage message) {
    KeysForOrphanedValuesResponseMessage responseMessage = (KeysForOrphanedValuesResponseMessage) message.getChannel()
        .createMessage(TCMessageType.KEYS_FOR_ORPHANED_VALUES_RESPONSE_MESSAGE);

    // handle the message for non active-active, ie. there's only one server group and this has knowledge about
    // state of the map and its values
    if (message.getMapObjectID() != null) {
      final Set<Object> response = new HashSet<Object>();

      final ManagedObject managedMap = objectManager.getObjectByID(message.getMapObjectID());
      try {
        final ManagedObjectState state = managedMap.getManagedObjectState();
        if (state instanceof PartialMapManagedObjectState) {
          final Set<NodeID> connectedClients = clientStateManager.getConnectedClientIDs();

          Map realMap = ((PartialMapManagedObjectState) state).getMap();
          for (Map.Entry entry : (Set<Map.Entry>) realMap.entrySet()) {
            if (entry.getValue() instanceof ObjectID) {
              boolean isOrphan = true;

              for (NodeID nodeID : connectedClients) {
                if (clientStateManager.hasReference(nodeID, (ObjectID) entry.getValue())) {
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

      // write the DNA of the orphaned keys into a byte array
      final ByteArrayOutputStream bytesOut = new ByteArrayOutputStream();
      final TCObjectOutputStream objectOut = new TCObjectOutputStream(bytesOut);
      objectOut.writeInt(response.size());
      for (Object key : response) {
        objectOut.writeObject(key);
      }
      objectOut.flush();

      responseMessage.initialize(message.getThreadID(), bytesOut.toByteArray());

    }
    // handle the message for active-active, this group receives the map values that it knows the state for and reports
    // whether they're orphans
    else if (message.getMapValueObjectIDs() != null) {

      final Set<ObjectID> response = new HashSet<ObjectID>();

      final Collection<ObjectID> objectIDs = message.getMapValueObjectIDs();
      final Set<NodeID> connectedClients = clientStateManager.getConnectedClientIDs();
      for (ObjectID objectID : objectIDs) {
        boolean isOrphan = true;

        for (NodeID nodeID : connectedClients) {
          if (clientStateManager.hasReference(nodeID, objectID)) {
            isOrphan = false;
            break;
          }
        }

        if (isOrphan) {
          response.add(objectID);
        }
      }

      responseMessage.initialize(message.getThreadID(), response);
    } else {
      logger.error("Received keys for orphaned values message without a map ID or map value IDs.");
    }

    responseMessage.send();
  }

  public void handleMessage(final NodeMetaDataMessage message) {
    NodeMetaDataResponseMessage responseMessage = (NodeMetaDataResponseMessage) message.getChannel()
        .createMessage(TCMessageType.NODE_META_DATA_RESPONSE_MESSAGE);

    String ip;
    String hostname;

    try {
      final MessageChannel channel = channelManager.getActiveChannel(message.getNodeID());
      final InetAddress address = channel.getRemoteAddress().getAddress();
      ip = address.getHostAddress();
      hostname = address.getHostName();
    } catch (NoSuchChannelException e) {
      logger.error("Couldn't find channel for node  '" + message.getNodeID()
                   + "' sending empty meta data as a response");
      ip = null;
      hostname = null;
    }

    responseMessage.initialize(message.getThreadID(), ip, hostname);
    responseMessage.send();
  }
}

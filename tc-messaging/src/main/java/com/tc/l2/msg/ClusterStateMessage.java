/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Terracotta Core.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
 */
package com.tc.l2.msg;

import com.tc.io.TCByteBufferInput;
import com.tc.io.TCByteBufferOutput;
import com.tc.l2.ha.ClusterState;
import com.tc.net.groups.AbstractGroupMessage;
import com.tc.net.groups.MessageID;
import com.tc.net.protocol.transport.ConnectionID;
import com.tc.util.Assert;

import java.io.EOFException;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class ClusterStateMessage extends AbstractGroupMessage {

  public static final int        NEW_CONNECTION_CREATED       = 0x01;
  public static final int        CONNECTION_DESTROYED         = 0x02;
  public static final int        COMPLETE_STATE               = 0xF0;
  public static final int        OPERATION_FAILED_SPLIT_BRAIN = 0xFE;
  public static final int        OPERATION_SUCCESS            = 0xFF;

  private long                   nextAvailableObjectID;
  private long                   nextAvailableGID;
  private String                 clusterID;
  private ConnectionID           connectionID;
  private long                   nextAvailableChannelID;
  private Set<ConnectionID>      connectionIDs;
  private byte[]                 configSyncData = new byte[0];

  // To make serialization happy
  public ClusterStateMessage() {
    super(-1);
  }

  public ClusterStateMessage(int type) {
    super(type);
  }

  public ClusterStateMessage(int type, MessageID requestID) {
    super(type, requestID);
  }

  public ClusterStateMessage(int type, ConnectionID connID) {
    super(type);
    this.connectionID = connID;
  }

  @Override
  protected void basicDeserializeFrom(TCByteBufferInput in) throws IOException {
    switch (getType()) {
      case NEW_CONNECTION_CREATED:
      case CONNECTION_DESTROYED:
        connectionID = ConnectionID.readFrom(in);
        break;
      case COMPLETE_STATE:
        nextAvailableObjectID = in.readLong();
        nextAvailableGID = in.readLong();
        nextAvailableChannelID = in.readLong();
        clusterID = in.readString();
        int size = in.readInt();
        connectionIDs = new HashSet<ConnectionID>(size);
        for (int i = 0; i < size; i++) {
          connectionIDs.add(ConnectionID.readFrom(in));
        }

        int configSyncDataSize = 0;
        try {
          configSyncDataSize = in.readInt();
        } catch (EOFException e) {
          // ignore
        }
        configSyncData = new byte[configSyncDataSize];
        in.read(configSyncData);

        break;
      case OPERATION_FAILED_SPLIT_BRAIN:
      case OPERATION_SUCCESS:
        break;
      default:
        throw new AssertionError("Unknown type : " + getType());
    }
  }

  @Override
  protected void basicSerializeTo(TCByteBufferOutput out) {
    switch (getType()) {
      case NEW_CONNECTION_CREATED:
      case CONNECTION_DESTROYED:
        connectionID.writeTo(out);
        break;
      case COMPLETE_STATE:
        out.writeLong(nextAvailableObjectID);
        out.writeLong(nextAvailableGID);
        out.writeLong(nextAvailableChannelID);
        out.writeString(clusterID);
        out.writeInt(connectionIDs.size());
        for (ConnectionID id : connectionIDs) {
          id.writeTo(out);
        }
        out.writeInt(configSyncData.length);
        out.write(configSyncData);
        break;
      case OPERATION_FAILED_SPLIT_BRAIN:
      case OPERATION_SUCCESS:
        break;
      default:
        throw new AssertionError("Unknown type : " + getType());
    }
  }

  public long getNextAvailableObjectID() {
    return nextAvailableObjectID;
  }

  public long getNextAvailableGlobalTxnID() {
    return nextAvailableGID;
  }

  public String getClusterID() {
    return clusterID;
  }

  public ConnectionID getConnectionID() {
    return connectionID;
  }

  public void initMessage(ClusterState state) {
    switch (getType()) {
      case COMPLETE_STATE:
        clusterID = state.getStripeID().getName();
        connectionIDs = state.getAllConnections();
        configSyncData = state.getConfigSyncData();
        nextAvailableGID = state.getStartGlobalMessageID();
        nextAvailableChannelID = state.getNextAvailableChannelID();
        break;
      default:
        throw new AssertionError("Wrong Type : " + getType());
    }
  }

  public void initState(ClusterState state) {
    switch (getType()) {
      case COMPLETE_STATE:
        state.setNextAvailableChannelID(nextAvailableChannelID);
        for (ConnectionID id : connectionIDs) {
          Assert.assertTrue(id.getChannelID() < nextAvailableChannelID);
          state.addNewConnection(id);
        }
        // trigger local stripeID ready event after StripeIDMap loaded.
        state.setStripeID(clusterID);
        state.setConfigSyncData(configSyncData);
        state.setStartGlobalMessageID(nextAvailableGID);
        break;
      case NEW_CONNECTION_CREATED:
        state.addNewConnection(connectionID);
        break;
      case CONNECTION_DESTROYED:
        state.removeConnection(connectionID);
        break;
      default:
        throw new AssertionError("Wrong Type : " + getType());
    }
  }

  public boolean isSplitBrainMessage() {
    return getType() == OPERATION_FAILED_SPLIT_BRAIN;
  }

  public static ClusterStateMessage createOKResponse(ClusterStateMessage msg) {
    ClusterStateMessage response = new ClusterStateMessage(ClusterStateMessage.OPERATION_SUCCESS, msg.getMessageID());
    return response;
  }

  public static ClusterStateMessage createNGSplitBrainResponse(ClusterStateMessage msg) {
    ClusterStateMessage response = new ClusterStateMessage(ClusterStateMessage.OPERATION_FAILED_SPLIT_BRAIN, msg
        .getMessageID());
    return response;
  }

  public static ClusterStateMessage createClusterStateMessage(ClusterState state) {
    ClusterStateMessage msg = new ClusterStateMessage(ClusterStateMessage.COMPLETE_STATE);
    msg.initMessage(state);
    return msg;
  }

  public static ClusterStateMessage createNewConnectionCreatedMessage(ConnectionID connID) {
    ClusterStateMessage msg = new ClusterStateMessage(ClusterStateMessage.NEW_CONNECTION_CREATED, connID);
    return msg;
  }

  public static ClusterStateMessage createConnectionDestroyedMessage(ConnectionID connID) {
    ClusterStateMessage msg = new ClusterStateMessage(ClusterStateMessage.CONNECTION_DESTROYED, connID);
    return msg;
  }

  @Override
  public String toString() {
    return "ClusterStateMessage{" + "type=" + getType() + '}';
  }
}

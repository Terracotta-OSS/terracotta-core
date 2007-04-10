/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.l2.msg;

import com.tc.l2.ha.ClusterState;
import com.tc.net.groups.AbstractGroupMessage;
import com.tc.net.groups.MessageID;
import com.tc.net.protocol.transport.ConnectionID;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class ClusterStateMessage extends AbstractGroupMessage {

  public static final int OBJECT_ID              = 0x00;
  public static final int NEW_CONNECTION_CREATED = 0x01;
  public static final int CONNECTION_DESTROYED   = 0x02;
  public static final int GLOBAL_TRANSACTION_ID  = 0x03;
  public static final int COMPLETE_STATE         = 0xF0;
  public static final int OPERATION_SUCCESS      = 0xFF;

  private long            nextAvailableObjectID;
  private long            nextAvailableGID;
  private String          clusterID;
  private ConnectionID    connectionID;
  private long            nextAvailableChannelID;
  private Set             connectionIDs;

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

  protected void basicReadExternal(int msgType, ObjectInput in) throws IOException {
    switch (msgType) {
      case OBJECT_ID:
        nextAvailableObjectID = in.readLong();
        break;
      case GLOBAL_TRANSACTION_ID:
        nextAvailableGID = in.readLong();
        break;
      case NEW_CONNECTION_CREATED:
      case CONNECTION_DESTROYED:
        connectionID = readConnectionID(in);
        break;
      case COMPLETE_STATE:
        nextAvailableObjectID = in.readLong();
        nextAvailableGID = in.readLong();
        nextAvailableChannelID = in.readLong();
        clusterID = in.readUTF();
        int size = in.readInt();
        connectionIDs = new HashSet(size);
        for (int i = 0; i < size; i++) {
          connectionIDs.add(readConnectionID(in));
        }
        break;
      case OPERATION_SUCCESS:
        break;
      default:
        throw new AssertionError("Unknown type : " + msgType);
    }
  }

  protected void basicWriteExternal(int msgType, ObjectOutput out) throws IOException {
    switch (msgType) {
      case OBJECT_ID:
        out.writeLong(nextAvailableObjectID);
        break;
      case GLOBAL_TRANSACTION_ID:
        out.writeLong(nextAvailableGID);
        break;
      case NEW_CONNECTION_CREATED:
      case CONNECTION_DESTROYED:
        writeConnectionID(connectionID, out);
        break;
      case COMPLETE_STATE:
        out.writeLong(nextAvailableObjectID);
        out.writeLong(nextAvailableGID);
        out.writeLong(nextAvailableChannelID);
        out.writeUTF(clusterID);
        out.writeInt(connectionIDs.size());
        for (Iterator i = connectionIDs.iterator(); i.hasNext();) {
          ConnectionID conn = (ConnectionID) i.next();
          writeConnectionID(conn, out);
        }
        break;
      case OPERATION_SUCCESS:
        break;
      default:
        throw new AssertionError("Unknown type : " + msgType);
    }
  }

  private void writeConnectionID(ConnectionID conn, ObjectOutput out) throws IOException {
    out.writeLong(conn.getChannelID());
    out.writeUTF(conn.getServerID());
  }

  private ConnectionID readConnectionID(ObjectInput in) throws IOException {
    return new ConnectionID(in.readLong(), in.readUTF());
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
      case OBJECT_ID:
        nextAvailableObjectID = state.getNextAvailableObjectID();
        break;
      case GLOBAL_TRANSACTION_ID:
        nextAvailableGID = state.getNextAvailableGlobalTxnID();
        break;
      case COMPLETE_STATE:
        nextAvailableObjectID = state.getNextAvailableObjectID();
        nextAvailableGID = state.getNextAvailableGlobalTxnID();
        nextAvailableChannelID = state.getNextAvailableChannelID();
        clusterID = state.getClusterID();
        connectionIDs = state.getAllConnections();
        break;
      default:
        throw new AssertionError("Wrong Type : " + getType());
    }
  }

  public void initState(ClusterState state) {
    switch (getType()) {
      case OBJECT_ID:
        state.setNextAvailableObjectID(nextAvailableObjectID);
        break;
      case GLOBAL_TRANSACTION_ID:
        state.setNextAvailableGlobalTransactionID(nextAvailableGID);
        break;
      case COMPLETE_STATE:
        state.setNextAvailableObjectID(nextAvailableObjectID);
        state.setNextAvailableGlobalTransactionID(nextAvailableGID);
        state.setNextAvailableChannelID(nextAvailableChannelID);
        state.setClusterID(clusterID);
        for (Iterator i = connectionIDs.iterator(); i.hasNext();) {
          ConnectionID conn = (ConnectionID) i.next();
          state.addNewConnection(conn);
        }
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

}

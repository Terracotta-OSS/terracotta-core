/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object;

import com.tc.async.api.Sink;
import com.tc.net.GroupID;
import com.tc.net.MaxConnectionsExceededException;
import com.tc.net.NodeID;
import com.tc.net.protocol.tcm.ChannelEventListener;
import com.tc.net.protocol.tcm.ClientMessageChannel;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.object.msg.AcknowledgeTransactionMessage;
import com.tc.object.msg.AcknowledgeTransactionMessageFactory;
import com.tc.object.msg.ClientHandshakeMessage;
import com.tc.object.msg.ClientHandshakeMessageFactory;
import com.tc.object.msg.CommitTransactionMessage;
import com.tc.object.msg.CommitTransactionMessageFactory;
import com.tc.object.msg.CompletedTransactionLowWaterMarkMessage;
import com.tc.object.msg.CompletedTransactionLowWaterMarkMessageFactory;
import com.tc.object.msg.JMXMessage;
import com.tc.object.msg.LockRequestMessage;
import com.tc.object.msg.LockRequestMessageFactory;
import com.tc.object.msg.ObjectIDBatchRequestMessage;
import com.tc.object.msg.ObjectIDBatchRequestMessageFactory;
import com.tc.object.msg.RequestManagedObjectMessage;
import com.tc.object.msg.RequestManagedObjectMessageFactory;
import com.tc.object.msg.RequestRootMessage;
import com.tc.object.msg.RequestRootMessageFactory;
import com.tc.object.net.DSOClientMessageChannel;
import com.tc.util.TCTimeoutException;

import java.io.IOException;
import java.net.UnknownHostException;

public class DSOClientMessageChannelImpl implements DSOClientMessageChannel, LockRequestMessageFactory,
    RequestRootMessageFactory, RequestManagedObjectMessageFactory, ClientHandshakeMessageFactory,
    ObjectIDBatchRequestMessageFactory, CommitTransactionMessageFactory, AcknowledgeTransactionMessageFactory,
    CompletedTransactionLowWaterMarkMessageFactory {

  private final ClientMessageChannel channel;
  private final GroupID              groups[];
  private ClientIDProvider           clientIDProvider;

  public DSOClientMessageChannelImpl(ClientMessageChannel theChannel, GroupID[] gids) {
    this.channel = theChannel;
    this.groups = gids;
    this.clientIDProvider = new ClientIDProviderImpl(theChannel.getChannelIDProvider());
  }

  public void addClassMapping(TCMessageType messageType, Class messageClass) {
    this.channel.addClassMapping(messageType, messageClass);
  }

  public ClientIDProvider getClientIDProvider() {
    return clientIDProvider;
  }

  public void addListener(ChannelEventListener listener) {
    channel.addListener(listener);
  }

  public void routeMessageType(TCMessageType messageType, Sink destSink, Sink hydrateSink) {
    channel.routeMessageType(messageType, destSink, hydrateSink);
  }

  public ClientMessageChannel channel() {
    return channel;
  }

  public void open() throws TCTimeoutException, UnknownHostException, IOException, MaxConnectionsExceededException {
    channel.open();
  }

  public void close() {
    channel.close();
  }

  public LockRequestMessage newLockRequestMessage() {
    return (LockRequestMessage) channel.createMessage(TCMessageType.LOCK_REQUEST_MESSAGE);
  }

  public LockRequestMessageFactory getLockRequestMessageFactory() {
    return this;
  }

  public RequestRootMessage newRequestRootMessage(NodeID nodeID) {
    return (RequestRootMessage) channel.createMessage(TCMessageType.REQUEST_ROOT_MESSAGE);
  }

  public RequestRootMessageFactory getRequestRootMessageFactory() {
    return this;
  }

  public RequestManagedObjectMessage newRequestManagedObjectMessage(NodeID nodeID) {
    return (RequestManagedObjectMessage) channel.createMessage(TCMessageType.REQUEST_MANAGED_OBJECT_MESSAGE);
  }

  public RequestManagedObjectMessageFactory getRequestManagedObjectMessageFactory() {
    return this;
  }

  public AcknowledgeTransactionMessageFactory getAcknowledgeTransactionMessageFactory() {
    return this;
  }

  public AcknowledgeTransactionMessage newAcknowledgeTransactionMessage(NodeID remoteNode) {
    return (AcknowledgeTransactionMessage) channel.createMessage(TCMessageType.ACKNOWLEDGE_TRANSACTION_MESSAGE);
  }

  public ClientHandshakeMessage newClientHandshakeMessage(NodeID remoteNode) {
    ClientHandshakeMessage rv = (ClientHandshakeMessage) channel.createMessage(TCMessageType.CLIENT_HANDSHAKE_MESSAGE);
    return rv;
  }

  public ClientHandshakeMessageFactory getClientHandshakeMessageFactory() {
    return this;
  }

  public ObjectIDBatchRequestMessage newObjectIDBatchRequestMessage() {
    return (ObjectIDBatchRequestMessage) channel.createMessage(TCMessageType.OBJECT_ID_BATCH_REQUEST_MESSAGE);
  }

  public JMXMessage getJMXMessage() {
    return (JMXMessage) channel.createMessage(TCMessageType.JMX_MESSAGE);
  }

  public CompletedTransactionLowWaterMarkMessage newCompletedTransactionLowWaterMarkMessage(NodeID remoteID) {
    return (CompletedTransactionLowWaterMarkMessage) channel
        .createMessage(TCMessageType.COMPLETED_TRANSACTION_LOWWATERMARK_MESSAGE);
  }

  public ObjectIDBatchRequestMessageFactory getObjectIDBatchRequestMessageFactory() {
    return this;
  }

  public CommitTransactionMessage newCommitTransactionMessage(NodeID remoteNode) {
    return (CommitTransactionMessage) channel.createMessage(TCMessageType.COMMIT_TRANSACTION_MESSAGE);
  }

  public CommitTransactionMessageFactory getCommitTransactionMessageFactory() {
    return this;
  }

  public boolean isConnected() {
    return channel.isConnected();
  }

  public CompletedTransactionLowWaterMarkMessageFactory getCompletedTransactionLowWaterMarkMessageFactory() {
    return this;
  }

  public GroupID[] getGroupIDs() {
    return groups;
  }
}
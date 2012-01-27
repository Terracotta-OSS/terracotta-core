/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object;

import com.tc.management.lock.stats.LockStatisticsReponseMessageFactory;
import com.tc.management.lock.stats.LockStatisticsResponseMessage;
import com.tc.net.CommStackMismatchException;
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
import com.tc.object.msg.KeysForOrphanedValuesMessage;
import com.tc.object.msg.KeysForOrphanedValuesMessageFactory;
import com.tc.object.msg.LockRequestMessage;
import com.tc.object.msg.LockRequestMessageFactory;
import com.tc.object.msg.NodeMetaDataMessage;
import com.tc.object.msg.NodeMetaDataMessageFactory;
import com.tc.object.msg.NodesWithKeysMessage;
import com.tc.object.msg.NodesWithKeysMessageFactory;
import com.tc.object.msg.NodesWithObjectsMessage;
import com.tc.object.msg.NodesWithObjectsMessageFactory;
import com.tc.object.msg.ObjectIDBatchRequestMessage;
import com.tc.object.msg.ObjectIDBatchRequestMessageFactory;
import com.tc.object.msg.RequestManagedObjectMessage;
import com.tc.object.msg.RequestManagedObjectMessageFactory;
import com.tc.object.msg.RequestRootMessage;
import com.tc.object.msg.RequestRootMessageFactory;
import com.tc.object.msg.SearchQueryRequestMessage;
import com.tc.object.msg.SearchRequestMessageFactory;
import com.tc.object.msg.ServerMapMessageFactory;
import com.tc.object.msg.ServerMapRequestMessage;
import com.tc.object.net.DSOClientMessageChannel;
import com.tc.util.TCTimeoutException;

import java.io.IOException;
import java.net.UnknownHostException;

public class DSOClientMessageChannelImpl implements DSOClientMessageChannel, LockRequestMessageFactory,
    RequestRootMessageFactory, RequestManagedObjectMessageFactory, ClientHandshakeMessageFactory,
    ObjectIDBatchRequestMessageFactory, CommitTransactionMessageFactory, AcknowledgeTransactionMessageFactory,
    CompletedTransactionLowWaterMarkMessageFactory, NodesWithObjectsMessageFactory, ServerMapMessageFactory,
    KeysForOrphanedValuesMessageFactory, NodeMetaDataMessageFactory, LockStatisticsReponseMessageFactory,
    SearchRequestMessageFactory, NodesWithKeysMessageFactory {

  private final ClientMessageChannel channel;
  private final GroupID              groups[];
  private final ClientIDProvider     clientIDProvider;

  public DSOClientMessageChannelImpl(final ClientMessageChannel theChannel, final GroupID[] gids) {
    this.channel = theChannel;
    this.groups = gids;
    this.clientIDProvider = new ClientIDProviderImpl(theChannel.getChannelIDProvider());
  }

  public ClientIDProvider getClientIDProvider() {
    return this.clientIDProvider;
  }

  public void addListener(final ChannelEventListener listener) {
    this.channel.addListener(listener);
  }

  public ClientMessageChannel channel() {
    return this.channel;
  }

  public void open() throws TCTimeoutException, UnknownHostException, IOException, MaxConnectionsExceededException,
      CommStackMismatchException {
    this.channel.open();
  }

  public void close() {
    this.channel.close();
  }

  public LockRequestMessage newLockRequestMessage(final NodeID nodeID) {
    return (LockRequestMessage) this.channel.createMessage(TCMessageType.LOCK_REQUEST_MESSAGE);
  }

  public LockRequestMessageFactory getLockRequestMessageFactory() {
    return this;
  }

  public RequestRootMessage newRequestRootMessage(final NodeID nodeID) {
    return (RequestRootMessage) this.channel.createMessage(TCMessageType.REQUEST_ROOT_MESSAGE);
  }

  public RequestRootMessageFactory getRequestRootMessageFactory() {
    return this;
  }

  public RequestManagedObjectMessage newRequestManagedObjectMessage(final NodeID nodeID) {
    return (RequestManagedObjectMessage) this.channel.createMessage(TCMessageType.REQUEST_MANAGED_OBJECT_MESSAGE);
  }

  public RequestManagedObjectMessageFactory getRequestManagedObjectMessageFactory() {
    return this;
  }

  public ServerMapMessageFactory getServerMapMessageFactory() {
    return this;
  }

  public SearchRequestMessageFactory getSearchRequestMessageFactory() {
    return this;
  }

  public AcknowledgeTransactionMessageFactory getAcknowledgeTransactionMessageFactory() {
    return this;
  }

  public AcknowledgeTransactionMessage newAcknowledgeTransactionMessage(final NodeID remoteNode) {
    return (AcknowledgeTransactionMessage) this.channel.createMessage(TCMessageType.ACKNOWLEDGE_TRANSACTION_MESSAGE);
  }

  public ClientHandshakeMessage newClientHandshakeMessage(NodeID remoteNode, String clientVersion,
                                                          boolean isEnterpriseClient) {
    final ClientHandshakeMessage rv = (ClientHandshakeMessage) this.channel
        .createMessage(TCMessageType.CLIENT_HANDSHAKE_MESSAGE);
    rv.setClientVersion(clientVersion);
    rv.setEnterpriseClient(isEnterpriseClient);
    return rv;
  }

  public ClientHandshakeMessageFactory getClientHandshakeMessageFactory() {
    return this;
  }

  public NodesWithObjectsMessageFactory getNodesWithObjectsMessageFactory() {
    return this;
  }

  public KeysForOrphanedValuesMessageFactory getKeysForOrphanedValuesMessageFactory() {
    return this;
  }

  public NodeMetaDataMessageFactory getNodeMetaDataMessageFactory() {
    return this;
  }

  public NodesWithKeysMessageFactory getNodesWithKeysMessageFactory() {
    return this;
  }

  public ObjectIDBatchRequestMessage newObjectIDBatchRequestMessage() {
    return (ObjectIDBatchRequestMessage) this.channel.createMessage(TCMessageType.OBJECT_ID_BATCH_REQUEST_MESSAGE);
  }

  public CompletedTransactionLowWaterMarkMessage newCompletedTransactionLowWaterMarkMessage(final NodeID remoteID) {
    return (CompletedTransactionLowWaterMarkMessage) this.channel
        .createMessage(TCMessageType.COMPLETED_TRANSACTION_LOWWATERMARK_MESSAGE);
  }

  public ObjectIDBatchRequestMessageFactory getObjectIDBatchRequestMessageFactory() {
    return this;
  }

  public CommitTransactionMessage newCommitTransactionMessage(final NodeID remoteNode) {
    return (CommitTransactionMessage) this.channel.createMessage(TCMessageType.COMMIT_TRANSACTION_MESSAGE);
  }

  public CommitTransactionMessageFactory getCommitTransactionMessageFactory() {
    return this;
  }

  public NodesWithObjectsMessage newNodesWithObjectsMessage(final NodeID nodeID) {
    return (NodesWithObjectsMessage) this.channel.createMessage(TCMessageType.NODES_WITH_OBJECTS_MESSAGE);
  }

  public KeysForOrphanedValuesMessage newKeysForOrphanedValuesMessage(final NodeID nodeID) {
    return (KeysForOrphanedValuesMessage) this.channel.createMessage(TCMessageType.KEYS_FOR_ORPHANED_VALUES_MESSAGE);
  }

  public ServerMapRequestMessage newServerMapRequestMessage(final NodeID nodeID, final ServerMapRequestType type) {
    return type.createRequestMessage(this.channel);
  }

  public SearchQueryRequestMessage newSearchQueryRequestMessage(final NodeID remoteID) {
    return (SearchQueryRequestMessage) this.channel.createMessage(TCMessageType.SEARCH_QUERY_REQUEST_MESSAGE);
  }

  public NodeMetaDataMessage newNodeMetaDataMessage() {
    return (NodeMetaDataMessage) this.channel.createMessage(TCMessageType.NODE_META_DATA_MESSAGE);
  }

  public boolean isConnected() {
    return this.channel.isConnected();
  }

  public CompletedTransactionLowWaterMarkMessageFactory getCompletedTransactionLowWaterMarkMessageFactory() {
    return this;
  }

  public LockStatisticsResponseMessage newLockStatisticsResponseMessage(final NodeID remoteID) {
    return (LockStatisticsResponseMessage) this.channel.createMessage(TCMessageType.LOCK_STATISTICS_RESPONSE_MESSAGE);
  }

  public LockStatisticsReponseMessageFactory getLockStatisticsReponseMessageFactory() {
    return this;
  }

  public GroupID[] getGroupIDs() {
    return this.groups;
  }

  public NodesWithKeysMessage newNodesWithKeysMessage(final NodeID nodeID) {
    return (NodesWithKeysMessage) this.channel.createMessage(TCMessageType.NODES_WITH_KEYS_MESSAGE);
  }
}

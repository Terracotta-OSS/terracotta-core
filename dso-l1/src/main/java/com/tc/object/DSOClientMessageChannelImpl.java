/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object;

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
import com.tc.object.msg.SearchResultsRequestMessage;
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
    KeysForOrphanedValuesMessageFactory, NodeMetaDataMessageFactory,
    SearchRequestMessageFactory, NodesWithKeysMessageFactory {

  private final ClientMessageChannel channel;
  private final GroupID              groups[];
  private final ClientIDProvider     clientIDProvider;

  public DSOClientMessageChannelImpl(final ClientMessageChannel theChannel, final GroupID[] gids) {
    this.channel = theChannel;
    this.groups = gids;
    this.clientIDProvider = new ClientIDProviderImpl(theChannel.getChannelIDProvider());
  }

  @Override
  public ClientIDProvider getClientIDProvider() {
    return this.clientIDProvider;
  }

  @Override
  public void addListener(final ChannelEventListener listener) {
    this.channel.addListener(listener);
  }

  @Override
  public ClientMessageChannel channel() {
    return this.channel;
  }

  @Override
  public void open(final char[] pw) throws TCTimeoutException, UnknownHostException, IOException, MaxConnectionsExceededException,
      CommStackMismatchException {
    this.channel.open(pw);
  }

  @Override
  public void close() {
    this.channel.close();
  }

  @Override
  public LockRequestMessage newLockRequestMessage(final NodeID nodeID) {
    return (LockRequestMessage) this.channel.createMessage(TCMessageType.LOCK_REQUEST_MESSAGE);
  }

  @Override
  public LockRequestMessageFactory getLockRequestMessageFactory() {
    return this;
  }

  @Override
  public RequestRootMessage newRequestRootMessage(final NodeID nodeID) {
    return (RequestRootMessage) this.channel.createMessage(TCMessageType.REQUEST_ROOT_MESSAGE);
  }

  @Override
  public RequestRootMessageFactory getRequestRootMessageFactory() {
    return this;
  }

  @Override
  public RequestManagedObjectMessage newRequestManagedObjectMessage(final NodeID nodeID) {
    return (RequestManagedObjectMessage) this.channel.createMessage(TCMessageType.REQUEST_MANAGED_OBJECT_MESSAGE);
  }

  @Override
  public RequestManagedObjectMessageFactory getRequestManagedObjectMessageFactory() {
    return this;
  }

  @Override
  public ServerMapMessageFactory getServerMapMessageFactory() {
    return this;
  }

  @Override
  public SearchRequestMessageFactory getSearchRequestMessageFactory() {
    return this;
  }

  @Override
  public AcknowledgeTransactionMessageFactory getAcknowledgeTransactionMessageFactory() {
    return this;
  }

  @Override
  public AcknowledgeTransactionMessage newAcknowledgeTransactionMessage(final NodeID remoteNode) {
    return (AcknowledgeTransactionMessage) this.channel.createMessage(TCMessageType.ACKNOWLEDGE_TRANSACTION_MESSAGE);
  }

  @Override
  public ClientHandshakeMessage newClientHandshakeMessage(NodeID remoteNode, String clientVersion,
                                                          boolean isEnterpriseClient) {
    final ClientHandshakeMessage rv = (ClientHandshakeMessage) this.channel
        .createMessage(TCMessageType.CLIENT_HANDSHAKE_MESSAGE);
    rv.setClientVersion(clientVersion);
    rv.setEnterpriseClient(isEnterpriseClient);
    return rv;
  }

  @Override
  public ClientHandshakeMessageFactory getClientHandshakeMessageFactory() {
    return this;
  }

  @Override
  public NodesWithObjectsMessageFactory getNodesWithObjectsMessageFactory() {
    return this;
  }

  @Override
  public KeysForOrphanedValuesMessageFactory getKeysForOrphanedValuesMessageFactory() {
    return this;
  }

  @Override
  public NodeMetaDataMessageFactory getNodeMetaDataMessageFactory() {
    return this;
  }

  @Override
  public NodesWithKeysMessageFactory getNodesWithKeysMessageFactory() {
    return this;
  }

  @Override
  public ObjectIDBatchRequestMessage newObjectIDBatchRequestMessage() {
    return (ObjectIDBatchRequestMessage) this.channel.createMessage(TCMessageType.OBJECT_ID_BATCH_REQUEST_MESSAGE);
  }

  @Override
  public CompletedTransactionLowWaterMarkMessage newCompletedTransactionLowWaterMarkMessage(final NodeID remoteID) {
    return (CompletedTransactionLowWaterMarkMessage) this.channel
        .createMessage(TCMessageType.COMPLETED_TRANSACTION_LOWWATERMARK_MESSAGE);
  }

  @Override
  public ObjectIDBatchRequestMessageFactory getObjectIDBatchRequestMessageFactory() {
    return this;
  }

  @Override
  public CommitTransactionMessage newCommitTransactionMessage(final NodeID remoteNode) {
    return (CommitTransactionMessage) this.channel.createMessage(TCMessageType.COMMIT_TRANSACTION_MESSAGE);
  }

  @Override
  public CommitTransactionMessageFactory getCommitTransactionMessageFactory() {
    return this;
  }

  @Override
  public NodesWithObjectsMessage newNodesWithObjectsMessage(final NodeID nodeID) {
    return (NodesWithObjectsMessage) this.channel.createMessage(TCMessageType.NODES_WITH_OBJECTS_MESSAGE);
  }

  @Override
  public KeysForOrphanedValuesMessage newKeysForOrphanedValuesMessage(final NodeID nodeID) {
    return (KeysForOrphanedValuesMessage) this.channel.createMessage(TCMessageType.KEYS_FOR_ORPHANED_VALUES_MESSAGE);
  }

  @Override
  public ServerMapRequestMessage newServerMapRequestMessage(final NodeID nodeID, final ServerMapRequestType type) {
    return type.createRequestMessage(this.channel);
  }

  @Override
  public SearchQueryRequestMessage newSearchQueryRequestMessage(final NodeID remoteID) {
    return (SearchQueryRequestMessage) this.channel.createMessage(TCMessageType.SEARCH_QUERY_REQUEST_MESSAGE);
  }

  @Override
  public SearchResultsRequestMessage newSearchResultsRequestMessage(NodeID nodeID) {
    return (SearchResultsRequestMessage) this.channel.createMessage(TCMessageType.SEARCH_RESULTS_REQUEST_MESSAGE);
  }

  @Override
  public NodeMetaDataMessage newNodeMetaDataMessage() {
    return (NodeMetaDataMessage) this.channel.createMessage(TCMessageType.NODE_META_DATA_MESSAGE);
  }

  @Override
  public boolean isConnected() {
    return this.channel.isConnected();
  }

  @Override
  public CompletedTransactionLowWaterMarkMessageFactory getCompletedTransactionLowWaterMarkMessageFactory() {
    return this;
  }

  @Override
  public GroupID[] getGroupIDs() {
    return this.groups;
  }

  @Override
  public NodesWithKeysMessage newNodesWithKeysMessage(final NodeID nodeID) {
    return (NodesWithKeysMessage) this.channel.createMessage(TCMessageType.NODES_WITH_KEYS_MESSAGE);
  }

}
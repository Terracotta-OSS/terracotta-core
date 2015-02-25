/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.event;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.SetMultimap;
import com.tc.async.api.ConfigurationContext;
import com.tc.async.api.PostInit;
import com.tc.async.api.Sink;
import com.tc.l2.objectserver.ServerTransactionFactory;
import com.tc.net.ClientID;
import com.tc.net.NodeID;
import com.tc.net.groups.GroupManager;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.object.ObjectID;
import com.tc.object.dna.impl.ObjectStringSerializer;
import com.tc.object.dna.impl.ObjectStringSerializerImpl;
import com.tc.object.net.DSOChannelManager;
import com.tc.object.net.DSOChannelManagerEventListener;
import com.tc.object.tx.ServerTransactionID;
import com.tc.objectserver.context.ServerTransactionCompleteContext;
import com.tc.objectserver.core.api.ServerConfigurationContext;
import com.tc.objectserver.impl.ServerTransactionBatchContext;
import com.tc.objectserver.tx.AbstractServerTransactionListener;
import com.tc.objectserver.tx.ServerTransaction;
import com.tc.objectserver.tx.TransactionBatchContext;
import com.tc.objectserver.tx.TransactionBatchManager;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ClientChannelMonitorImpl extends AbstractServerTransactionListener implements ClientChannelMonitor, DSOChannelManagerEventListener, PostInit {
  private final Multimap<ClientID, ObjectID> subscriberMap;
  private final DSOChannelManager            channelManager;
  private final ServerTransactionFactory     serverTransactionFactory;
  private GroupManager                       groupManager;
  private TransactionBatchManager            transactionBatchManager;
  
  private final Set<ServerTransactionID> channelRemovalTransactions = Collections.newSetFromMap(new ConcurrentHashMap<ServerTransactionID, Boolean>());
  private Sink lwmSink;

  public ClientChannelMonitorImpl(final DSOChannelManager channelManager,
                                  final ServerTransactionFactory serverTransactionFactory) {
    SetMultimap<ClientID, ObjectID> setMultimap = HashMultimap.create();
    this.subscriberMap = Multimaps.synchronizedSetMultimap(setMultimap);
    this.serverTransactionFactory = serverTransactionFactory;
    this.channelManager = channelManager;
    this.channelManager.addEventListener(this);
  }

  @Override
  public void initializeContext(final ConfigurationContext context) {
    final ServerConfigurationContext scc = (ServerConfigurationContext) context;
    this.groupManager = scc.getL2Coordinator().getGroupManager();
    this.transactionBatchManager = scc.getTransactionBatchManager();
    scc.getTransactionManager().addTransactionListener(this);
    lwmSink = scc.getStage(ServerConfigurationContext.TRANSACTION_LOWWATERMARK_STAGE).getSink();
  }

  @Override
  public void monitorClient(ClientID clientToBeMonitored, ObjectID subscriber) {
    subscriberMap.put(clientToBeMonitored, subscriber);
  }

  @Override
  public void channelCreated(MessageChannel channel) {
    // DO Nothing
  }

  @Override
  public void channelRemoved(MessageChannel channel) {
     final ClientID clientID = channelManager.getClientIDFor(channel.getChannelID());
     if (clientID != null) {
      Collection<ObjectID> subscribedOIDs = subscriberMap.removeAll(clientID);

      informClientDisconnectionToSubscribers(clientID, subscribedOIDs);
    }
  }

  @Override
  public void transactionCompleted(ServerTransactionID stxID) {
    if (channelRemovalTransactions.remove(stxID)) {
      lwmSink.add(new ServerTransactionCompleteContext(stxID));
    }
  }

  private void informClientDisconnectionToSubscribers(final ClientID clientID, final Collection<ObjectID> subscribedOIDs) {
    NodeID localNodeID = groupManager.getLocalNodeID();
    ObjectStringSerializer serializer = new ObjectStringSerializerImpl();

    for (ObjectID objectID : subscribedOIDs) {
      ServerTransaction serverTransaction = serverTransactionFactory
          .createRemoveEventListeningClientTransaction(objectID, clientID, serializer);

      channelRemovalTransactions.add(serverTransaction.getServerTransactionID());
      
      TransactionBatchContext batchContext = new ServerTransactionBatchContext(localNodeID, serverTransaction, serializer);
      transactionBatchManager.processTransactions(batchContext);
    }
  }

}

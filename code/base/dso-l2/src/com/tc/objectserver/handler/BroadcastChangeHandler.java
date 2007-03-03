/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.handler;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.ConfigurationContext;
import com.tc.async.api.EventContext;
import com.tc.async.api.EventHandlerException;
import com.tc.async.api.Sink;
import com.tc.net.protocol.tcm.ChannelID;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.object.ObjectRequestID;
import com.tc.object.dmi.DmiDescriptor;
import com.tc.object.msg.BatchTransactionAcknowledgeMessage;
import com.tc.object.msg.BroadcastTransactionMessage;
import com.tc.object.net.DSOChannelManager;
import com.tc.object.net.NoSuchChannelException;
import com.tc.object.tx.TransactionID;
import com.tc.object.tx.TxnBatchID;
import com.tc.objectserver.context.BroadcastChangeContext;
import com.tc.objectserver.context.ManagedObjectRequestContext;
import com.tc.objectserver.core.api.ServerConfigurationContext;
import com.tc.objectserver.l1.api.ClientStateManager;
import com.tc.objectserver.tx.NoSuchBatchException;
import com.tc.objectserver.tx.ServerTransactionManager;
import com.tc.objectserver.tx.TransactionBatchManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Broadcast the change to all connected clients
 * 
 * @author steve
 */
public class BroadcastChangeHandler extends AbstractEventHandler {
  private DSOChannelManager             channelManager;
  private ClientStateManager            clientStateManager;
  private ServerTransactionManager      transactionManager;
  private Sink                          managedObjectRequestSink;
  private Sink                          respondObjectRequestSink;
  private final TransactionBatchManager transactionBatchManager;

  public BroadcastChangeHandler(TransactionBatchManager transactionBatchManager) {
    this.transactionBatchManager = transactionBatchManager;
  }

  public void handleEvent(EventContext context) throws EventHandlerException {
    BroadcastChangeContext bcc = (BroadcastChangeContext) context;

    final ChannelID committerID = bcc.getChannelID();
    final TransactionID txnID = bcc.getTransactionID();

    final MessageChannel[] channels = channelManager.getActiveChannels();

    for (int i = 0; i < channels.length; i++) {
      MessageChannel client = channels[i];
      ChannelID clientID = client.getChannelID();

      Map newRoots = bcc.getNewRoots();
      Set notifiedWaiters = bcc.getNewlyPendingWaiters().getNotifiedFor(clientID);
      List prunedChanges = Collections.EMPTY_LIST;
      Set lookupObjectIDs = new HashSet();

      if (!clientID.equals(committerID)) {
        prunedChanges = clientStateManager.createPrunedChangesAndAddObjectIDTo(bcc.getChanges(), bcc.getIncludeIDs(),
                                                                               clientID, lookupObjectIDs);
      }

      DmiDescriptor[] prunedDmis = pruneDmiDescriptors(bcc.getDmiDescriptors(), clientID, clientStateManager);
      final boolean includeDmi = !clientID.equals(committerID) && prunedDmis.length > 0;
      if (!prunedChanges.isEmpty() || !lookupObjectIDs.isEmpty() || !notifiedWaiters.isEmpty() || !newRoots.isEmpty()
          || includeDmi) {
        transactionManager.addWaitingForAcknowledgement(committerID, txnID, clientID);
        if (lookupObjectIDs.size() > 0) {
          // TODO:: Request ID is not used anywhere. RemoveIT.
          // XXX:: It is important to keep the maxReachableSize to <= 0 so that we dont go into recursive lookups @see
          // ObjectManagerImpl
          this.managedObjectRequestSink.add(new ManagedObjectRequestContext(clientID, ObjectRequestID.NULL_ID,
                                                                            lookupObjectIDs, -1,
                                                                            this.respondObjectRequestSink));
        }
        final DmiDescriptor[] dmi = (includeDmi) ? prunedDmis : DmiDescriptor.EMPTY_ARRAY;
        BroadcastTransactionMessage responseMessage = (BroadcastTransactionMessage) client
            .createMessage(TCMessageType.BROADCAST_TRANSACTION_MESSAGE);
        responseMessage.initialize(prunedChanges, lookupObjectIDs, bcc.getSerializer(), bcc.getLockIDs(),
                                   getNextChangeIDFor(clientID), txnID, committerID, bcc.getGlobalTransactionID(), bcc
                                       .getTransactionType(), bcc.getLowGlobalTransactionIDWatermark(),
                                   notifiedWaiters, newRoots, dmi);

        responseMessage.send();
      }
    }
    transactionManager.broadcasted(committerID, txnID);
    try {
      TxnBatchID batchID = bcc.getBatchID();
      if (transactionBatchManager.batchComponentComplete(committerID, batchID, txnID)) {
        try {
          BatchTransactionAcknowledgeMessage msg = channelManager.newBatchTransactionAcknowledgeMessage(committerID);
          msg.initialize(batchID);
          msg.send();
        } catch (NoSuchChannelException e) {
          getLogger().warn("Can't send transaction batch acknowledge message to unconnected client: " + committerID);
        }
      }
    } catch (NoSuchBatchException e) {
      throw new EventHandlerException(e);
    }
  }

  private static DmiDescriptor[] pruneDmiDescriptors(DmiDescriptor[] dmiDescriptors, ChannelID clientID,
                                                     ClientStateManager clientStateManager) {
    List list = new ArrayList();
    for (int i = 0; i < dmiDescriptors.length; i++) {
      DmiDescriptor dd = dmiDescriptors[i];
      if (clientStateManager.hasReference(clientID, dd.getReceiverId())) list.add(dd);
    }
    DmiDescriptor[] rv = new DmiDescriptor[list.size()];
    list.toArray(rv);
    return rv;
  }

  private synchronized long getNextChangeIDFor(ChannelID id) {
    // FIXME Fix this facility. Should keep a counter for every client and
    // increment on every
    return 0;
  }

  public void initialize(ConfigurationContext context) {
    super.initialize(context);
    ServerConfigurationContext scc = (ServerConfigurationContext) context;
    this.channelManager = scc.getChannelManager();
    this.clientStateManager = scc.getClientStateManager();
    this.transactionManager = scc.getTransactionManager();
    this.managedObjectRequestSink = scc.getStage(ServerConfigurationContext.MANAGED_OBJECT_REQUEST_STAGE).getSink();
    this.respondObjectRequestSink = scc.getStage(ServerConfigurationContext.RESPOND_TO_OBJECT_REQUEST_STAGE).getSink();
  }
}

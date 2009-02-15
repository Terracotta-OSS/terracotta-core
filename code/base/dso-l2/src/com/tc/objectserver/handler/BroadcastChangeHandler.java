/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.handler;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.ConfigurationContext;
import com.tc.async.api.EventContext;
import com.tc.async.api.Sink;
import com.tc.net.ClientID;
import com.tc.net.NodeID;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.object.ObjectRequestID;
import com.tc.object.dmi.DmiDescriptor;
import com.tc.object.dna.api.DNA;
import com.tc.object.msg.BroadcastTransactionMessage;
import com.tc.object.net.DSOChannelManager;
import com.tc.object.tx.TransactionID;
import com.tc.objectserver.context.BroadcastChangeContext;
import com.tc.objectserver.context.ObjectRequestServerContextImpl;
import com.tc.objectserver.core.api.ServerConfigurationContext;
import com.tc.objectserver.l1.api.ClientStateManager;
import com.tc.objectserver.mgmt.ObjectStatsRecorder;
import com.tc.objectserver.tx.ServerTransactionManager;
import com.tc.stats.counter.sampled.SampledCounter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Broadcast the change to all connected clients
 */
public class BroadcastChangeHandler extends AbstractEventHandler {

  private DSOChannelManager         channelManager;
  private ClientStateManager        clientStateManager;
  private ServerTransactionManager  transactionManager;
  private Sink                      managedObjectRequestSink;
  private final ObjectStatsRecorder objectStatsRecorder;

  private final SampledCounter      broadcastCounter;
  private final SampledCounter      changeCounter;

  public BroadcastChangeHandler(SampledCounter broadcastCounter, SampledCounter changeCounter,
                                ObjectStatsRecorder objectStatsRecorder) {
    this.broadcastCounter = broadcastCounter;
    this.changeCounter = changeCounter;
    this.objectStatsRecorder = objectStatsRecorder;
  }

  @Override
  public void handleEvent(EventContext context) {
    BroadcastChangeContext bcc = (BroadcastChangeContext) context;

    final NodeID committerID = bcc.getNodeID();
    final TransactionID txnID = bcc.getTransactionID();

    final MessageChannel[] channels = this.channelManager.getActiveChannels();

    for (int i = 0; i < channels.length; i++) {
      MessageChannel client = channels[i];
      // TODO:: make message channel return clientID and short channelManager call.
      ClientID clientID = this.channelManager.getClientIDFor(client.getChannelID());

      Map newRoots = bcc.getNewRoots();
      Set notifiedWaiters = bcc.getNewlyPendingWaiters().getNotifiedFor(clientID);
      List prunedChanges = Collections.EMPTY_LIST;
      TreeSet lookupObjectIDs = new TreeSet();

      if (!clientID.equals(committerID)) {
        prunedChanges = this.clientStateManager.createPrunedChangesAndAddObjectIDTo(bcc.getChanges(), bcc
            .getIncludeIDs(), clientID, lookupObjectIDs);
      }

      if (this.objectStatsRecorder.getBroadcastDebug()) {
        updateStats(prunedChanges);
      }

      DmiDescriptor[] prunedDmis = pruneDmiDescriptors(bcc.getDmiDescriptors(), clientID, this.clientStateManager);
      final boolean includeDmi = !clientID.equals(committerID) && prunedDmis.length > 0;
      if (!prunedChanges.isEmpty() || !lookupObjectIDs.isEmpty() || !notifiedWaiters.isEmpty() || !newRoots.isEmpty()
          || includeDmi) {
        this.transactionManager.addWaitingForAcknowledgement(committerID, txnID, clientID);
        if (lookupObjectIDs.size() > 0) {
          this.managedObjectRequestSink.add(new ObjectRequestServerContextImpl(clientID, ObjectRequestID.NULL_ID,
                                                                               lookupObjectIDs, Thread.currentThread()
                                                                                   .getName(), -1, true));
        }
        final DmiDescriptor[] dmi = (includeDmi) ? prunedDmis : DmiDescriptor.EMPTY_ARRAY;
        BroadcastTransactionMessage responseMessage = (BroadcastTransactionMessage) client
            .createMessage(TCMessageType.BROADCAST_TRANSACTION_MESSAGE);
        responseMessage.initialize(prunedChanges, bcc.getSerializer(), bcc.getLockIDs(), getNextChangeIDFor(clientID),
                                   txnID, committerID, bcc.getGlobalTransactionID(), bcc.getTransactionType(), bcc
                                       .getLowGlobalTransactionIDWatermark(), notifiedWaiters, newRoots, dmi);

        responseMessage.send();

        this.broadcastCounter.increment();
        this.changeCounter.increment(prunedChanges.size());
      }
    }
    this.transactionManager.broadcasted(committerID, txnID);
  }

  private void updateStats(List prunedChanges) {
    for (Iterator i = prunedChanges.iterator(); i.hasNext();) {
      DNA dna = (DNA) i.next();
      String className = dna.getTypeName();
      if (className == null) {
        className = "UNKNOWN"; // Could happen on restart scenario
      }
      this.objectStatsRecorder.updateBroadcastStats(className);
    }
  }

  private static DmiDescriptor[] pruneDmiDescriptors(DmiDescriptor[] dmiDescriptors, ClientID clientID,
                                                     ClientStateManager clientStateManager) {
    if (dmiDescriptors.length == 0) { return dmiDescriptors; }

    List list = new ArrayList();
    for (int i = 0; i < dmiDescriptors.length; i++) {
      DmiDescriptor dd = dmiDescriptors[i];
      if (dd.isFaultReceiver() || clientStateManager.hasReference(clientID, dd.getReceiverId())) {
        list.add(dd);
      }
    }
    DmiDescriptor[] rv = new DmiDescriptor[list.size()];
    list.toArray(rv);
    return rv;
  }

  private long getNextChangeIDFor(ClientID clientID) {
    // FIXME Fix this facility. Should keep a counter for every client and
    // increment on every
    return 0;
  }

  @Override
  protected void initialize(ConfigurationContext context) {
    ServerConfigurationContext scc = (ServerConfigurationContext) context;
    this.channelManager = scc.getChannelManager();
    this.clientStateManager = scc.getClientStateManager();
    this.transactionManager = scc.getTransactionManager();
    this.managedObjectRequestSink = scc.getStage(ServerConfigurationContext.MANAGED_OBJECT_REQUEST_STAGE).getSink();
  }
}

/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.handler;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.ConfigurationContext;
import com.tc.async.api.EventContext;
import com.tc.async.api.Sink;
import com.tc.invalidation.Invalidations;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.ClientID;
import com.tc.net.NodeID;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.object.ObjectID;
import com.tc.object.ObjectRequestID;
import com.tc.object.ObjectRequestServerContext.LOOKUP_STATE;
import com.tc.object.dmi.DmiDescriptor;
import com.tc.object.dna.api.DNA;
import com.tc.object.msg.BroadcastTransactionMessage;
import com.tc.object.net.DSOChannelManager;
import com.tc.object.tx.TransactionID;
import com.tc.objectserver.context.BroadcastChangeContext;
import com.tc.objectserver.context.ObjectRequestServerContextImpl;
import com.tc.objectserver.core.api.ServerConfigurationContext;
import com.tc.objectserver.l1.api.ClientStateManager;
import com.tc.objectserver.l1.api.InvalidateObjectManager;
import com.tc.objectserver.mgmt.ObjectStatsRecorder;
import com.tc.objectserver.tx.ServerTransactionManager;
import com.tc.stats.counter.sampled.SampledCounter;
import com.tc.stats.counter.sampled.derived.SampledRateCounter;
import com.tc.util.ObjectIDSet;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;

/**
 * Broadcast the change to all connected clients
 */
public class BroadcastChangeHandler extends AbstractEventHandler {
  private static final TCLogger         logger = TCLogging.getLogger(BroadcastChangeHandler.class);

  private DSOChannelManager             channelManager;
  private ClientStateManager            clientStateManager;
  private ServerTransactionManager      transactionManager;
  private Sink                          managedObjectRequestSink;
  private final ObjectStatsRecorder     objectStatsRecorder;

  private final SampledCounter          broadcastCounter;
  private final SampledRateCounter      changesPerBroadcast;
  private final InvalidateObjectManager invalidateObjMgr;

  public BroadcastChangeHandler(final SampledCounter broadcastCounter, final ObjectStatsRecorder objectStatsRecorder,
                                final SampledRateCounter changesPerBroadcast, InvalidateObjectManager invalidateObjMgr) {
    this.broadcastCounter = broadcastCounter;
    this.objectStatsRecorder = objectStatsRecorder;
    this.changesPerBroadcast = changesPerBroadcast;
    this.invalidateObjMgr = invalidateObjMgr;
  }

  @Override
  public void handleEvent(final EventContext context) {
    final BroadcastChangeContext bcc = (BroadcastChangeContext) context;

    final NodeID committerID = bcc.getNodeID();
    final TransactionID txnID = bcc.getTransactionID();

    final MessageChannel[] channels = this.channelManager.getActiveChannels();

    for (final MessageChannel client : channels) {
      // TODO:: make message channel return clientID and short channelManager call.
      final ClientID clientID = this.channelManager.getClientIDFor(client.getChannelID());

      final Map newRoots = bcc.getNewRoots();
      final Set notifiedWaiters = bcc.getNewlyPendingWaiters().getNotifiedFor(clientID);
      List prunedChanges = Collections.EMPTY_LIST;
      final SortedSet<ObjectID> lookupObjectIDs = new ObjectIDSet();
      final Invalidations invalidateObjectIDs = new Invalidations();

      if (!clientID.equals(committerID)) {
        prunedChanges = this.clientStateManager.createPrunedChangesAndAddObjectIDTo(bcc.getChanges(), bcc
            .getApplyInfo(), clientID, lookupObjectIDs, invalidateObjectIDs);
      }

      if (!invalidateObjectIDs.isEmpty()) {
        invalidateObjMgr.invalidateObjectFor(clientID, invalidateObjectIDs);
      }

      if (this.objectStatsRecorder.getBroadcastDebug()) {
        updateStats(prunedChanges);
      }

      final DmiDescriptor[] prunedDmis = pruneDmiDescriptors(bcc.getDmiDescriptors(), clientID, this.clientStateManager);
      final boolean includeDmi = !clientID.equals(committerID) && prunedDmis.length > 0;
      if (!prunedChanges.isEmpty() || !lookupObjectIDs.isEmpty() || !notifiedWaiters.isEmpty() || !newRoots.isEmpty()
          || includeDmi) {
        this.transactionManager.addWaitingForAcknowledgement(committerID, txnID, clientID);

        // check here if the client is already not disconnected
        // if it is then we remove the clientID from the list of clients to acknowledge back
        // otherwise the committerID will never receive the acknowledgment from the server
        if (client.isClosed()) {
          this.transactionManager.acknowledgement(committerID, txnID, clientID);
          continue;
        }

        if (lookupObjectIDs.size() > 0) {
          this.managedObjectRequestSink.add(new ObjectRequestServerContextImpl(clientID, ObjectRequestID.NULL_ID,
                                                                               lookupObjectIDs, Thread.currentThread()
                                                                                   .getName(), -1,
                                                                               LOOKUP_STATE.SERVER_INITIATED));
        }
        final DmiDescriptor[] dmi = (includeDmi) ? prunedDmis : DmiDescriptor.EMPTY_ARRAY;
        final BroadcastTransactionMessage responseMessage = (BroadcastTransactionMessage) client
            .createMessage(TCMessageType.BROADCAST_TRANSACTION_MESSAGE);
        responseMessage.initialize(prunedChanges, bcc.getSerializer(), bcc.getLockIDs(), getNextChangeIDFor(clientID),
                                   txnID, committerID, bcc.getGlobalTransactionID(), bcc.getTransactionType(), bcc
                                       .getLowGlobalTransactionIDWatermark(), notifiedWaiters, newRoots, dmi);

        responseMessage.send();

        if (logger.isDebugEnabled() && !notifiedWaiters.isEmpty()) {
          logger.debug("Notified waiters " + clientID + " " + notifiedWaiters);
        }

        this.broadcastCounter.increment();
        // changesPerBroadcast = number of changes/number of broadcasts
        this.changesPerBroadcast.increment(prunedChanges.size(), 1);
      }
    }
    this.transactionManager.broadcasted(committerID, txnID);
  }

  private void updateStats(final List prunedChanges) {
    for (final Iterator i = prunedChanges.iterator(); i.hasNext();) {
      final DNA dna = (DNA) i.next();
      String className = dna.getTypeName();
      if (className == null) {
        className = "UNKNOWN"; // Could happen on restart scenario
      }
      this.objectStatsRecorder.updateBroadcastStats(className);
    }
  }

  private static DmiDescriptor[] pruneDmiDescriptors(final DmiDescriptor[] dmiDescriptors, final ClientID clientID,
                                                     final ClientStateManager clientStateManager) {
    if (dmiDescriptors.length == 0) { return dmiDescriptors; }

    final List list = new ArrayList();
    for (final DmiDescriptor dd : dmiDescriptors) {
      if (dd.isFaultReceiver() || clientStateManager.hasReference(clientID, dd.getReceiverId())) {
        list.add(dd);
      }
    }
    final DmiDescriptor[] rv = new DmiDescriptor[list.size()];
    list.toArray(rv);
    return rv;
  }

  private long getNextChangeIDFor(final ClientID clientID) {
    // FIXME Fix this facility. Should keep a counter for every client and
    // increment on every
    return 0;
  }

  @Override
  protected void initialize(final ConfigurationContext context) {
    final ServerConfigurationContext scc = (ServerConfigurationContext) context;
    this.channelManager = scc.getChannelManager();
    this.clientStateManager = scc.getClientStateManager();
    this.transactionManager = scc.getTransactionManager();
    this.managedObjectRequestSink = scc.getStage(ServerConfigurationContext.MANAGED_OBJECT_REQUEST_STAGE).getSink();
  }
}

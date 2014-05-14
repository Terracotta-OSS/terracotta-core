/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.handler;

import com.google.common.collect.Multimap;
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
import com.tc.object.dna.api.DNA;
import com.tc.object.dna.api.LogicalChangeID;
import com.tc.object.dna.api.LogicalChangeResult;
import com.tc.object.msg.BroadcastTransactionMessage;
import com.tc.object.net.DSOChannelManager;
import com.tc.object.tx.TransactionID;
import com.tc.objectserver.context.BroadcastChangeContext;
import com.tc.objectserver.context.ObjectRequestServerContextImpl;
import com.tc.objectserver.core.api.ServerConfigurationContext;
import com.tc.objectserver.l1.api.ClientStateManager;
import com.tc.objectserver.l1.api.InvalidateObjectManager;
import com.tc.objectserver.mgmt.ObjectStatsRecorder;
import com.tc.objectserver.tx.BroadcastDurabilityLevel;
import com.tc.objectserver.tx.ServerTransactionManager;
import com.tc.properties.TCPropertiesImpl;
import com.tc.server.ServerEvent;
import com.tc.stats.counter.sampled.SampledCounter;
import com.tc.stats.counter.sampled.derived.SampledRateCounter;
import com.tc.util.BitSetObjectIDSet;

import java.util.Collection;
import java.util.Collections;
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
  private final BroadcastDurabilityLevel broadcastDurabilityLevel;

  public BroadcastChangeHandler(final SampledCounter broadcastCounter, final ObjectStatsRecorder objectStatsRecorder,
                                final SampledRateCounter changesPerBroadcast, InvalidateObjectManager invalidateObjMgr) {
    this.broadcastCounter = broadcastCounter;
    this.objectStatsRecorder = objectStatsRecorder;
    this.changesPerBroadcast = changesPerBroadcast;
    this.invalidateObjMgr = invalidateObjMgr;
    this.broadcastDurabilityLevel = BroadcastDurabilityLevel.getFromProperties(TCPropertiesImpl.getProperties());
  }

  @Override
  public void handleEvent(final EventContext context) {
    final BroadcastChangeContext bcc = (BroadcastChangeContext) context;
    final NodeID committerID = bcc.getNodeID();
    final TransactionID txnID = bcc.getTransactionID();
    final MessageChannel[] channels = this.channelManager.getActiveChannels();

    final Multimap<ClientID, ServerEvent> serverEventsPerClient = bcc.getApplyInfo()
        .getServerEventBuffer().getServerEventsPerClient(bcc.getGlobalTransactionID());

    if (bcc.getApplyInfo().getApplyResultRecorder().needPersist()) {
      if (broadcastDurabilityLevel.isWaitForCommit()) {
        transactionManager.waitForTransactionCommit(bcc.getServerTransactionID());
      }
      if (broadcastDurabilityLevel.isWaitForRelay()) {
        transactionManager.waitForTransactionRelay(bcc.getServerTransactionID());
      }
    }

    for (final MessageChannel client : channels) {
      // TODO:: make message channel return clientID and short channelManager call.
      final ClientID clientID = this.channelManager.getClientIDFor(client.getChannelID());

      final Map newRoots = bcc.getNewRoots();
      final Set notifiedWaiters = bcc.getNewlyPendingWaiters().getNotifiedFor(clientID);
      List<DNA> prunedChanges;
      final SortedSet<ObjectID> lookupObjectIDs = new BitSetObjectIDSet();
      final Invalidations invalidateObjectIDs = new Invalidations();

      if (!clientID.equals(committerID) || !bcc.getApplyInfo().getObjectsToEchoChangesFor().isEmpty()) {
        prunedChanges = this.clientStateManager.createPrunedChangesAndAddObjectIDTo(bcc.getChanges(),
            bcc.getApplyInfo(), clientID, lookupObjectIDs, invalidateObjectIDs);
      }  else {
        prunedChanges = Collections.emptyList();
      }

      Map<LogicalChangeID, LogicalChangeResult> logicalChangeResults = (clientID.equals(committerID) ?
          bcc.getApplyInfo().getApplyResultRecorder().getResults() :
          Collections.<LogicalChangeID, LogicalChangeResult>emptyMap());

      Collection<ServerEvent> serverEvents = serverEventsPerClient.get(clientID);
      if (serverEvents == null) {
        serverEvents = Collections.emptyList();
      }

      if (!invalidateObjectIDs.isEmpty()) {
        invalidateObjMgr.invalidateObjectFor(clientID, invalidateObjectIDs);
      }

      if (this.objectStatsRecorder.getBroadcastDebug()) {
        updateStats(prunedChanges);
      }

      if (!prunedChanges.isEmpty() || !lookupObjectIDs.isEmpty() || !notifiedWaiters.isEmpty() || !newRoots.isEmpty()
          || !logicalChangeResults.isEmpty() || !serverEvents.isEmpty()) {
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

        final BroadcastTransactionMessage responseMessage = (BroadcastTransactionMessage) client
            .createMessage(TCMessageType.BROADCAST_TRANSACTION_MESSAGE);
        responseMessage.initialize(prunedChanges, bcc.getSerializer(), bcc.getLockIDs(), getNextChangeIDFor(clientID),
                                   txnID, committerID, bcc.getGlobalTransactionID(), bcc.getTransactionType(),
                                   bcc.getLowGlobalTransactionIDWatermark(), notifiedWaiters, newRoots,
                                   logicalChangeResults, serverEvents);

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
    if (bcc.getServerTransactionID().isServerGeneratedTransaction()) {
      bcc.getApplyInfo().getServerEventBuffer().removeEventsForTransaction(bcc.getGlobalTransactionID());
    }
  }

  private void updateStats(final List prunedChanges) {
    for (final Object prunedChange : prunedChanges) {
      final DNA dna = (DNA) prunedChange;
      String className = dna.getTypeName();
      if (className == null) {
        className = "UNKNOWN"; // Could happen on restart scenario
      }
      this.objectStatsRecorder.updateBroadcastStats(className);
    }
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

/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.handler;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.ConfigurationContext;
import com.tc.async.api.EventContext;
import com.tc.async.api.Sink;
import com.tc.exception.TCClassNotFoundException;
import com.tc.exception.TCNotRunningException;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.NodeID;
import com.tc.object.ClientConfigurationContext;
import com.tc.object.context.ServerEventDeliveryContext;
import com.tc.object.dna.api.LogicalChangeID;
import com.tc.object.dna.api.LogicalChangeResult;
import com.tc.object.gtx.ClientGlobalTransactionManager;
import com.tc.object.gtx.GlobalTransactionID;
import com.tc.object.locks.ClientLockManager;
import com.tc.object.locks.ClientServerExchangeLockContext;
import com.tc.object.msg.AcknowledgeTransactionMessage;
import com.tc.object.msg.AcknowledgeTransactionMessageFactory;
import com.tc.object.msg.BroadcastTransactionMessage;
import com.tc.object.msg.BroadcastTransactionMessageImpl;
import com.tc.object.session.SessionManager;
import com.tc.object.tx.ClientTransactionManager;
import com.tc.server.ServerEvent;

import java.util.Collection;
import java.util.Map;

/**
 * @author steve
 */
public class ReceiveTransactionHandler extends AbstractEventHandler {

  private static final TCLogger logger = TCLogging.getLogger(ReceiveTransactionHandler.class);

  private ClientTransactionManager txManager;
  private ClientLockManager lockManager;
  private final SessionManager sessionManager;
  private final ClientGlobalTransactionManager gtxManager;
  private final AcknowledgeTransactionMessageFactory atmFactory;
  private final Sink eventDeliverySink;

  public ReceiveTransactionHandler(final AcknowledgeTransactionMessageFactory atmFactory,
                                   final ClientGlobalTransactionManager gtxManager,
                                   final SessionManager sessionManager,
                                   final Sink eventDeliverySink) {
    this.atmFactory = atmFactory;
    this.gtxManager = gtxManager;
    this.sessionManager = sessionManager;
    this.eventDeliverySink = eventDeliverySink;
  }

  @Override
  public void handleEvent(EventContext context) {
    final BroadcastTransactionMessageImpl btm = (BroadcastTransactionMessageImpl) context;

    final GlobalTransactionID lowWaterMark = btm.getLowGlobalTransactionIDWatermark();
    if (!lowWaterMark.isNull()) {
      this.gtxManager.setLowWatermark(lowWaterMark, btm.getSourceNodeID());
    }

    if (this.gtxManager.startApply(btm.getCommitterID(), btm.getTransactionID(), btm.getGlobalTransactionID(),
        btm.getSourceNodeID())) {
      final Collection changes = btm.getObjectChanges();
      if (changes.size() > 0 || btm.getNewRoots().size() > 0) {
        try {
          this.txManager.apply(btm.getTransactionType(), btm.getLockIDs(), changes, btm.getNewRoots());
        } catch (TCClassNotFoundException cnfe) {
          logger.warn("transaction apply failed for " + btm.getTransactionID(), cnfe);
          // Do not ignore, re-throw to kill this L1
          throw cnfe;
        } catch (TCNotRunningException tcnre) {
          // Catch and ignore, since we are shutting down anyway; ack should be sent below though, to prevent blockage
          // when this is an echo broadcast (for new root creation, for instance)
          logger.debug("ignoring transaction apply failure for " + btm.getTransactionID() + " due to " + tcnre);
        }

      }

      notifyLogicalChangeResultsReceived(btm);
      sendServerEvents(btm);
    }

    notifyLockManager(btm);
    sendAck(btm);
    btm.recycle();
  }

  void sendAck(final BroadcastTransactionMessage btm) {
    // XXX:: This is a potential race condition here 'coz after we decide to send an ACK
    // and before we actually send it, the server may go down and come back up !
    if (this.sessionManager.isCurrentSession(btm.getSourceNodeID(), btm.getLocalSessionID())) {
      AcknowledgeTransactionMessage ack = this.atmFactory.newAcknowledgeTransactionMessage(btm.getSourceNodeID());
      ack.initialize(btm.getCommitterID(), btm.getTransactionID());
      ack.send();
    }
  }

  void notifyLockManager(final BroadcastTransactionMessage btm) {
    final Collection notifies = btm.getNotifies();
    for (final Object notify : notifies) {
      ClientServerExchangeLockContext lc = (ClientServerExchangeLockContext) notify;
      this.lockManager.notified(lc.getLockID(), lc.getThreadID());
    }
  }

  void notifyLogicalChangeResultsReceived(final BroadcastTransactionMessageImpl btm) {
    final Map<LogicalChangeID, LogicalChangeResult> logicalChangeResults = btm.getLogicalChangeResults();
    if (!logicalChangeResults.isEmpty()) {
      this.txManager.receivedLogicalChangeResult(logicalChangeResults);
    }
  }

  void sendServerEvents(final BroadcastTransactionMessage btm) {
    final NodeID remoteNode = btm.getChannel().getRemoteNodeID();
    // unfold the batch and multiplex messages to different queues based on the event key
    for (final ServerEvent event : btm.getEvents()) {
      // blocks when the internal stage's queue reaches TCPropertiesConsts.L1_SERVER_EVENT_DELIVERY_QUEUE_SIZE
      // to delay the transaction acknowledgement and provide back-pressure on clients
      eventDeliverySink.add(new ServerEventDeliveryContext(event, remoteNode));
    }
  }


  @Override
  public void initialize(ConfigurationContext context) {
    super.initialize(context);
    ClientConfigurationContext ccc = (ClientConfigurationContext) context;
    this.txManager = ccc.getTransactionManager();
    this.lockManager = ccc.getLockManager();
  }
}

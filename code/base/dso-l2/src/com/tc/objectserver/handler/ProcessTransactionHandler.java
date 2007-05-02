/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.handler;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.ConfigurationContext;
import com.tc.async.api.EventContext;
import com.tc.async.api.Sink;
import com.tc.async.api.Stage;
import com.tc.l2.context.IncomingTransactionContext;
import com.tc.l2.objectserver.ReplicatedObjectManager;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.protocol.tcm.ChannelID;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.object.msg.CommitTransactionMessageImpl;
import com.tc.object.msg.MessageRecycler;
import com.tc.objectserver.core.api.ServerConfigurationContext;
import com.tc.objectserver.tx.ServerTransaction;
import com.tc.objectserver.tx.ServerTransactionManager;
import com.tc.objectserver.tx.TransactionBatchManager;
import com.tc.objectserver.tx.TransactionBatchReader;
import com.tc.objectserver.tx.TransactionBatchReaderFactory;
import com.tc.objectserver.tx.TransactionalObjectManager;
import com.tc.util.SequenceValidator;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;

public class ProcessTransactionHandler extends AbstractEventHandler {
  private static final TCLogger            logger = TCLogging.getLogger(ProcessTransactionHandler.class);

  private TransactionBatchReaderFactory    batchReaderFactory;
  private ReplicatedObjectManager          replicatedObjectMgr;

  private final TransactionBatchManager    transactionBatchManager;
  private final MessageRecycler            messageRecycler;
  private final SequenceValidator          sequenceValidator;
  private final TransactionalObjectManager txnObjectManager;

  private Sink                             txnRelaySink;

  private ServerTransactionManager         transactionManager;

  public ProcessTransactionHandler(TransactionBatchManager transactionBatchManager,
                                   TransactionalObjectManager txnObjectManager, SequenceValidator sequenceValidator,
                                   MessageRecycler messageRecycler) {
    this.transactionBatchManager = transactionBatchManager;
    this.txnObjectManager = txnObjectManager;
    this.sequenceValidator = sequenceValidator;
    this.messageRecycler = messageRecycler;
  }

  public void handleEvent(EventContext context) {
    final CommitTransactionMessageImpl ctm = (CommitTransactionMessageImpl) context;
    try {
      final TransactionBatchReader reader = batchReaderFactory.newTransactionBatchReader(ctm);
      transactionBatchManager.defineBatch(reader.getChannelID(), reader.getBatchID(), reader.getNumTxns());
      Collection completedTxnIds = reader.addAcknowledgedTransactionIDsTo(new HashSet());
      ServerTransaction txn;

      //XXX:: Has to be ordered.
      Map txns = new LinkedHashMap(reader.getNumTxns());
      ChannelID channelID = reader.getChannelID();
      // NOTE::XXX:: GlobalTransactionID id assigned in the process transaction stage. The transaction could be
      // re-ordered before apply. This is not a problem because for an transaction to be re-ordered, it should not
      // have any common objects between them. hence if g1 is the first txn and g2 is the second txn, g2 will be applied
      // before g1, only when g2 has no common objects with g1. If this is not true then we cant assign gid here.
      while ((txn = reader.getNextTransaction()) != null) {
        sequenceValidator.setCurrent(channelID, txn.getClientSequenceID());
        txns.put(txn.getServerTransactionID(), txn);
      }
      messageRecycler.addMessage(ctm, txns.keySet());
      if (replicatedObjectMgr.relayTransactions()) {
        transactionManager.incomingTransactions(channelID, txns, true);
        txnRelaySink.add(new IncomingTransactionContext(channelID, ctm, txns));
      } else {
        transactionManager.incomingTransactions(channelID, txns, false);
      }
      txnObjectManager.addTransactions(txns.values(), completedTxnIds);
    } catch (Exception e) {
      logger.error("Error reading transaction batch. : ", e);
      MessageChannel c = ctm.getChannel();
      logger.error("Closing channel " + c.getChannelID() + " due to previous errors !");
      c.close();
    }
  }

  public void initialize(ConfigurationContext context) {
    super.initialize(context);
    ServerConfigurationContext oscc = (ServerConfigurationContext) context;
    batchReaderFactory = oscc.getTransactionBatchReaderFactory();
    transactionManager = oscc.getTransactionManager();
    replicatedObjectMgr = oscc.getL2Coordinator().getReplicatedObjectManager();
    Stage relayStage = oscc.getStage(ServerConfigurationContext.TRANSACTION_RELAY_STAGE);
    if (relayStage != null) {
      txnRelaySink = relayStage.getSink();
    }
  }
}

/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.objectserver.handler;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.ConfigurationContext;
import com.tc.async.api.EventContext;
import com.tc.async.api.EventHandlerException;
import com.tc.async.api.Sink;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.protocol.tcm.ChannelID;
import com.tc.object.msg.CommitTransactionMessageImpl;
import com.tc.object.msg.MessageRecycler;
import com.tc.objectserver.context.LookupEventContext;
import com.tc.objectserver.core.api.ServerConfigurationContext;
import com.tc.objectserver.tx.BatchDefinedException;
import com.tc.objectserver.tx.ServerTransaction;
import com.tc.objectserver.tx.TransactionBatchManager;
import com.tc.objectserver.tx.TransactionBatchReader;
import com.tc.objectserver.tx.TransactionBatchReaderFactory;
import com.tc.objectserver.tx.TransactionalObjectManager;
import com.tc.util.SequenceValidator;

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * I'm going to keep this simple at first and block the thread of the Object request if the objects can't be retrieved.
 * I'll make this fancy later
 * 
 * @author steve
 */
public class ProcessTransactionHandler extends AbstractEventHandler {
  private static final TCLogger             logger = TCLogging.getLogger(ProcessTransactionHandler.class);
  private TransactionBatchReaderFactory     batchReaderFactory;
  private final TransactionBatchManager     transactionBatchManager;
  private final MessageRecycler             messageRecycler;
  private final SequenceValidator           sequenceValidator;
  private final TransactionalObjectManager txnObjectManager;
  private Sink lookupStage;

  public ProcessTransactionHandler(TransactionBatchManager transactionBatchManager,
                                   TransactionalObjectManager txnObjectManager,
                                   SequenceValidator sequenceValidator, MessageRecycler messageRecycler) {
    this.transactionBatchManager = transactionBatchManager;
    this.txnObjectManager = txnObjectManager;
    this.sequenceValidator = sequenceValidator;
    this.messageRecycler = messageRecycler;
  }

  public void handleEvent(EventContext context) throws EventHandlerException {
    try {
      final TransactionBatchReader reader = batchReaderFactory.newTransactionBatchReader(context);
      try {
        transactionBatchManager.defineBatch(reader.getChannelID(), reader.getBatchID(), reader.getNumTxns());
      } catch (BatchDefinedException e) {
        throw new EventHandlerException(e);
      }
      Collection completedTxnIds = reader.addAcknowledgedTransactionIDsTo(new HashSet());
      ServerTransaction txn;

      List txns = new LinkedList();
      Set serverTxnIDs = new HashSet();
      ChannelID channelID = reader.getChannelID();
      while ((txn = reader.getNextTransaction()) != null) {
        sequenceValidator.setCurrent(channelID, txn.getClientSequenceID());
        txns.add(txn);
        serverTxnIDs.add(txn.getServerTransactionID());
      }
      messageRecycler.addMessage((CommitTransactionMessageImpl) context, serverTxnIDs);
      txnObjectManager.addTransactions(reader.getChannelID(), txns, completedTxnIds);
      lookupStage.add(new LookupEventContext());
    } catch (IOException e) {
      logger.error("Error reading transaction batch. Discarding remaining changes in this batch", e);
    }
  }

  public void initialize(ConfigurationContext context) {
    super.initialize(context);
    ServerConfigurationContext oscc = (ServerConfigurationContext) context;
    batchReaderFactory = oscc.getTransactionBatchReaderFactory();
    lookupStage = context.getStage(ServerConfigurationContext.TRANSACTION_LOOKUP_STAGE).getSink();
  }
}

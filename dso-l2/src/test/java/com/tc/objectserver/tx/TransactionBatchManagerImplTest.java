/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.tx;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import org.mockito.InOrder;

import com.tc.async.api.EventContext;
import com.tc.async.api.Sink;
import com.tc.net.ClientID;
import com.tc.object.msg.CommitTransactionMessage;
import com.tc.object.msg.MessageRecycler;
import com.tc.object.net.DSOChannelManager;
import com.tc.object.tx.ServerTransactionID;
import com.tc.object.tx.TransactionID;
import com.tc.object.tx.TxnBatchID;
import com.tc.object.tx.TxnType;
import com.tc.objectserver.core.api.ServerConfigurationContext;
import com.tc.objectserver.gtx.ServerGlobalTransactionManager;
import com.tc.test.TCTestCase;
import com.tc.util.BitSetObjectIDSet;
import com.tc.util.SequenceID;
import com.tc.util.SequenceValidator;

import java.io.IOException;

public class TransactionBatchManagerImplTest extends TCTestCase {

  private TransactionBatchManagerImpl mgr;
  private TransactionFilter filter;
  private Sink syncWriteSink;
  private ResentTransactionSequencer resentTransactionSequencer;
  private TransactionBatchReader batchReader;
  private ServerTransactionManager serverTransactionManager;
  private ServerGlobalTransactionManager serverGlobalTransactionManager;
  private DSOChannelManager dsoChannelManager;
  private CommitTransactionMessage ctm;


  @Override
  protected void setUp() throws Exception {
    filter = mock(TransactionFilter.class);
    syncWriteSink = mock(Sink.class);
    resentTransactionSequencer = spy(new ResentTransactionSequencer());
    mgr = new TransactionBatchManagerImpl(mock(SequenceValidator.class), mock(MessageRecycler.class), filter, syncWriteSink,
        resentTransactionSequencer);
    serverTransactionManager = mock(ServerTransactionManager.class);
    serverGlobalTransactionManager = mock(ServerGlobalTransactionManager.class);
    dsoChannelManager = mock(DSOChannelManager.class);
    ctm = mock(CommitTransactionMessage.class);
    batchReader = mock(TransactionBatchReader.class);
    TransactionBatchReaderFactory transactionBatchReaderFactory = when(mock(TransactionBatchReaderFactory.class).newTransactionBatchReader(any(CommitTransactionMessage.class))).thenReturn(batchReader).getMock();
    ServerConfigurationContext serverConfigurationContext = mock(ServerConfigurationContext.class);
    when(serverConfigurationContext.getTransactionBatchReaderFactory()).thenReturn(transactionBatchReaderFactory);
    when(serverConfigurationContext.getTransactionManager()).thenReturn(serverTransactionManager);
    when(serverConfigurationContext.getServerGlobalTransactionManager()).thenReturn(serverGlobalTransactionManager);
    when(serverConfigurationContext.getChannelManager()).thenReturn(dsoChannelManager);
    mgr.initializeContext(serverConfigurationContext);
  }

  public void testSyncWriteReceived() throws Exception {
    batch(true);
    mgr.addTransactionBatch(ctm);
    InOrder inOrder = inOrder(filter, syncWriteSink);
    inOrder.verify(filter).addTransactionBatch(any(IncomingTransactionBatchContext.class));
    inOrder.verify(syncWriteSink).add(any(EventContext.class));
  }

  private void batch(boolean sync) throws IOException {
    ServerTransaction txn = txn(1, sync);
    when(batchReader.containsSyncWriteTransaction()).thenReturn(sync);
    when(batchReader.getBatchID()).thenReturn(new TxnBatchID(1));
    when(batchReader.getNextTransaction()).thenReturn(txn).thenReturn(null);
  }

  private ServerTransaction txn(long id, boolean syncWrite) {
    ServerTransaction serverTransaction = mock(ServerTransaction.class);
    when(serverTransaction.getTransactionID()).thenReturn(new TransactionID(id));
    when(serverTransaction.getServerTransactionID()).thenReturn(new ServerTransactionID(new ClientID(1), new TransactionID(id)));
    when(serverTransaction.getClientSequenceID()).thenReturn(new SequenceID(id));
    when(serverTransaction.getTransactionType()).thenReturn(syncWrite ? TxnType.SYNC_WRITE : TxnType.NORMAL);
    when(serverTransaction.getNewObjectIDs()).thenReturn(new BitSetObjectIDSet());
    return serverTransaction;
  }
}

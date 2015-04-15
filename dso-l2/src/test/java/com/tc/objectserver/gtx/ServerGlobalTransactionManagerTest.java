/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
 */
package com.tc.objectserver.gtx;

import org.junit.Before;
import org.junit.Test;

import com.tc.async.api.EventContext;
import com.tc.async.api.Sink;
import com.tc.net.ClientID;
import com.tc.net.ServerID;
import com.tc.object.gtx.GlobalTransactionID;
import com.tc.object.tx.ServerTransactionID;
import com.tc.object.tx.TransactionID;
import com.tc.objectserver.api.TransactionStore;
import com.tc.objectserver.event.ServerEventBuffer;
import com.tc.objectserver.persistence.PersistenceTransactionProvider;
import com.tc.util.SequenceValidator;
import com.tc.util.sequence.Sequence;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ServerGlobalTransactionManagerTest {
  private ServerGlobalTransactionManager serverGlobalTransactionManager;
  private TransactionStore transactionStore;
  private ServerEventBuffer serverEventBuffer;
  private Sink callbackSink;
  private Sequence gidSequence;

  @Before
  public void setUp() throws Exception {
    transactionStore = mock(TransactionStore.class);
    serverEventBuffer = mock(ServerEventBuffer.class);
    callbackSink = mock(Sink.class);
    gidSequence = mock(Sequence.class);
    serverGlobalTransactionManager = new ServerGlobalTransactionManagerImpl(mock(SequenceValidator.class),
        transactionStore, mock(GlobalTransactionIDSequenceProvider.class), gidSequence,
        callbackSink, mock(PersistenceTransactionProvider.class), serverEventBuffer);
  }

  @Test
  public void testClearCommittedTransaction() throws Exception {
    ServerTransactionID serverTransactionID = new ServerTransactionID(ServerID.NULL_ID, new TransactionID(1));
    GlobalTransactionDescriptor descriptor = new GlobalTransactionDescriptor(serverTransactionID, new GlobalTransactionID(1));
    when(transactionStore.clearCommittedTransaction(serverTransactionID)).thenReturn(descriptor);

    serverGlobalTransactionManager.clearCommittedTransaction(serverTransactionID);

    verify(transactionStore).clearCommittedTransaction(serverTransactionID);
    verify(serverEventBuffer).removeEventsForTransaction(descriptor.getGlobalTransactionID());
  }

  @Test
  public void testCallbacksForClearCommittedTransactions() throws Exception {
    ServerTransactionID stxID1 = new ServerTransactionID(ServerID.NULL_ID, new TransactionID(1));
    ServerTransactionID stxID2 = new ServerTransactionID(ServerID.NULL_ID, new TransactionID(2));
    GlobalTransactionDescriptor descriptor1 = new GlobalTransactionDescriptor(stxID1, new GlobalTransactionID(1));
    GlobalTransactionDescriptor descriptor2 = new GlobalTransactionDescriptor(stxID2, new GlobalTransactionID(2));
    when(transactionStore.clearCommittedTransaction(stxID1)).thenReturn(descriptor1);
    when(transactionStore.clearCommittedTransaction(stxID2)).thenReturn(descriptor2);
    when(gidSequence.current()).thenReturn(2L);
    when(transactionStore.getLeastGlobalTransactionID()).thenReturn(descriptor1.getGlobalTransactionID());
    serverGlobalTransactionManager.registerCallbackOnLowWaterMarkReached(mock(Runnable.class));

    serverGlobalTransactionManager.clearCommittedTransaction(stxID1);
    verify(callbackSink, never()).add(any(EventContext.class));

    when(transactionStore.getLeastGlobalTransactionID()).thenReturn(GlobalTransactionID.NULL_ID);
    serverGlobalTransactionManager.clearCommittedTransaction(stxID2);
    verify(callbackSink).add(any(EventContext.class));
  }

  @Test
  public void testShutdownClientWithLiveTransactions() throws Exception {
    ClientID clientID = new ClientID(0);
    ServerTransactionID sid = new ServerTransactionID(clientID, new TransactionID(1));
    GlobalTransactionID gid = serverGlobalTransactionManager.getOrCreateGlobalTransactionID(sid);
    serverGlobalTransactionManager.shutdownNode(clientID);
    assertTrue(serverGlobalTransactionManager.initiateApply(sid));
    assertEquals(gid, serverGlobalTransactionManager.getGlobalTransactionID(sid));
    serverGlobalTransactionManager.commit(sid);
    serverGlobalTransactionManager.clearCommitedTransactionsBelowLowWaterMark(sid);
    assertNull(serverGlobalTransactionManager.getGlobalTransactionID(sid));
  }
}
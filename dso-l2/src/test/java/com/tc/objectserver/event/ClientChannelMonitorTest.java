package com.tc.objectserver.event;

import org.junit.Test;
import org.mockito.Answers;

import com.tc.async.api.Sink;
import com.tc.async.api.Stage;
import com.tc.l2.objectserver.ServerTransactionFactory;
import com.tc.net.ClientID;
import com.tc.net.protocol.tcm.ChannelID;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.object.ObjectID;
import com.tc.object.dna.impl.ObjectStringSerializer;
import com.tc.object.net.DSOChannelManager;
import com.tc.object.tx.ServerTransactionID;
import com.tc.object.tx.TransactionID;
import com.tc.objectserver.context.ServerTransactionCompleteContext;
import com.tc.objectserver.core.api.ServerConfigurationContext;
import com.tc.objectserver.tx.ServerTransaction;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ClientChannelMonitorTest {
  @Test
  public void testClearCompletedTransaction() throws Exception {
    ServerTransactionFactory serverTransactionFactory = mock(ServerTransactionFactory.class);
    DSOChannelManager channelManager = mock(DSOChannelManager.class);
    ClientID clientID = new ClientID(1);
    ChannelID channelID = new ChannelID(1);
    when(channelManager.getClientIDFor(channelID)).thenReturn(clientID);
    ClientChannelMonitorImpl monitor = new ClientChannelMonitorImpl(channelManager,
        serverTransactionFactory);
    ServerConfigurationContext context = mock(ServerConfigurationContext.class, Answers.RETURNS_MOCKS.get());
    Sink lwmSink = mock(Sink.class);
    Stage lwmStage = mock(Stage.class);
    when(lwmStage.getSink()).thenReturn(lwmSink);
    when(context.getStage(ServerConfigurationContext.TRANSACTION_LOWWATERMARK_STAGE)).thenReturn(lwmStage);
    monitor.initializeContext(context);


    ObjectID oid = new ObjectID(0);
    monitor.monitorClient(clientID, oid);

    MessageChannel channel = mock(MessageChannel.class);
    when(channel.getChannelID()).thenReturn(channelID);

    ServerTransaction serverTransaction = mock(ServerTransaction.class);
    ServerTransactionID serverTransactionID = new ServerTransactionID(clientID, new TransactionID(0));
    when(serverTransaction.getServerTransactionID()).thenReturn(serverTransactionID);
    when(serverTransactionFactory.createRemoveEventListeningClientTransaction(eq(oid), eq(clientID), any(ObjectStringSerializer.class))).thenReturn(serverTransaction);
    monitor.channelRemoved(channel);
    
    monitor.transactionCompleted(serverTransactionID);
    
    verify(lwmSink).add(new ServerTransactionCompleteContext(serverTransactionID));
  }
}
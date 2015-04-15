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
package com.tc.objectserver.handler;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.tc.async.impl.MockStage;
import com.tc.net.ClientID;
import com.tc.net.ServerID;
import com.tc.net.protocol.tcm.ChannelID;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.object.ObjectID;
import com.tc.object.dna.api.LogicalChangeID;
import com.tc.object.dna.api.LogicalChangeResult;
import com.tc.object.gtx.GlobalTransactionID;
import com.tc.object.msg.BroadcastTransactionMessage;
import com.tc.object.net.DSOChannelManager;
import com.tc.object.tx.ServerTransactionID;
import com.tc.object.tx.TransactionID;
import com.tc.objectserver.context.BroadcastChangeContext;
import com.tc.objectserver.core.api.ServerConfigurationContext;
import com.tc.objectserver.core.api.TestDNA;
import com.tc.objectserver.event.ClientChannelMonitor;
import com.tc.objectserver.event.ServerEventBuffer;
import com.tc.objectserver.l1.api.ClientStateManager;
import com.tc.objectserver.l1.api.InvalidateObjectManager;
import com.tc.objectserver.l1.impl.ClientStateManagerImpl;
import com.tc.objectserver.locks.NotifiedWaiters;
import com.tc.objectserver.managedobject.ApplyTransactionInfo;
import com.tc.objectserver.mgmt.ObjectStatsRecorder;
import com.tc.objectserver.tx.ServerTransaction;
import com.tc.objectserver.tx.ServerTransactionManager;
import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;
import com.tc.server.ServerEvent;
import com.tc.stats.counter.sampled.SampledCounterConfig;
import com.tc.stats.counter.sampled.SampledCounterImpl;
import com.tc.stats.counter.sampled.derived.SampledRateCounterConfig;
import com.tc.stats.counter.sampled.derived.SampledRateCounterImpl;
import com.tc.test.TCTestCase;

import java.util.Arrays;
import java.util.List;

public class BroadcastChangeHandlerTest extends TCTestCase {

  private BroadcastChangeHandler     handler;
  private ServerConfigurationContext serverCfgCxt;
  private DSOChannelManager channelManager;
  private ServerTransactionManager transactionManager;
  private ClientStateManager clientStateManager;
  private SampledCounterImpl sci;
  private SampledRateCounterImpl srci;
  private ServerTransaction txn;
  private ApplyTransactionInfo applyTransactionInfo;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    applyTransactionInfo = new ApplyTransactionInfo();
    sci = new SampledCounterImpl(new SampledCounterConfig(5, 10, true, 0));
    srci = new SampledRateCounterImpl(new SampledRateCounterConfig(5, 10, true));
    channelManager = mock(DSOChannelManager.class);
    transactionManager = mock(ServerTransactionManager.class);
    clientStateManager = spy(new ClientStateManagerImpl());
    this.serverCfgCxt = mock(ServerConfigurationContext.class);
    when(serverCfgCxt.getChannelManager()).thenReturn(channelManager);
    when(serverCfgCxt.getTransactionManager()).thenReturn(transactionManager);
    when(serverCfgCxt.getStage(anyString())).thenReturn(new MockStage("foo"));
    when(serverCfgCxt.getClientStateManager()).thenReturn(clientStateManager);
    createBroadcastHandler();
    txn = createTransaction(1, 1, 1);
    createChannels(1);
  }

  private void createBroadcastHandler() {
    this.handler = new BroadcastChangeHandler(sci, new ObjectStatsRecorder(), srci,
        mock(InvalidateObjectManager.class));
    this.handler.initialize(this.serverCfgCxt);

  }

  public void testBasic() throws Exception {
    // Sneak the broadcast message out of the other mock.
    BroadcastTransactionMessage broadcastTransactionMessage =
        (BroadcastTransactionMessage)channelManager.getActiveChannels()[0].createMessage(TCMessageType.BROADCAST_TRANSACTION_MESSAGE);
    clientStateManager.addReference(new ClientID(0), new ObjectID(1));
    final BroadcastChangeContext context = new BroadcastChangeContext(txn, new GlobalTransactionID(1),
                                                                      new NotifiedWaiters(), applyTransactionInfo);
    this.handler.handleEvent(context);
    verify(broadcastTransactionMessage).send();
    verify(transactionManager).addWaitingForAcknowledgement(new ClientID(1), new TransactionID(1), new ClientID(0));
    verify(transactionManager).broadcasted(new ClientID(1), new TransactionID(1));
  }

  public void testSuccessResultNoneMode() throws Exception {
    // Set mode to disk and recreate the handler
    TCPropertiesImpl.getProperties().setProperty(TCPropertiesConsts.L2_TRANSACTIONMANAGER_BROADCAST_DURABILITY_LEVEL, "NONE");
    createBroadcastHandler();

    applyTransactionInfo.getApplyResultRecorder().recordResult(new LogicalChangeID(1), new LogicalChangeResult(true));
    handler.handleEvent(new BroadcastChangeContext(txn, new GlobalTransactionID(1), new NotifiedWaiters(), applyTransactionInfo));
    verify(transactionManager, never()).waitForTransactionCommit(txn.getServerTransactionID());
    verify(transactionManager, never()).waitForTransactionRelay(txn.getServerTransactionID());
  }

  public void testSuccessResultDiskMode() throws Exception {
    // Set mode to disk and recreate the handler
    TCPropertiesImpl.getProperties().setProperty(TCPropertiesConsts.L2_TRANSACTIONMANAGER_BROADCAST_DURABILITY_LEVEL, "DISK");
    createBroadcastHandler();

    applyTransactionInfo.getApplyResultRecorder().recordResult(new LogicalChangeID(1), new LogicalChangeResult(true));
    handler.handleEvent(new BroadcastChangeContext(txn, new GlobalTransactionID(1), new NotifiedWaiters(), applyTransactionInfo));
    verify(transactionManager).waitForTransactionCommit(txn.getServerTransactionID());
    verify(transactionManager).waitForTransactionRelay(txn.getServerTransactionID());
  }

  public void testNoSuccessResultDiskMode() throws Exception {
    // Set mode to disk and recreate the handler
    TCPropertiesImpl.getProperties().setProperty(TCPropertiesConsts.L2_TRANSACTIONMANAGER_BROADCAST_DURABILITY_LEVEL, "DISK");
    createBroadcastHandler();

    handler.handleEvent(new BroadcastChangeContext(txn, new GlobalTransactionID(1), new NotifiedWaiters(), applyTransactionInfo));
    verify(transactionManager, never()).waitForTransactionCommit(txn.getServerTransactionID());
    verify(transactionManager, never()).waitForTransactionRelay(txn.getServerTransactionID());
  }

  public void testSuccessResultRelayedMode() throws Exception {
    // Set mode to disk and recreate the handler
    TCPropertiesImpl.getProperties().setProperty(TCPropertiesConsts.L2_TRANSACTIONMANAGER_BROADCAST_DURABILITY_LEVEL, "RELAYED");
    createBroadcastHandler();

    applyTransactionInfo.getApplyResultRecorder().recordResult(new LogicalChangeID(1), new LogicalChangeResult(true));
    handler.handleEvent(new BroadcastChangeContext(txn, new GlobalTransactionID(1), new NotifiedWaiters(), applyTransactionInfo));
    verify(transactionManager, never()).waitForTransactionCommit(txn.getServerTransactionID());
    verify(transactionManager).waitForTransactionRelay(txn.getServerTransactionID());
  }

  public void testClearBroadcastedServerEventFromBuffer() throws Exception {
    GlobalTransactionID gid = new GlobalTransactionID(1);
    when(txn.getServerTransactionID()).thenReturn(new ServerTransactionID(new ServerID("1234", new byte[20]), new TransactionID(1)));
    ServerEventBuffer serverEventBuffer = when(mock(ServerEventBuffer.class).getServerEventsPerClient(gid)).thenReturn(
        HashMultimap.<ClientID, ServerEvent>create()).getMock();

    applyTransactionInfo = new ApplyTransactionInfo(true, txn.getServerTransactionID(), gid, false,
        true, serverEventBuffer, mock(ClientChannelMonitor.class));

    handler.handleEvent(new BroadcastChangeContext(txn, gid, new NotifiedWaiters(), applyTransactionInfo));

    verify(serverEventBuffer).removeEventsForTransaction(gid);
  }

  private static ServerTransaction createTransaction(long sourceID, long txID, long gid) {
    ServerTransaction transaction = mock(ServerTransaction.class);
    when(transaction.getSourceID()).thenReturn(new ClientID(sourceID));
    when(transaction.getServerTransactionID()).thenReturn(new ServerTransactionID(new ClientID(sourceID), new TransactionID(txID)));
    when(transaction.getGlobalTransactionID()).thenReturn(new GlobalTransactionID(gid));
    when(transaction.getTransactionID()).thenReturn(new TransactionID(txID));
    when(transaction.getChanges()).thenReturn(Arrays.asList(new TestDNA(new ObjectID(1), true)));
    return transaction;
  }

  private void createChannels(int count) {
    List<MessageChannel> channels = Lists.newArrayList();
    for (int i = 0; i < count; i++) {
      MessageChannel channel = mock(MessageChannel.class);
      BroadcastTransactionMessage message = mock(BroadcastTransactionMessage.class);
      when(channel.getChannelID()).thenReturn(new ChannelID(i));
      when(channel.createMessage(TCMessageType.BROADCAST_TRANSACTION_MESSAGE)).thenReturn(message);
      channels.add(channel);
      when(channelManager.getClientIDFor(new ChannelID(i))).thenReturn(new ClientID(i));
      clientStateManager.startupNode(new ClientID(i));
    }
    when(channelManager.getActiveChannels()).thenReturn(channels.toArray(new MessageChannel[count]));
  }
}

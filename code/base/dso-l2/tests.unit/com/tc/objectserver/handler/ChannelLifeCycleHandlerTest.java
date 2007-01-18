/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.handler;

import com.tc.net.protocol.tcm.ChannelID;
import com.tc.net.protocol.tcm.TestCommunicationsManager;
import com.tc.net.protocol.tcm.TestMessageChannel;
import com.tc.objectserver.core.impl.TestServerConfigurationContext;
import com.tc.objectserver.tx.TestServerTransactionManager;
import com.tc.objectserver.tx.TestTransactionBatchManager;

import junit.framework.TestCase;

public class ChannelLifeCycleHandlerTest extends TestCase {
  private ChannelLifeCycleHandler      handler;
  private TestTransactionBatchManager  transactionBatchManager;
  private TestServerTransactionManager transactionManager;
  private TestCommunicationsManager    commsManager;

  protected void setUp() throws Exception {
    super.setUp();
    transactionManager = new TestServerTransactionManager();
    this.transactionBatchManager = new TestTransactionBatchManager();
    this.commsManager = new TestCommunicationsManager();
    handler = new ChannelLifeCycleHandler(this.commsManager, this.transactionManager, this.transactionBatchManager);
    TestServerConfigurationContext tscc = new TestServerConfigurationContext();
    handler.initialize(tscc);
  }

  public void tests() throws Exception {
    final ChannelID channelID = new ChannelID(1);

    TestMessageChannel channel = new TestMessageChannel();
    channel.channelID = channelID;

    ChannelLifeCycleHandler.Event disconnectEvent = new ChannelLifeCycleHandler.Event(
                                                                                      ChannelLifeCycleHandler.Event.REMOVE,
                                                                                      channel);
    handler.handleEvent(disconnectEvent);

    assertEquals(channelID, transactionManager.shutdownClientCalls.poll(1));
    assertEquals(channelID, transactionBatchManager.shutdownClientCalls.poll(1));

    // now make sure it doesn't do anything if the comms manager is in shutdown.
    this.commsManager.shutdown = true;
    handler.handleEvent(disconnectEvent);

    assertNull(transactionManager.shutdownClientCalls.poll(1));
    assertNull(transactionBatchManager.shutdownClientCalls.poll(1));
  }

}

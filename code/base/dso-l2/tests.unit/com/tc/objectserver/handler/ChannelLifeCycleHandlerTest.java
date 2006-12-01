/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.objectserver.handler;

import com.tc.exception.ImplementMe;
import com.tc.net.protocol.tcm.ChannelEvent;
import com.tc.net.protocol.tcm.ChannelEventType;
import com.tc.net.protocol.tcm.ChannelID;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.tcm.TestCommunicationsManager;
import com.tc.objectserver.core.impl.TestServerConfigurationContext;
import com.tc.objectserver.tx.TestBatchedTransactionProcessor;
import com.tc.objectserver.tx.TestServerTransactionManager;
import com.tc.objectserver.tx.TestTransactionBatchManager;

import java.util.Date;

import junit.framework.TestCase;

public class ChannelLifeCycleHandlerTest extends TestCase {
  private ChannelLifeCycleHandler handler;
  private TestTransactionBatchManager transactionBatchManager;
  private TestServerTransactionManager transactionManager;
  private TestCommunicationsManager commsManager;
  private TestBatchedTransactionProcessor txnBatchProcessor;
  
  protected void setUp() throws Exception {
    super.setUp();
    transactionManager = new TestServerTransactionManager();
    this.transactionBatchManager = new TestTransactionBatchManager();
    this.commsManager = new TestCommunicationsManager();
    this.txnBatchProcessor = new TestBatchedTransactionProcessor();
    handler = new ChannelLifeCycleHandler(this.commsManager, this.transactionManager, this.transactionBatchManager);
    TestServerConfigurationContext tscc = new TestServerConfigurationContext();
    tscc.txnBatchProcessor = this.txnBatchProcessor;
    handler.initialize(tscc);
  }

  public void tests() throws Exception {
    final ChannelID channelID = new ChannelID(1);
    
    ChannelEvent disconnectEvent = new ChannelEvent() {

      public MessageChannel getChannel() {
        throw new ImplementMe();
      }

      public Date getTimestamp() {
        throw new ImplementMe();
      }

      public ChannelEventType getType() {
        return ChannelEventType.TRANSPORT_DISCONNECTED_EVENT;
      }

      public ChannelID getChannelID() {
        return channelID;
      }
      
    };
    handler.handleEvent(disconnectEvent);
    
    assertEquals(channelID, txnBatchProcessor.shutdownClientCalls.poll(1));
    assertEquals(channelID, transactionManager.shutdownClientCalls.poll(1));
    assertEquals(channelID, transactionBatchManager.shutdownClientCalls.poll(1));
    
    // now make sure it doesn't do anything if the comms manager is in shutdown.
    this.commsManager.shutdown = true;
    handler.handleEvent(disconnectEvent);
    
    assertNull(txnBatchProcessor.shutdownClientCalls.poll(1));
    assertNull(transactionManager.shutdownClientCalls.poll(1));
    assertNull(transactionBatchManager.shutdownClientCalls.poll(1));
  }
  
}

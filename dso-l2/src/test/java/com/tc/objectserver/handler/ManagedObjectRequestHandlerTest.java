/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.handler;

import com.tc.async.impl.MockStage;
import com.tc.exception.ImplementMe;
import com.tc.net.NodeID;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.object.ObjectID;
import com.tc.object.ServerMapRequestType;
import com.tc.object.TestRequestManagedObjectMessage;
import com.tc.object.net.ChannelStats;
import com.tc.object.net.TestDSOChannelManager;
import com.tc.objectserver.api.TestSink;
import com.tc.objectserver.core.api.ServerConfigurationContext;
import com.tc.objectserver.core.impl.TestServerConfigurationContext;
import com.tc.objectserver.impl.ObjectRequestManagerImpl;
import com.tc.objectserver.impl.TestObjectManager;
import com.tc.objectserver.l1.api.TestClientStateManager;
import com.tc.objectserver.mgmt.ObjectStatsRecorder;
import com.tc.stats.counter.Counter;
import com.tc.stats.counter.CounterImpl;
import com.tc.util.ObjectIDSet;

import junit.framework.TestCase;

public class ManagedObjectRequestHandlerTest extends TestCase {

  public void testObjectRequestCounter() {
    Counter channelReqCounter = new CounterImpl(666L);
    Counter channelRemCounter = new CounterImpl(69L);

    Counter requestCounter = new CounterImpl(0L);
    Counter removeCounter = new CounterImpl(0L);

    TestChannelStats channelStats = new TestChannelStats(channelReqCounter, channelRemCounter);

    TestObjectManager objectManager = new TestObjectManager();
    TestDSOChannelManager channelManager = new TestDSOChannelManager();
    TestClientStateManager clientStateManager = new TestClientStateManager();
    TestSink requestSink = new TestSink();
    TestSink respondSink = new TestSink();
    ObjectRequestManagerImpl objectRequestManager = new ObjectRequestManagerImpl(objectManager, channelManager,
                                                                                 clientStateManager, requestSink,
                                                                                 respondSink, new ObjectStatsRecorder());

    TestServerConfigurationContext context = new TestServerConfigurationContext();
    context.setObjectRequestManager(objectRequestManager);
    context.clientStateManager = new TestClientStateManager();
    context.addStage(ServerConfigurationContext.RESPOND_TO_OBJECT_REQUEST_STAGE, new MockStage("yo"));
    context.channelStats = channelStats;

    ManagedObjectRequestHandler handler = new ManagedObjectRequestHandler(requestCounter, removeCounter);
    handler.initializeContext(context);

    TestRequestManagedObjectMessage msg = new TestRequestManagedObjectMessage();
    ObjectIDSet s = new ObjectIDSet();
    s.add(new ObjectID(1));
    msg.setObjectIDs(s);
    ObjectIDSet removed = makeRemovedSet(31);
    msg.setRemoved(removed);

    assertEquals(0, requestCounter.getValue());
    assertEquals(0, removeCounter.getValue());
    assertEquals(666, channelReqCounter.getValue());
    assertEquals(69, channelRemCounter.getValue());
    handler.handleEvent(msg);
    assertEquals(1, requestCounter.getValue());
    assertEquals(31, removeCounter.getValue());
    assertEquals(667, channelReqCounter.getValue());
    assertEquals(100, channelRemCounter.getValue());
  }

  private ObjectIDSet makeRemovedSet(int num) {
    ObjectIDSet rv = new ObjectIDSet();
    for (int i = 0; i < num; i++) {
      rv.add(new ObjectID(i));
    }
    return rv;
  }

  private static class TestChannelStats implements ChannelStats {

    private final Counter channelReqCounter;
    private final Counter channelRemCounter;

    public TestChannelStats(Counter channelReqCounter, Counter channelRemCounter) {
      this.channelReqCounter = channelReqCounter;
      this.channelRemCounter = channelRemCounter;
    }

    public Counter getCounter(MessageChannel channel, String name) {
      throw new RuntimeException(name);
    }

    public void notifyTransaction(NodeID nodeID, int numTxns) {
      throw new ImplementMe();
    }

    public void notifyObjectRemove(MessageChannel channel, int numObjectsRemoved) {
      this.channelRemCounter.increment(numObjectsRemoved);
    }

    public void notifyObjectRequest(MessageChannel channel, int numObjectsRequested) {
      this.channelReqCounter.increment(numObjectsRequested);
    }

    public void notifyTransactionAckedFrom(NodeID nodeID) {
      throw new ImplementMe();

    }

    public void notifyTransactionBroadcastedTo(NodeID nodeID) {
      throw new ImplementMe();

    }

    public void notifyServerMapRequest(ServerMapRequestType type, MessageChannel channel, int numRequests) {
      throw new ImplementMe();
    }
  }

}

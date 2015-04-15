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
import com.tc.util.BitSetObjectIDSet;
import com.tc.util.ObjectIDSet;

import junit.framework.TestCase;

public class ManagedObjectRequestHandlerTest extends TestCase {

  public void testObjectRequestCounter() {
    Counter channelReqCounter = new CounterImpl(666L);

    Counter requestCounter = new CounterImpl(0L);

    TestChannelStats channelStats = new TestChannelStats(channelReqCounter);

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

    ManagedObjectRequestHandler handler = new ManagedObjectRequestHandler(requestCounter);
    handler.initializeContext(context);

    TestRequestManagedObjectMessage msg = new TestRequestManagedObjectMessage();
    ObjectIDSet s = new BitSetObjectIDSet();
    s.add(new ObjectID(1));
    msg.setObjectIDs(s);
    ObjectIDSet removed = makeRemovedSet(31);
    msg.setRemoved(removed);

    assertEquals(0, requestCounter.getValue());
    assertEquals(666, channelReqCounter.getValue());
    handler.handleEvent(msg);
    assertEquals(1, requestCounter.getValue());
    assertEquals(667, channelReqCounter.getValue());
  }

  private ObjectIDSet makeRemovedSet(int num) {
    ObjectIDSet rv = new BitSetObjectIDSet();
    for (int i = 0; i < num; i++) {
      rv.add(new ObjectID(i));
    }
    return rv;
  }

  private static class TestChannelStats implements ChannelStats {

    private final Counter channelReqCounter;

    public TestChannelStats(Counter channelReqCounter) {
      this.channelReqCounter = channelReqCounter;
    }

    @Override
    public Counter getCounter(MessageChannel channel, String name) {
      throw new RuntimeException(name);
    }

    @Override
    public void notifyTransaction(NodeID nodeID, int numTxns) {
      throw new ImplementMe();
    }

    @Override
    public void notifyReadOperations(MessageChannel channel, int numObjectsRequested) {
      this.channelReqCounter.increment(numObjectsRequested);
    }

    @Override
    public void notifyTransactionAckedFrom(NodeID nodeID) {
      throw new ImplementMe();

    }

    @Override
    public void notifyTransactionBroadcastedTo(NodeID nodeID) {
      throw new ImplementMe();

    }

    @Override
    public void notifyServerMapRequest(ServerMapRequestType type, MessageChannel channel, int numRequests) {
      throw new ImplementMe();
    }
  }

}

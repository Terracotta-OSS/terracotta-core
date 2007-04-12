/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.handler;

import com.tc.async.impl.MockStage;
import com.tc.exception.ImplementMe;
import com.tc.net.protocol.tcm.ChannelID;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.object.ObjectID;
import com.tc.object.TestRequestManagedObjectMessage;
import com.tc.object.net.ChannelStats;
import com.tc.objectserver.core.api.ServerConfigurationContext;
import com.tc.objectserver.core.impl.TestServerConfigurationContext;
import com.tc.objectserver.impl.TestObjectRequestManager;
import com.tc.objectserver.l1.api.TestClientStateManager;
import com.tc.stats.counter.Counter;
import com.tc.stats.counter.CounterImpl;

import java.util.HashSet;
import java.util.Set;

import junit.framework.TestCase;

public class ManagedObjectRequestHandlerTest extends TestCase {

  public void testObjectRequestCounter() {
    TestChannelStats channelStats = new TestChannelStats();
    Counter channelReqCounter = new CounterImpl(666L);
    Counter channelRemCounter = new CounterImpl(69L);
    Counter requestCounter = new CounterImpl(0L);
    Counter removeCounter = new CounterImpl(0L);
    channelStats.reqCounter = channelReqCounter;
    channelStats.remCounter = channelRemCounter;

    TestServerConfigurationContext context = new TestServerConfigurationContext();
    context.clientStateManager = new TestClientStateManager();
    context.addStage(ServerConfigurationContext.RESPOND_TO_OBJECT_REQUEST_STAGE, new MockStage("yo"));
    context.channelStats = channelStats;

    ManagedObjectRequestHandler handler = new ManagedObjectRequestHandler(requestCounter, removeCounter,
                                                                          new TestObjectRequestManager());
    handler.initialize(context);

    TestRequestManagedObjectMessage msg = new TestRequestManagedObjectMessage();
    HashSet s = new HashSet();
    s.add(new ObjectID(1));
    msg.setObjectIDs(s);
    Set removed = makeRemovedSet(31);
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

  private Set makeRemovedSet(int num) {
    Set rv = new HashSet();
    for (int i = 0; i < num; i++) {
      rv.add(new ObjectID(i));
    }
    return rv;
  }

  private static class TestChannelStats implements ChannelStats {
    Counter remCounter;
    Counter reqCounter;

    public Counter getCounter(MessageChannel channel, String name) {
      if (OBJECT_FLUSH_RATE.equals(name)) {
        return remCounter;
      } else if (OBJECT_REQUEST_RATE.equals(name)) { return reqCounter; }
      throw new RuntimeException(name);
    }

    public void notifyTransaction(ChannelID channelID) {
      throw new ImplementMe();

    }
  }

}

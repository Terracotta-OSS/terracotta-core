/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.util;

import com.tc.util.msg.TickerTokenMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

import junit.framework.TestCase;

public class TickerTokenManagerTest extends TestCase {

  private static final int NUMBER_OF_HANDLERS = 10;

  public void test() {
    List<TickerTuple> tuples = createTickerTuples();

    TestTickerTokenManager tickerTokenManager = tuples.get(0).getTickerTokenManager();
    TickerTokenHandleImpl handle = new TickerTokenHandleImpl();
    tickerTokenManager.startTicker(TestTickerToken.class, handle);
  }

 

  private List<TickerTuple> createTickerTuples() {
    List<TickerTuple> tickerTuples = new ArrayList<TickerTuple>();

    for (int i = 0; i < NUMBER_OF_HANDLERS; i++) {
      final Queue<TickerTokenMessage> mQueue = new LinkedBlockingQueue<TickerTokenMessage>();
      final TestTickerTokenManager manager = new TestTickerTokenManager(i, 100, mQueue, NUMBER_OF_HANDLERS);
      manager.registerTickerTokenFactory(TestTickerToken.class, new TestTickerTokenFactory());
      TickerTuple tickerTuple = new TickerTuple(mQueue, manager);
      tickerTuples.add(tickerTuple);
    }
    return tickerTuples;
  }

  private static class TickerTuple {

    private final Queue<TickerTokenMessage> messageQueue;
    private final TestTickerTokenManager        tickerTokenManager;

    public TickerTuple(Queue<TickerTokenMessage> messageQueue, TestTickerTokenManager tickerTokenManager) {
      this.messageQueue = messageQueue;
      this.tickerTokenManager = tickerTokenManager;
    }

    public Queue<TickerTokenMessage> getMessageQueue() {
      return messageQueue;
    }

    public TestTickerTokenManager getTickerTokenManager() {
      return tickerTokenManager;
    }

  }

}

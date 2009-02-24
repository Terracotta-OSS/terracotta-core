/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.util.tickertoken;

import com.tc.util.tickertoken.msg.TickerTokenMessage;

import java.util.Queue;

public class TestTickerTokenManager extends TickerTokenManagerImpl {
  
  private final Queue<TickerTokenMessage> mQueue;
  
  public TestTickerTokenManager(int id, int timerPeriod, Queue<TickerTokenMessage> mQueue, int tokenCount) {
    super(id, timerPeriod, tokenCount);
    this.mQueue = mQueue;
  }

 
  @Override
  public void sendMessage(TickerTokenMessage message) {
    mQueue.add(message);
  }
  
}
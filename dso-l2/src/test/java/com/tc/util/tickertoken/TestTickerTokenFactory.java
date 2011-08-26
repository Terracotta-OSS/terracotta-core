/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.util.tickertoken;

import com.tc.util.tickertoken.msg.TickerTokenMessage;

public class TestTickerTokenFactory implements TickerTokenFactory {

  public TickerTokenMessage createMessage(TickerToken token) {
    TestTickerTokenMessage message = new TestTickerTokenMessage(token);
    return message;
  }

  public TickerToken createToken(TickerTokenMessage message) {
    return message.getTickerToken();
  }

  public TickerToken createTriggerToken(int id, int startTick, int tickerCount) {
    return new TestTickerToken(id, startTick, tickerCount);
  }
}

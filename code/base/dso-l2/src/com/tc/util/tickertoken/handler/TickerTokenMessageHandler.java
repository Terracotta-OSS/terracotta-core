/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.util.tickertoken.handler;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.EventContext;
import com.tc.util.tickertoken.TickerToken;
import com.tc.util.tickertoken.TickerTokenManager;
import com.tc.util.tickertoken.msg.TickerTokenMessage;

public class TickerTokenMessageHandler extends AbstractEventHandler {

  private final TickerTokenManager tickerManager;

  public TickerTokenMessageHandler(TickerTokenManager tickerManager) {
    this.tickerManager = tickerManager;
  }

  @Override
  public void handleEvent(EventContext context) {
    if (context instanceof TickerTokenMessage) {
      TickerTokenMessage message = (TickerTokenMessage) context;
      TickerToken token = message.getTickerToken();
      this.tickerManager.receive(token);
    } else {
      throw new AssertionError("Invalid Message TickerTokenMessageHandler " + context.getClass());
    }
  }
}

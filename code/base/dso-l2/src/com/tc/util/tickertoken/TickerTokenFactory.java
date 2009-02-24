/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.util.tickertoken;

import com.tc.util.tickertoken.msg.TickerTokenMessage;

public interface TickerTokenFactory {

  public TickerToken createTriggerToken(int id, int startTick, int totalTickers);
  
  public TickerToken createToken(TickerTokenMessage message);

  public TickerTokenMessage createMessage(TickerToken token);
}

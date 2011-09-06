/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.util.tickertoken.msg;

import com.tc.async.api.EventContext;
import com.tc.net.groups.AbstractGroupMessage;
import com.tc.net.groups.MessageID;
import com.tc.util.tickertoken.TickerToken;


public abstract class TickerTokenMessage extends AbstractGroupMessage implements EventContext {

  protected TickerToken tickerToken;

  public TickerTokenMessage() {
    super(-1);
  }

  public TickerTokenMessage(int type) {
    super(type);
  }

  public TickerTokenMessage(int type, MessageID requestID) {
    super(type, requestID);
  }

  public TickerToken getTickerToken() {
    return this.tickerToken;
  }
}

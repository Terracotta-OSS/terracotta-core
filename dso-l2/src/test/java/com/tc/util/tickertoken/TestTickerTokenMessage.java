/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.util.tickertoken;

import com.tc.io.TCByteBufferInput;
import com.tc.io.TCByteBufferOutput;
import com.tc.util.tickertoken.msg.TickerTokenMessage;


public class TestTickerTokenMessage extends TickerTokenMessage {

  public TestTickerTokenMessage(TickerToken tickerToken) {
    this.tickerToken = tickerToken;
  }

  @Override
  protected void basicDeserializeFrom(TCByteBufferInput in) {
   //
  }

  @Override
  protected void basicSerializeTo(TCByteBufferOutput out) {
    //
  }
}
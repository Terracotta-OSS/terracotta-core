/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.util.tickertoken;

import com.tc.util.tickertoken.msg.TickerTokenMessage;

import junit.framework.TestCase;

public class TickerTokenMessageTest extends TestCase {
  
  public void testMessageAndFactory() {
    TestTickerTokenFactory factory = new TestTickerTokenFactory();
    TickerToken triggerToken = factory.createTriggerToken(0, 1, 1);
    
    TickerTokenMessage message = factory.createMessage(triggerToken);
    
    assertEquals(triggerToken, message.getTickerToken());
    
    TickerToken token  = factory.createToken(message);
    
    assertEquals(message.getTickerToken(), token);
    
    
  }

}

/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.util;

import com.tc.util.msg.TickerTokenMessage;

import junit.framework.TestCase;

public class TickerTokenTest extends TestCase {

  
  public void testToken() {
    final int primaryId = 0;
    int tickValue = 0;
    TestTickerToken token = new TestTickerToken(primaryId, tickValue, 1);
    assertEquals(primaryId, token.getPrimaryID());
    
//    assertEquals(0, token.getTokenStateMap().size());
//    
//    token.collectToken(0, false);
//    token.collectToken(1, false);
//    token.collectToken(2, true);
//    token.collectToken(3, true);
//    
//    assertEquals(4, token.getTokenStateMap().size());
//    
//    assertFalse(token.getTokenStateMap().get(0));
//    assertFalse(token.getTokenStateMap().get(1));
//    assertTrue(token.getTokenStateMap().get(2));
//    assertTrue(token.getTokenStateMap().get(3));
//    
  }
  
  public void testTokenWithFactory() {
    TestTickerTokenFactory factory = new TestTickerTokenFactory();
    
    TickerToken triggerToken = factory.createTriggerToken(0, 1, 1);
    assertEquals(0, triggerToken.getPrimaryID());
    assertEquals(1, triggerToken.getStartTick());
    
  //  assertEquals(0, triggerToken.getTokenStateMap().size());
    
    
    TickerTokenMessage message = factory.createMessage(triggerToken);
    TickerToken messageToken = message.getTickerToken();
    
    assertEquals(0, messageToken.getPrimaryID());
    assertEquals(1, messageToken.getStartTick());
    
    TickerToken triggerToken2 = factory.createTriggerToken(1, 2, 1);
    
    assertEquals(1, triggerToken2.getPrimaryID());
    assertEquals(2, triggerToken2.getStartTick());
   
   // assertEquals(0, triggerToken2.getTokenStateMap().size());
    
  }
  
}

/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.util.tickertoken;

import com.tc.util.tickertoken.msg.TickerTokenMessage;

import junit.framework.TestCase;

public class TickerTokenTest extends TestCase {

  
  public void testToken() {
    final int primaryId = 0;
    int tickValue = 0;
    TestTickerToken token = new TestTickerToken(primaryId, tickValue, 4);
    assertEquals(primaryId, token.getPrimaryID());
    
    assertEquals(0, token.getTokenStateMap().size());
    
    CollectContext context = new CollectContext();
    context.collect(TestTickerToken.DIRTY_STATE, false);
    token.collectToken(0, context);
    context.collect(TestTickerToken.DIRTY_STATE, false);
    token.collectToken(1, context);
    context.collect(TestTickerToken.DIRTY_STATE, true);
    token.collectToken(2, context);
    context.collect(TestTickerToken.DIRTY_STATE, true);
    token.collectToken(3, context);
    
    assertEquals(4, token.getTokenStateMap().size());
    
    assertFalse(token.getTokenStateMap().get(0));
    assertFalse(token.getTokenStateMap().get(1));
    assertTrue(token.getTokenStateMap().get(2));
    assertTrue(token.getTokenStateMap().get(3));
    
    assertFalse(token.evaluateComplete());
    
    context.collect(TestTickerToken.DIRTY_STATE, false);
    token.collectToken(2, context);
    context.collect(TestTickerToken.DIRTY_STATE, false);
    token.collectToken(3, context);
    
    assertTrue(token.evaluateComplete());
    
  }
  
  public void testTokenWithFactory() {
    TestTickerTokenFactory factory = new TestTickerTokenFactory();
    
    TickerToken triggerToken = factory.createTriggerToken(0, 1, 2);
    assertEquals(0, triggerToken.getPrimaryID());
    assertEquals(1, triggerToken.getStartTick());
    assertFalse(triggerToken.evaluateComplete());
    
    CollectContext context = new CollectContext();
    context.collect(TestTickerToken.DIRTY_STATE, false);
    triggerToken.collectToken(0, context);
    
    context.collect(TestTickerToken.DIRTY_STATE, false);
   
    triggerToken.collectToken(1, context);
    
    
    TickerTokenMessage message = factory.createMessage(triggerToken);
    TickerToken messageToken = message.getTickerToken();
    
    assertEquals(0, messageToken.getPrimaryID());
    assertEquals(1, messageToken.getStartTick());
    
    TickerToken triggerToken2 = factory.createTriggerToken(1, 2, 1);
    
    assertEquals(1, triggerToken2.getPrimaryID());
    assertEquals(2, triggerToken2.getStartTick());
  }
  
}

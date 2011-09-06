/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.util.tickertoken;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class TestTickerToken extends TickerTokenImpl implements TickerToken {

  public static final String   DIRTY_STATE   = "dirty_state";

  public Map<Integer, Boolean> tokenStateMap = new HashMap<Integer, Boolean>();

  public TestTickerToken(int primaryID, int startTick, int totalTickers) {
    super(primaryID, startTick, totalTickers);
  }

  @Override
  public void collectToken(int id, CollectContext context) {
    this.tokenStateMap.put(id, (Boolean) context.getValue(DIRTY_STATE));

  }

  public Map<Integer, Boolean> getTokenStateMap() {
    return tokenStateMap;
  }

  @Override
  public boolean evaluateComplete() {
    boolean complete = true;
    if (tokenStateMap.size() < totalTickers) { return false; }

    for (Iterator<Boolean> iter = tokenStateMap.values().iterator(); iter.hasNext();) {
      boolean state = iter.next();
      if (state) {
        complete = false;
      }
    }

    return complete;
  }


  public boolean evaluateEqual(TickerToken token) {
    TestTickerToken compareTo = (TestTickerToken)token;
    
    return tokenStateMap.equals(compareTo.tokenStateMap);
  
  }

}
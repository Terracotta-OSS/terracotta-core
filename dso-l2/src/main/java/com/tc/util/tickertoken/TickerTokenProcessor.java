/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.util.tickertoken;

public interface TickerTokenProcessor {
  
  public static final TickerTokenProcessor NULL_TICKER_TOKEN_PROCESSOR = new TickerTokenProcessor() {

    public TickerToken processToken(TickerToken token) {
      return token;
    }
    
  };
  
  public TickerToken processToken( TickerToken token );
  
}

/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.util.tickertoken;

public interface TickerTokenManager {

  public abstract int getId();

  public abstract TickerTokenHandle startTicker(String identifier, Class tickerTokenType);

  public abstract void cancelTicker(String identifier);

  public abstract void send(TickerToken token);

  public abstract void receive(TickerToken token);

}
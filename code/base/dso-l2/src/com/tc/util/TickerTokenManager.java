/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.util;

public interface TickerTokenManager {

  public abstract int getId();

  public abstract TickerTokenHandle startTicker(Class tickerTokenType, TickerTokenHandle handle);

  public abstract void cancelTicker(TickerTokenHandle handle);

  public abstract void send(TickerToken token);

  public abstract void receive(TickerToken token);

}
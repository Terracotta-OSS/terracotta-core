/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.util;

public interface TickerTokenManager {

  public abstract int getId();

  public abstract TickerTokenHandle createHandle(String identifier);

  public abstract void startTicker(Class tickerTokenType, String identifier);

  public abstract void cancelTicker(String identifier);

  public abstract void send(TickerToken token);

  public abstract void receive(TickerToken token);

}
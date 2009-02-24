/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.util.tickertoken;

public class TickerTokenException extends RuntimeException {

  public TickerTokenException(String message) {
    super(message);
  }

  public TickerTokenException(Throwable reason) {
    super(reason);
  }

}

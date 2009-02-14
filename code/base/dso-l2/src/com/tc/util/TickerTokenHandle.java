/*
 * All content copyright (c) 2003-2009 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.util;

public interface TickerTokenHandle {
  
  public void complete();
  
  public void cancel();
  
  public void setKey(TickerTokenKey key);
  
  public TickerTokenKey getKey();

}

/*
 * All content copyright (c) 2003-2009 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.util;

public class TickerTokenKey {
  private final Class classType;
  private final int   primaryID;
  private final int   startTick;

  public TickerTokenKey(TickerToken token) {
    this(token.getClass(), token.getPrimaryID(), token.getStartTick());
  }

  public TickerTokenKey(Class classType, int primaryID, int startTick) {
    this.classType = classType;
    this.primaryID = primaryID;
    this.startTick = startTick;
  }

  public Class getClassType() {
    return classType;
  }

  public int getPrimaryID() {
    return primaryID;
  }

  public int getStartTick() {
    return startTick;
  }

  
  @Override
  public int hashCode() {
    return classType.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof TickerTokenKey) {
      TickerTokenKey key = (TickerTokenKey) obj;
      return (getClassType().equals(key.getClassType()) && getPrimaryID() == key.getPrimaryID() && getStartTick() == key
          .getStartTick());
    }
    return false;
  }

}

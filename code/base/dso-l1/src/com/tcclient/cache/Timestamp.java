/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tcclient.cache;

import java.util.Date;

public class Timestamp {
  private long timeToExpireMillis = 0;
  private long timeToDieMillis = 0;

  public Timestamp(long millis, long timeToDieMillis) {
    this.timeToExpireMillis = millis;
    this.timeToDieMillis = timeToDieMillis;
  }

  public synchronized long getInvalidatedTimeMillis() {
    if (timeToDieMillis <= 0) { return timeToExpireMillis; }
    
    return (timeToExpireMillis < timeToDieMillis)? timeToExpireMillis : timeToDieMillis;
  }
  
  public synchronized long getExpiredTimeMillis() {
    return timeToExpireMillis;
  }

  public synchronized void setExpiredTimeMillis(long millis) {
    if (timeToDieMillis <= 0 || timeToDieMillis > millis) {
      this.timeToExpireMillis = millis;
    }
  }

  public String toString() {
    return new Date(getInvalidatedTimeMillis()).toString();
  }
}

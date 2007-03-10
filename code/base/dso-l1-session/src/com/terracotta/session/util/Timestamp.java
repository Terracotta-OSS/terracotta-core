/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.terracotta.session.util;

import java.util.Date;

public class Timestamp {

  private long millis = 0;

  public Timestamp(long millis) {
    this.millis = millis;
  }

  public synchronized long getMillis() {
    return millis;
  }

  public synchronized void setMillis(long millis) {
    this.millis = millis;
  }

  public String toString() {
    return new Date(getMillis()).toString();
  }
}

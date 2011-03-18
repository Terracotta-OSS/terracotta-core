/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.core;

public class ConnectionAddressIterator {

  private final ConnectionInfo[] cis;
  private int                    current = -1;

  public ConnectionAddressIterator(ConnectionInfo[] cis) {
    this.cis = cis;
  }

  public boolean hasNext() {
    return current < (cis.length - 1);
  }

  public ConnectionInfo next() {
    if (!hasNext()) return null;
    return cis[++current];
  }
}

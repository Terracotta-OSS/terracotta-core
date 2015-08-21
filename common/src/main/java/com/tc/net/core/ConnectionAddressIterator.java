/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
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

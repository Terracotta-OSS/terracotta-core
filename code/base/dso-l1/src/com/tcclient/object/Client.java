/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tcclient.object;

import java.util.LinkedList;
import java.util.List;

/**
 * Representation of a client
 */
public class Client {
  private final List myEvents    = new LinkedList();
  private long       lastChecked = System.currentTimeMillis();

  public void add(DistributedMethodCall dmc) {
    myEvents.add(dmc);
  }

  public DistributedMethodCall next() {
    return (DistributedMethodCall) myEvents.remove(0);
  }

  public boolean isEmpty() {
    lastChecked = System.currentTimeMillis();
    return myEvents.isEmpty();
  }

  public boolean isTimedOut() {
    return System.currentTimeMillis() - lastChecked > 60 * 1000 * 10;
  }
  
  public String toString() {
    return super.toString() + "[" + myEvents + "]";
  }
}
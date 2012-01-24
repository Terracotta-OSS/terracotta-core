/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import com.tctest.builtin.ArrayList;

public class DisconnectingALClient {

  final ArrayList<ArrayList> arrayList = new ArrayList<ArrayList>();
  private final String       id;
  private final long         timeout;

  public DisconnectingALClient(String id, long runtime) {
    this.id = id;
    this.timeout = runtime > 0 ? System.currentTimeMillis() + runtime : 0;
  }

  private void run() {
    long total = 0;
    while (keepGoing()) {
      total = 0;
      synchronized (arrayList) {
        for (ArrayList inner : arrayList) {
          for (Object o : inner) {
            // act like we're doing something with the value
            total += o.hashCode();
          }
        }
      }
    }
    System.err.println("DisconnectingALClient " + id + " stopping with total " + total);
  }

  private boolean keepGoing() {
    return timeout == 0L || System.currentTimeMillis() < timeout;
  }

  public static void main(String[] args) {
    String id = args[0];
    long runtime = Long.parseLong(args[1]);
    new DisconnectingALClient(id, runtime).run();
  }

}

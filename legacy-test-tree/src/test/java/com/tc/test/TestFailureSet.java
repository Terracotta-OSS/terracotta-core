/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.test;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class TestFailureSet {
  private final List list = new ArrayList();

  public void put(TestFailure f) {
    synchronized (list) {
      list.add(f);
    }
  }

  public String toString() {
    StringBuffer buf = new StringBuffer("Test failures...\n");
    synchronized (list) {
      for (Iterator i = list.iterator(); i.hasNext();) {
        TestFailure f = (TestFailure) i.next();
        buf.append(f + "\n");
      }
    }
    return buf.toString();
  }

  public int size() {
    synchronized (list) {
      return list.size();
    }
  }
}
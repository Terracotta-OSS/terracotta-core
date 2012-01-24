/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import com.tctest.builtin.HashMap;

import java.util.Iterator;
import java.util.Map;

/**
 * Reads from the shared map and that's it
 */
public class TestReader {

  public final static int READ_COUNT = 2000;

  private final Map       stuff      = new HashMap();
  private final String    name;

  public TestReader(String name) {
    this.name = name;
  }

  public void read() {
    try {
      int count = 0;
      while (count++ < READ_COUNT) {
        doARead();
      }
    } catch (StackOverflowError e) {
      e.printStackTrace(System.out);
      throw e;
    }
  }

  public void doARead() {
    synchronized (stuff) {
      // System.out.println("begin Reading:" + name);
      if (stuff.size() > 0 && (stuff.size() % 4) == 0) {
        for (Iterator i = stuff.values().iterator(); i.hasNext();) {
          i.next();
        }
      }
    }
    // System.out.println("DONE Reading:" + name);
  }

  protected String getName() {
    return name;
  }

}

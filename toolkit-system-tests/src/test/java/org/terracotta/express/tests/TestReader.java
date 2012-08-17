/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.express.tests;

import java.util.HashMap;
import java.util.Map;

/**
 * Reads from the shared map and that's it
 */
public class TestReader {

  public final static int READ_COUNT = 1000;

  private Map             stuff      = new HashMap();
  private final String    name;

  public TestReader(Map map, String name) {
    this.stuff = map;
    this.name = name;
  }

  public void read() {
    try {
      int count = 0;
      System.out.println("begin Reading:" + name);
      while (count++ < READ_COUNT) {
        doARead();
      }
      System.out.println("DONE Reading:" + name);
    } catch (StackOverflowError e) {
      e.printStackTrace(System.out);
      throw e;
    }
  }

  public void doARead() {
    int size = stuff.size();
    if (size > 0 && (size % 4) == 0) {
      for (int i = 0; i < size; ++i) {
        stuff.get(Integer.valueOf(i));
      }
    }
  }

  protected String getName() {
    return name;
  }

}

/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.express.tests;

import com.tc.util.concurrent.ThreadUtil;

import java.util.Map;
import java.util.Random;

/**
 * writer for the FastReadSlowWriteTest
 */
public class TestWriter {
  public final static int WRITE_COUNT = 100;
  public final static int WRITE_DELAY = 10;

  private final Map       stuff;
  private final Random    r           = new Random();
  private final String    name;

  public TestWriter(Map map, String name) {
    this.stuff = map;
    this.name = name;
  }

  public void write() {
    int count = 0;
    while (count++ < WRITE_COUNT) {
      doAWrite();
      if (count % 10 == 0) {
        System.out.println("writing " + name);
      }
      ThreadUtil.reallySleep(WRITE_DELAY);
    }
  }

  public void doAWrite() {
    stuff.put(Integer.valueOf(stuff.size() + 1), "" + r.nextLong());
  }
}

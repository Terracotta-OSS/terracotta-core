/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import com.tc.util.concurrent.ThreadUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * writer for the FastReadSlowWriteTest
 */
public class TestWriter {
  public final static int WRITE_COUNT = 100;
  public final static int WRITE_DELAY = 10;

  private final Map       stuff       = new HashMap();
  private final Random    r           = new Random();

  public void write() {
    int count = 0;
    while (count++ < WRITE_COUNT) {
      doAWrite();
      ThreadUtil.reallySleep(WRITE_DELAY);
    }
  }

  public void doAWrite() {
    synchronized (stuff) {
      stuff.put(Integer.valueOf(stuff.size() + 1), "" + r.nextLong());
    }
  }

}

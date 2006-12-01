/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.logging;

import com.tc.util.Assert;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * A simple thread that copies one stream to another. Useful for copying a process's output/error streams to this
 * process's output/error streams.
 */
public class TCStreamLogger extends Thread {

  protected final BufferedReader in;
  protected final TCLogger       out;
  protected final LogLevel       level;

  public TCStreamLogger(InputStream stream, TCLogger logger, LogLevel level) {
    Assert.assertNotNull(stream);

    this.in = new BufferedReader(new InputStreamReader(stream));
    this.out = logger;
    this.level = level;
  }

  public void run() {
    String line;
    try {
      while ((line = in.readLine()) != null) {
        out.log(level, line);
      }
    } catch (IOException e) {
      out.log(level, "Exception reading InputStream: " + e);
    }
  }

}
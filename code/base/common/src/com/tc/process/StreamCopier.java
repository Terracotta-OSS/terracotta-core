/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.process;

import org.apache.commons.io.CopyUtils;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.util.Assert;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * A simple thread that copies one stream to another. Useful for copying a process's output/error streams to this
 * process's output/error streams.
 */
public class StreamCopier extends Thread {

  private static final TCLogger logger = TCLogging.getLogger(StreamCollector.class);

  protected final InputStream   in;
  protected final OutputStream  out;

  public StreamCopier(InputStream stream, OutputStream out) {
    Assert.assertNotNull(stream);
    Assert.assertNotNull(out);

    this.in = stream;
    this.out = out;
    setName("Stream Copier");
    setDaemon(true);
  }

  public void run() {
    try {
      CopyUtils.copy(this.in, this.out);
    } catch (IOException ioe) {
      logger.warn("Couldn't copy stream", ioe);
    }
  }

}
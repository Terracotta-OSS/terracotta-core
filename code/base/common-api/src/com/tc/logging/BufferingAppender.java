/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.logging;

import org.apache.log4j.Appender;
import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.spi.LoggingEvent;

import EDU.oswego.cs.dl.util.concurrent.BoundedBuffer;

/**
 * An {@link Appender} that simply buffers records (in a bounded queue) until they're needed. This is used for making
 * sure all logging information gets to the file; we buffer records created before logging gets sent to a file, then
 * send them there.
 */
public class BufferingAppender extends AppenderSkeleton {

  private final BoundedBuffer buffer;
  private boolean             on;

  public BufferingAppender(int maxCapacity) {
    this.buffer = new BoundedBuffer(maxCapacity);
    this.on = true;
  }

  protected synchronized void append(LoggingEvent event) {
    if (on) {
      try {
        this.buffer.offer(event, 0);
      } catch (InterruptedException ie) {
        Thread.currentThread().interrupt();
      }
    }
  }

  public boolean requiresLayout() {
    return false;
  }

  public void close() {
    // nothing needs to be here.
  }

  public void stopAndSendContentsTo(Appender otherAppender) {
    synchronized (this) {
      on = false;
    }

    while (true) {
      try {
        LoggingEvent event = (LoggingEvent) this.buffer.poll(0);
        if (event == null) break;
        otherAppender.doAppend(event);
      } catch (InterruptedException ie) {
        Thread.currentThread().interrupt();
      }
    }
  }

}

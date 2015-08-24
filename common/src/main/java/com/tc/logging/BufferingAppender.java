/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.logging;

import org.apache.log4j.Appender;
import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.spi.LoggingEvent;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * An {@link Appender} that simply buffers records (in a bounded queue) until they're needed. This is used for making
 * sure all logging information gets to the file; we buffer records created before logging gets sent to a file, then
 * send them there.
 */
public class BufferingAppender extends AppenderSkeleton {

  private final BlockingQueue<LoggingEvent> buffer;
  private boolean             on;

  public BufferingAppender(int maxCapacity) {
    this.buffer = new ArrayBlockingQueue<LoggingEvent>(maxCapacity);
    this.on = true;
  }

  @Override
  protected synchronized void append(LoggingEvent event) {
    if (on) {
      this.buffer.offer(event);
    }
  }

  @Override
  public boolean requiresLayout() {
    return false;
  }

  @Override
  public void close() {
    // nothing needs to be here.
  }

  public void stopAndSendContentsTo(Appender otherAppender) {
    synchronized (this) {
      on = false;
    }

    while (true) {
      LoggingEvent event = this.buffer.poll();
      if (event == null) break;
      otherAppender.doAppend(event);
    }
  }

}

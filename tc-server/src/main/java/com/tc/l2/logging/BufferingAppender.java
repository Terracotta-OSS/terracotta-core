/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Terracotta Core.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
 */
package com.tc.l2.logging;

import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.Context;
import ch.qos.logback.core.OutputStreamAppender;
import ch.qos.logback.core.joran.spi.ConsoleTarget;
import static com.tc.l2.logging.TCLogbackLogging.STDOUT_APPENDER;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;

/**
 * An {@link Appender} that simply buffers records (in a bounded queue) until they're needed. This is used for making
 * sure all logging information gets to the file; we buffer records created before logging gets sent to a file, then
 * send them there.
 */
public class BufferingAppender extends OutputStreamAppender<ILoggingEvent> {

  private final Queue<ILoggingEvent> buffer;
  private boolean bufferLogs = true;

  public BufferingAppender() {
    this.buffer = new ConcurrentLinkedQueue<>();
    setName(STDOUT_APPENDER);
    setImmediateFlush(true);
  }

  @Override
  public void start() {
    if (this.getEncoder() == null) {
      PatternLayoutEncoder defaultEncoder = new PatternLayoutEncoder();
      defaultEncoder.setPattern("%d %p - %m%n");
      defaultEncoder.setParent(this);
      defaultEncoder.setContext(context);
      defaultEncoder.start();
      this.setEncoder(defaultEncoder);
    }
    if (this.getOutputStream() == null) {
      this.setOutputStream(ConsoleTarget.SystemOut.getStream());
    }
    super.start();
  }
  
  @Override
  public void setContext(Context context) {
    super.setContext(context);
  }

  @Override
  protected void append(ILoggingEvent eventObject) {
    if (bufferLogs) {
      while (buffer.size() > 1024) {
        buffer.poll();
      }
      buffer.add(eventObject);
    }
    super.append(eventObject);
  }

  public void sendContentsTo(Consumer<ILoggingEvent> otherAppender) {
    while (true) {
      ILoggingEvent event = this.buffer.poll();
      if (event == null) break;
      otherAppender.accept(event);
    }
    bufferLogs = false;
  }
}

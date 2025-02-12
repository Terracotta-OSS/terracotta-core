/*
 *  Copyright Terracotta, Inc.
 *  Copyright IBM Corp. 2024, 2025
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
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
      while (buffer.size() > 4096) {
        buffer.poll();
      }
      buffer.add(eventObject);
    } else {
      //  be paranoid about not losing any buffer lines in the
      // buffer during transition.  
      drainBuffer(null);
      super.append(eventObject);
    }
  }

  private void drainBuffer(Consumer<ILoggingEvent> other) {
    while (true) {
      ILoggingEvent event = this.buffer.poll();
      if (event == null) break;
      super.append(event);
      if (other != null) {
        other.accept(event);
      }
    }
  }

  public void sendContentsTo(Consumer<ILoggingEvent> otherAppender) {
    drainBuffer(otherAppender);
    bufferLogs = false;
  }
}

/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
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

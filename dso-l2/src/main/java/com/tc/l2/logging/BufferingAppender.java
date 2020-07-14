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

import ch.qos.logback.core.Appender;
import ch.qos.logback.core.ConsoleAppender;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Predicate;

/**
 * An {@link Appender} that simply buffers records (in a bounded queue) until they're needed. This is used for making
 * sure all logging information gets to the file; we buffer records created before logging gets sent to a file, then
 * send them there.
 */
public class BufferingAppender<E> extends ConsoleAppender<E> {

  private final Queue<E> buffer;
  private Predicate<E> printToConsole = e->false;
  private boolean doBuffer = true;

  public BufferingAppender() {
    this.buffer = new ConcurrentLinkedQueue<>();
  }

  public void setConsoleFilter(Predicate<E> filter) {
    printToConsole = filter;
  }

  public void setConsole(boolean console) {
    this.printToConsole = e->console;
  }

  public void disableBuffering() {
    this.doBuffer = false;
    this.printToConsole = e->true;
    sendContentsTo(this);
  }

  @Override
  public void doAppend(E eventObject) {
    if (printToConsole.test(eventObject)) {
      super.doAppend(eventObject);
    }
    if (doBuffer) {
      buffer.add(eventObject);
    }
  }

  public void sendContentsTo(Appender<E> otherAppender) {
    while (true) {
      E event = this.buffer.poll();
      if (event == null) break;
      otherAppender.doAppend(event);
    }
  }

}

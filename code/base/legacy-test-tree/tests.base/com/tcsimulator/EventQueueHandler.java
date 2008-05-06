/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tcsimulator;

import EDU.oswego.cs.dl.util.concurrent.LinkedQueue;


public class EventQueueHandler implements Runnable {
  private final LinkedQueue queue;
  private final Setup       setup;

  public EventQueueHandler(LinkedQueue queue, Setup setup) {
    this.queue = queue;
    this.setup = setup;
  }

  public void run() {
    while (true) {
      try {
        Object obj = this.queue.take();
        if (obj instanceof QueueEvent) {
          QueueEvent event = (QueueEvent) obj;
          if (event.getAction() == QueueEvent.SERVER_CRASH) {
            setup.crashServer();
          } else if (event.getAction() == QueueEvent.SERVER_RESTART) {
            setup.restartServer();
          }
        } else {
          throw new AssertionError("EventQueue was populated with a non-QueueEvent object.");
        }
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
  }
}

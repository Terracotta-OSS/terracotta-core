/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tcsimulator.listener;

import EDU.oswego.cs.dl.util.concurrent.LinkedQueue;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

public class QueuePrinter implements Runnable {

  private final LinkedQueue queue;
  private final Collection  outputStreams;

  public QueuePrinter(LinkedQueue queue, PrintStream out) {
    this.queue = queue;
    this.outputStreams = new ArrayList();
    this.outputStreams.add(out);
  }

  public void setAdditionalOutput(PrintStream out) {
    this.outputStreams.add(out);
  }

  public void run() {
    while (true) {
      try {
        Object obj = this.queue.take();
        for (Iterator i = outputStreams.iterator(); i.hasNext();) {
          ((PrintStream) i.next()).println(obj);
        }
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
    }
  }

}

/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.test.server.appserver.load;

import EDU.oswego.cs.dl.util.concurrent.LinkedQueue;

import com.tc.test.server.util.HttpUtil;

public class RequestQueueHandler extends Thread {

  private final LinkedQueue      queue;

  public RequestQueueHandler(LinkedQueue queue) {
    this.queue = queue;
  }

  public void run() {
    while (true) {
      try {
        Object obj = this.queue.take();
        if (obj instanceof ExitRequest) {
          return;
        } else if (obj instanceof Request) {
          Request request = (Request) obj;
          request.setExitQueueTime();
          HttpUtil.getInt(request.getUrl(), request.getClient());
          request.setProcessCompletionTime();
        } else {
          throw new AssertionError("EventQueue was populated with a non-Request object.");
        }
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
  }
}

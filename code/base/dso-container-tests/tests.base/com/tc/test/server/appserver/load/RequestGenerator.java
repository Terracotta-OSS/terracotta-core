/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.test.server.appserver.load;

import org.apache.commons.httpclient.HttpClient;

import java.net.URL;
import java.util.List;

import EDU.oswego.cs.dl.util.concurrent.LinkedQueue;

public class RequestGenerator extends Thread {

  // private static final long NANOSEC_PER_SEC = 1000000000;
  // private static final long NANOSEC_PER_MILLISEC = 1000000;
  private static final long MILLISEC_PER_SEC = 1000;

  // generation rate unit: # req/sec
  private final int         generationRate;
  private final int         appserverID;
  private final URL         url;
  private final LinkedQueue requestQueue;
  private final Object[]    clients;
  private final long        test_duration;
  private final List        requests;
  private final int         clientsPerNode;
  private final Thread      requestQueueHandler;

  public RequestGenerator(int generationRate, int clientsPerNode, int appserverID, URL url, List clients,
                          long duration, List requests) {
    this.generationRate = generationRate;
    this.clientsPerNode = clientsPerNode;
    this.appserverID = appserverID;
    this.url = url;
    this.clients = clients.toArray();
    this.test_duration = duration;
    this.requests = requests;
    this.requestQueue = new LinkedQueue();
    this.requestQueueHandler = new RequestQueueHandler(this.requestQueue);
    this.requestQueueHandler.start();
  }

  public void run() {
    // nanosec per request
    // long creationInterval = NANOSEC_PER_SEC / this.generationRate;

    // millisec per request
    long creationInterval = MILLISEC_PER_SEC / this.generationRate;

    int curIndex = 0;
    long startTime, endTime, diff, sleepTime, dur = this.test_duration;

    do {
      // startTime = System.nanoTime();
      startTime = System.currentTimeMillis();

      Request r = new DataKeeperRequest((HttpClient) this.clients[curIndex], this.appserverID, this.url);
      curIndex = (curIndex + 1) % clientsPerNode;
      this.requests.add(r);

      try {
        r.setEnterQueueTime();
        this.requestQueue.put(r);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }

      // endTime = System.nanoTime();
      endTime = System.currentTimeMillis();
      diff = endTime - startTime;
      dur -= diff;
      sleepTime = creationInterval - diff;
      if (sleepTime < 0) {
        continue;
      }

      try {
        // Thread.sleep(NANO_PER_MILLISEC / sleepTime, (int) (NANO_PER_MILLISEC % sleepTime));
        Thread.sleep(sleepTime);
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }

      dur -= sleepTime;
    } while (dur > 0);

    try {
      this.requestQueue.put(new ExitRequest());
      this.requestQueueHandler.join();
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }
}

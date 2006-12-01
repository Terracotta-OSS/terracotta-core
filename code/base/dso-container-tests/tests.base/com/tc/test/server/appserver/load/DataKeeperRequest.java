/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.test.server.appserver.load;

import org.apache.commons.httpclient.HttpClient;

import com.tc.util.Assert;

import java.net.URL;

public class DataKeeperRequest implements Request {

  private static final int UNDEFINED = -1;
  private long             enterQueueTime;
  private long             exitQueueTime;
  private long             processCompletionTime;
  private final HttpClient client;
  private final int        appserverID;
  private final URL        url;

  public DataKeeperRequest(HttpClient client, int appserverID, URL url) {
    this.client = client;
    this.appserverID = appserverID;
    this.url = url;
    this.enterQueueTime = UNDEFINED;
    this.exitQueueTime = UNDEFINED;
    this.processCompletionTime = UNDEFINED;
  }

  public void setEnterQueueTime() {
    Assert.assertEquals(UNDEFINED, this.enterQueueTime);
    // this.enterQueueTime = System.nanoTime();
    this.enterQueueTime = System.currentTimeMillis();
  }

  public void setExitQueueTime() {
    Assert.assertEquals(UNDEFINED, this.exitQueueTime);
    // this.exitQueueTime = System.nanoTime();
    this.exitQueueTime = System.currentTimeMillis();
  }

  public void setProcessCompletionTime() {
    Assert.assertEquals(UNDEFINED, this.processCompletionTime);
    // this.processCompletionTime = System.nanoTime();
    this.processCompletionTime = System.currentTimeMillis();
  }

  public URL getUrl() {
    return this.url;
  }

  public long getEnterQueueTime() {
    return this.enterQueueTime;
  }

  public long getExitQueueTime() {
    return this.exitQueueTime;
  }

  public long getProcessCompletionTime() {
    return this.processCompletionTime;
  }

  public HttpClient getClient() {
    return this.client;
  }

  public int getAppserverID() {
    return this.appserverID;
  }

  public String toString() {
    return "client=" + this.client + " AppserverID=" + this.appserverID;
  }

  public String printData() {

    return this.enterQueueTime + "," + this.exitQueueTime + "," + this.processCompletionTime + this.appserverID + ","
           + this.client + "," + this.client.getState().getCookies()[0].toString();
  }
}

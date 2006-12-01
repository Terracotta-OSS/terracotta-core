/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.performance.http.load;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;

import com.tctest.performance.http.load.AbstractHttpLoadTest.LoadTestThread;

import java.io.IOException;
import java.io.InputStream;

public abstract class ExpiringWorkItem implements WorkItem {

  private final long endtime;

  protected ExpiringWorkItem(long endtime) {
    this.endtime = endtime;
  }

  public boolean expired(long currenttime) {
    return currenttime >= endtime;
  }

  public final boolean stop() {
    return false;
  }

  protected abstract void executeImpl(HttpClient client, StatsCollector c) throws IOException;

  public final void execute(StatsCollector c) throws IOException {
    HttpClient httpClient = ((LoadTestThread) Thread.currentThread()).getHttpClient();
    executeImpl(httpClient, c);
  }

  protected void consumeResponse(HttpMethod method) throws IOException {
    byte[] buf = new byte[4096];
    int len = buf.length;
    InputStream in = method.getResponseBodyAsStream();
    while (in.read(buf, 0, len) > -1) {
      //
    }
  }

  public void done() {
    // override if desired
  }

}

/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.test.server.appserver.load;

import org.apache.commons.httpclient.HttpClient;

import java.net.URL;

public class ExitRequest implements Request {

  public void setEnterQueueTime() {
    throw new RuntimeException("ExitRequest object has no data!");
  }

  public void setExitQueueTime() {
    throw new RuntimeException("ExitRequest object has no data!");
  }

  public void setProcessCompletionTime() {
    throw new RuntimeException("ExitRequest object has no data!");
  }

  public URL getUrl() {
    throw new RuntimeException("ExitRequest object is not associated with an url!");
  }

  public long getEnterQueueTime() {
    throw new RuntimeException("ExitRequest object has no data!");
  }

  public long getExitQueueTime() {
    throw new RuntimeException("ExitRequest object has no data!");
  }

  public long getProcessCompletionTime() {
    throw new RuntimeException("ExitRequest object has no data!");
  }

  public HttpClient getClient() {
    throw new RuntimeException("ExitRequest object is not associated with a client!");
  }

  public int getAppserverID() {
    throw new RuntimeException("ExitRequest object is not associated with an app server!");
  }

  public String toString() {
    return "ExitRequest";
  }

  public String printData() {
    throw new RuntimeException("ExitRequest object has no data!");
  }
}

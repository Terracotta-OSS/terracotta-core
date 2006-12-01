/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.performance.http.load.webapp;

import com.tctest.performance.http.load.AbstractHttpLoadTest;
import com.tctest.performance.http.load.OneHitWorkItem;
import com.tctest.performance.http.load.WorkItem;

public class SessionCreationTest extends AbstractHttpLoadTest {

  private static final String CREATE_SESSION = "/perftest/WebAppServlet?op=create";

  final String[]              hosts;
  int                         currHost       = 0;

  public SessionCreationTest(String[] args) {
    super(args);
    hosts = testProperties.getHosts();
  }

  protected WorkItem[] generateFinishWorkItems() {
    return new WorkItem[0];
  }

  protected WorkItem[] generateWarmUpWorkItems() {
    // hit each server once...
    final WorkItem[] rv = new WorkItem[hosts.length];
    for (int i = 0; i < rv.length; i++) {
      rv[i] = new OneHitWorkItem(hosts[i], CREATE_SESSION);
    }
    return rv;
  }

  protected WorkItem generateWorkItem(long endtime) {
    return new OneHitWorkItem(getNextHost(), CREATE_SESSION, true, endtime);
  }

  private synchronized String getNextHost() {
    final String rv = hosts[currHost];
    currHost = (currHost + 1) % hosts.length;
    return rv;
  }

  public static void main(String[] args) throws Exception {
    new SessionCreationTest(args).execute();
  }

}

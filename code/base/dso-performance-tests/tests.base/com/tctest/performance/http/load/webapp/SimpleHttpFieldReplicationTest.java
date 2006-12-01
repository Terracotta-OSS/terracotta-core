/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.performance.http.load.webapp;

import org.apache.commons.httpclient.HttpState;

import com.tctest.performance.http.load.HttpClientAdapter;
import com.tctest.performance.http.load.SessionWorkItem;
import com.tctest.performance.http.load.WorkItem;

import java.util.Random;

public final class SimpleHttpFieldReplicationTest extends ValidateHttpFieldReplicationTest {

  private static final int          CHANGES    = 20;
  private static final int          GRAPH_SIZE = 10;

  private final Random              random     = new Random(0);
  private final HttpClientAdapter[] sessions;
  private int                       sessionCounter;

  private SimpleHttpFieldReplicationTest(String[] args) {
    super(args);
    final int sessionsCount = testProperties.getSessionsCount();
    final String[] hosts = testProperties.getHosts();
    int hostCounter = 0;
    sessions = new HttpClientAdapter[sessionsCount];
    for (int i = 0; i < sessionsCount; i++) {
      HttpState state = new HttpState();
      sessions[i] = new HttpClientAdapter(state, hosts[hostCounter]);
      hostCounter = (hostCounter + 1) % hosts.length;
    }

  }

  public static void main(String[] args) throws Exception {
    new SimpleHttpFieldReplicationTest(args).execute();
  }

  protected WorkItem[] generateWarmUpWorkItems() {
    WorkItem[] rv = new WorkItem[testProperties.getSessionsCount()];
    for (int i = 0; i < rv.length; i++) {
      final String urlPart = "/perftest/OrganicObjectGraphServlet?create=" + graphSize() + "&changes=0";
      final WorkItem workItem = makeNewWorkItem(i, urlPart, false, Long.MAX_VALUE);
      rv[i] = workItem;
    }
    return rv;
  }

  protected WorkItem generateWorkItem(long endtime) {
    final String urlPart = "/perftest/OrganicObjectGraphServlet?create=0&changes=" + CHANGES;
    final WorkItem workItem = makeNewWorkItem(sessionCounter, urlPart, true, endtime);
    sessionCounter = (sessionCounter + 1) % testProperties.getSessionsCount();
    return workItem;
  }

  protected WorkItem[] generateFinishWorkItems() {
    WorkItem[] rv = new WorkItem[testProperties.getSessionsCount()];
    for (int i = 0; i < testProperties.getSessionsCount(); i++) {
      final String urlPart = "/perftest/OrganicObjectGraphServlet?finished=true";
      final WorkItem workItem = makeNewWorkItem(i, urlPart, false, Long.MAX_VALUE);
      rv[i] = workItem;
    }
    return rv;
  }

  private int getRandom(int bound) {
    return random.nextInt(bound);
  }

  private WorkItem makeNewWorkItem(int sessionIndex, String urlPart, boolean gatherStats, long endtime) {
    final HttpClientAdapter clientAdapter = sessions[sessionIndex];

    // sticky sessions
    int randomValue = getRandom(100) + 1;
    if (randomValue > testProperties.getStickyRatio()) {
      String prevHost = clientAdapter.getHost();
      String newHost;
      do {
        final String[] hosts = testProperties.getHosts();
        newHost = hosts[getRandom(hosts.length)];
      } while (newHost.equals(prevHost));
      clientAdapter.setHost(newHost);
    }

    WorkItem rv = new SessionWorkItem(clientAdapter, urlPart, gatherStats, endtime);
    return rv;
  }

  protected int changes() {
    return CHANGES;
  }

  protected int graphSize() {
    return GRAPH_SIZE;
  }
}

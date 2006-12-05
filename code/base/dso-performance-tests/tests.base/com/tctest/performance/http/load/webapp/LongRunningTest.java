/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.performance.http.load.webapp;

import org.apache.commons.httpclient.HttpState;

import EDU.oswego.cs.dl.util.concurrent.LinkedQueue;

import com.tctest.performance.http.load.AbstractHttpLoadTest;
import com.tctest.performance.http.load.HttpClientAdapter;
import com.tctest.performance.http.load.SessionWorkItem;
import com.tctest.performance.http.load.WorkItem;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Random;

public class LongRunningTest extends AbstractHttpLoadTest {

  // 1 in this many will be either invalidated or abandoned
  private static final int    END_SESSION    = 50;

  // 1 in this many will be explicitly invalidated (versus simply abandoned)
  private static final int    INVALIDATE     = 4;

  // 1 in this many will add/remove (vs. mutate) session data
  private static final int    ADD_REMOVE     = 20;

  // session idle timeout
  private static final int    IDLE_SECONDS   = 300;

  private static final String SERVLET        = "/perftest/LongRunningTestServlet";
  private static final String CREATE_URL     = SERVLET + "?action=create&idle=" + IDLE_SECONDS;
  private static final String INVALIDATE_URL = SERVLET + "?action=invalidate";
  private static final String ADD_URL        = SERVLET + "?action=add";
  private static final String REMOVE_URL     = SERVLET + "?action=remove";
  private static final String MUTATE_URL     = SERVLET + "?action=mutate";

  private final Random        random         = new Random();
  private final Map           needsCreate    = new IdentityHashMap();
  private final Sessions      sessions       = new Sessions();
  private final int           sessionsCount;

  protected LongRunningTest(String[] args) {
    super(args);

    this.sessionsCount = testProperties.getSessionsCount();
    final String[] hosts = testProperties.getHosts();
    int hostCounter = 0;

    for (int i = 0; i < sessionsCount; i++) {
      HttpClientAdapter adapter = new HttpClientAdapter(new HttpState(), hosts[hostCounter]);
      sessions.put(adapter);
      hostCounter = (hostCounter + 1) % hosts.length;
    }
  }

  protected WorkItem[] generateFinishWorkItems() {
    return new WorkItem[] {};
  }

  protected WorkItem[] generateWarmUpWorkItems() {
    WorkItem[] rv = new WorkItem[sessionsCount];
    for (int i = 0; i < rv.length; i++) {
      final WorkItem workItem = new Work(sessions.take(), CREATE_URL, false, Long.MAX_VALUE);
      rv[i] = workItem;
    }
    return rv;
  }

  protected WorkItem generateWorkItem(long endtime) {
    HttpClientAdapter adapter = sessions.take();
    if (needsCreate.remove(adapter) != null) {
      //
      return new Work(adapter, CREATE_URL, true, endtime);
    }

    final WorkItem rv;
    if (random.nextInt(END_SESSION) == 0) {
      rv = endSession(adapter, endtime);
    } else {
      rv = regularAccess(adapter, endtime);
    }

    return rv;
  }

  private WorkItem regularAccess(HttpClientAdapter adapter, long endtime) {
    int n = random.nextInt(ADD_REMOVE);
    if (n == 0) {
      if (random.nextBoolean()) {
        return new Work(adapter, ADD_URL, true, endtime);
      } else {
        return new Work(adapter, REMOVE_URL, true, endtime);
      }
    } else {
      return new Work(adapter, MUTATE_URL, true, endtime);
    }
  }

  private WorkItem endSession(HttpClientAdapter adapter, long endtime) {
    if (random.nextInt(INVALIDATE) == 0) {
      HttpClientAdapter newAdapter = new HttpClientAdapter(new HttpState(), adapter.getHost());
      needsCreate.put(newAdapter, newAdapter);
      sessions.put(newAdapter);
      return new Work(adapter, INVALIDATE_URL, true, endtime, false);
    }

    HttpClientAdapter newAdapter = new HttpClientAdapter(new HttpState(), adapter.getHost());
    return new Work(newAdapter, CREATE_URL, true, endtime);
  }

  public static void main(String args[]) throws Exception {
    new LongRunningTest(args).execute();
  }

  private class Work extends SessionWorkItem {

    private final HttpClientAdapter clientAdapter;
    private final boolean           returnSession;

    public Work(HttpClientAdapter clientAdapter, String urlPart, boolean gatherStatistic, long expire) {
      this(clientAdapter, urlPart, gatherStatistic, expire, true);
    }

    public Work(HttpClientAdapter clientAdapter, String urlPart, boolean gatherStatistic, long expire,
                boolean returnSession) {
      super(clientAdapter, urlPart, gatherStatistic, expire);
      this.clientAdapter = clientAdapter;
      this.returnSession = returnSession;
    }

    public void done() {
      if (returnSession) {
        sessions.put(clientAdapter);
      }
    }

  }

  private static class Sessions {
    private final LinkedQueue availSessions = new LinkedQueue();

    void put(HttpClientAdapter adapter) {
      try {
        availSessions.put(adapter);
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
    }

    HttpClientAdapter take() {
      try {
        return (HttpClientAdapter) availSessions.take();
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
    }

  }

}

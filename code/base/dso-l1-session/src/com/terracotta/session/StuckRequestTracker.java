/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.terracotta.session;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

public class StuckRequestTracker implements RequestTracker {

  private final Map     requests = Collections.synchronizedMap(new IdentityHashMap());
  private final Monitor monitor;

  StuckRequestTracker(long sleepTime, long stuckThreshold, boolean dump) {
    monitor = new Monitor(sleepTime, stuckThreshold, dump);
  }

  void start() {
    monitor.start();
  }

  public void begin(HttpServletRequest request) {
    getRequestDetail(true).begin(request);
  }

  public void recordSessionId(TerracottaRequest tr) {
    getRequestDetail(false).recordSessionId(tr);
  }

  public boolean end() {
    boolean done = getRequestDetail(false).end();
    if (done) {
      requests.remove(Thread.currentThread());
    }
    return done;
  }

  private RequestDetail getRequestDetail(boolean create) {
    Thread t = Thread.currentThread();
    RequestDetail rd = (RequestDetail) requests.get(t);
    if (rd == null) {
      if (!create) { throw new AssertionError("missing request detail"); }
      rd = new RequestDetail(t);
      requests.put(t, rd);
    }

    return rd;
  }

  private static class RequestDetail implements RequestTracker {
    private final Thread thread;
    private final List   requests = new ArrayList();
    private final long   start    = System.currentTimeMillis();
    private String       sid;
    private int          count;

    RequestDetail(Thread thread) {
      this.thread = thread;
    }

    public synchronized String toString() {
      return "[" + thread.getName() + "], session " + sid + ", request(s) " + requests;
    }

    public synchronized void begin(HttpServletRequest req) {
      count++;

      StringBuffer buf = new StringBuffer(req.getRequestURI());

      String query = req.getQueryString();
      if (query != null && query.length() > 0) {
        buf.append('?').append(query);
      }

      requests.add(buf.toString());
    }

    public synchronized boolean end() {
      count--;
      return count == 0;
    }

    public synchronized void recordSessionId(TerracottaRequest tr) {
      if (sid == null) {
        HttpSession s = tr.getSession(false);
        if (s != null) {
          sid = s.getId();
        }
      }
    }
  }

  private static class ThreadDump {
    private static final String[] CMD;
    private static final boolean  hasKillAll;

    static {
      String killall = findKillAll();

      if (killall == null) {
        hasKillAll = false;
        CMD = null;
      } else {
        hasKillAll = true;
        CMD = new String[] { killall, "-3", "java" };
      }
    }

    private static String findKillAll() {
      String[] variants = new String[] { "/usr/bin/killall", "/usr/sbin/killall", "/sbin/killall", "/bin/killall" };
      for (int i = 0; i < variants.length; i++) {
        String path = variants[i];
        File f = new File(path);
        if (f.exists()) { return path; }
      }
      return null;
    }

    static void dumpThreads() {
      if (hasKillAll) {
        try {
          Process proc = Runtime.getRuntime().exec(CMD);
          proc.getOutputStream().close();
          consume(proc.getInputStream());
          consume(proc.getErrorStream());
          proc.waitFor();
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    }

    private static final byte[] buf = new byte[128];

    private static void consume(InputStream is) throws IOException {
      while (is.read(buf, 0, 128) >= 0) {
        //
      }
    }

  }

  private class Monitor extends Thread {

    private final long    stuckThreshold;
    private final long    sleepTime;
    private final boolean dump;

    Monitor(long sleepTime, long stuckThreshold, boolean dump) {
      this.sleepTime = sleepTime;
      this.stuckThreshold = stuckThreshold;
      this.dump = dump;
      setDaemon(true);
      setName("Session Stuck Thread Monitor");
    }

    public void run() {
      while (true) {
        try {
          sleep(sleepTime);
        } catch (InterruptedException e) {
          continue;
        }

        long now = System.currentTimeMillis();
        Map stuck = new TreeMap();
        Object[] currentRequests = StuckRequestTracker.this.requests.values().toArray();
        for (int i = 0, n = currentRequests.length; i < n; i++) {
          RequestDetail rd = (RequestDetail) currentRequests[i];

          long time = now - rd.start;
          if (time > stuckThreshold) {
            stuck.put(new Long(time), rd);
          }
        }

        if (stuck.size() > 0) {
          StringBuffer message = new StringBuffer("Stuck Threads (").append(stuck.size()).append(")\n");
          Object[] stuckRequests = stuck.entrySet().toArray();
          for (int i = stuckRequests.length - 1; i >= 0; i--) {
            Map.Entry entry = (Entry) stuckRequests[i];
            RequestDetail t = (RequestDetail) entry.getValue();
            long time = ((Long) entry.getKey()).longValue();
            message.append("    ").append(time).append(" ").append(t).append("\n");
          }

          System.err.println(message);
          System.err.flush();
          if (dump) {
            ThreadDump.dumpThreads();
          }
        }
      }
    }
  }

}
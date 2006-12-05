/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.performance.http.load;

import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpConnectionManager;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.params.HttpConnectionManagerParams;
import org.apache.commons.lang.SerializationUtils;

import EDU.oswego.cs.dl.util.concurrent.PooledExecutor;
import EDU.oswego.cs.dl.util.concurrent.ThreadFactory;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.net.InetAddress;
import java.text.DateFormat;
import java.util.Date;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.zip.GZIPOutputStream;

public abstract class AbstractHttpLoadTest {

  private static final String       REPORT      = "report";
  private static final String       RESULTS_DIR = "results";

  private final LinkedBlockingQueue workQ;
  private final HttpLoadClient      loadClient;
  private final int                 duration;
  private final String              workingDir;
  private final boolean             printReport;
  private final RequestCounter      counter;
  protected final TestProperties    testProperties;

  protected AbstractHttpLoadTest(String[] args) {
    validateArgs(args);
    this.printReport = (args.length == 3 && args[2].equals(REPORT));
    this.workQ = new LinkedBlockingQueue(100);
    this.duration = Integer.parseInt(args[0]);
    this.workingDir = args[1];
    this.testProperties = new TestProperties(this.workingDir);
    this.counter = new RequestCounter();
    this.loadClient = (printReport || duration == 0) ? null : makeInstance();
  }

  public HttpLoadClient makeInstance() {
    HttpConnectionManager connMgr = new MultiThreadedHttpConnectionManager();
    HttpConnectionManagerParams params = connMgr.getParams();
    params.setMaxTotalConnections(testProperties.getHosts().length * testProperties.getThreadCount());
    params.setConnectionTimeout(30 * 10000);
    params.setTcpNoDelay(true);
    String[] hosts = testProperties.getHosts();
    for (int i = 0; i < hosts.length; i++) {
      String host = hosts[i];
      HostConfiguration hostConfig = new HostConfiguration();
      String[] parts = host.split(":");
      hostConfig.setHost(parts[0], Integer.valueOf(parts[1]).intValue(), "http");
      params.setMaxConnectionsPerHost(hostConfig, testProperties.getThreadCount());
    }
    connMgr.setParams(params);

    PooledExecutor requestExecutor = new PooledExecutor(testProperties.getThreadCount());
    requestExecutor.setThreadFactory(new LoadTestThreadFactory(connMgr));
    requestExecutor.setKeepAliveTime(1000);
    requestExecutor.waitWhenBlocked();
    return new HttpLoadClient(workQ, requestExecutor, testProperties.getHosts(), testProperties.getSessionsCount(),
                              testProperties.getStickyRatio(), counter);
  }

  protected abstract WorkItem[] generateWarmUpWorkItems();

  protected abstract WorkItem generateWorkItem(long endtime);

  protected abstract WorkItem[] generateFinishWorkItems();

  protected void warmUp() throws InterruptedException {
    WorkItem[] items = generateWarmUpWorkItems();
    System.out.println("WARMING UP " + items.length + " SESSIONS");
    addWorkItems(workQ, items);
    counter.waitForCount(items.length);
  }

  protected void execute() throws Exception {
    if (printReport) {
      HttpResponseAnalysisReport.printReport(resultsDir(), getClass().getSimpleName(), duration);
      return;
    }
    Thread worker = new Thread() {
      public void run() {
        try {
          warmUp();
          DateFormat df = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM);
          String time = df.format(new Date(System.currentTimeMillis()));
          System.out.println("BEGINNING TIMED LOAD: " + time);
          long endTime = System.currentTimeMillis() + (duration * 1000);
          while (endTime > System.currentTimeMillis()) {
            WorkItem item = generateWorkItem(endTime);
            workQ.put(item);
          }
          System.out.println(df.format(new Date(System.currentTimeMillis())));
          finished();
        } catch (InterruptedException e) {
          e.printStackTrace();
          System.exit(0);
        }
      }
    };
    worker.setPriority(Thread.MAX_PRIORITY);
    worker.start();

    final String host = InetAddress.getLocalHost().getHostName();
    final StatsCollector collector = loadClient.getCollector();
    resultsDir().mkdir();

    Thread statsWriter = new Thread() {
      private int count = 0;

      public void run() {
        try {
          run0();
        } catch (Exception e) {
          e.printStackTrace();
        }
      }

      private void run0() throws Exception {
        File outFile = new File(resultsDir(), HttpResponseAnalysisReport.RESULTS_FILE_PREFIX + "." + host);
        FileOutputStream fout = new FileOutputStream(outFile);
        GZIPOutputStream gzOut = new GZIPOutputStream(new BufferedOutputStream(fout));

        while (true) {
          ResponseStatistic stat = collector.takeStat();

          if (stat == null) {
            break;
          }

          gzOut.write(SerializationUtils.serialize(stat));
          count++;
        }

        gzOut.close();

        boolean renamed = outFile.renameTo(new File(outFile.getParent(), outFile.getName() + "." + count + ".gz"));
        if (!renamed) { throw new RuntimeException("rename failed: " + outFile + ", count " + count); }
      }
    };
    statsWriter.start();

    loadClient.execute();

    statsWriter.join();
    worker.join();
  }

  protected void finished() throws InterruptedException {
    System.out.println("COOLING DOWN");
    final WorkItem[] items = generateFinishWorkItems();
    addWorkItems(workQ, items);
    counter.waitForCount(items.length); // XXX: I don't think this does anything, isn't count already way past this
    // value
    workQ.put(new StopWorkItem());
  }

  protected void validateArgs(String[] args) {
    if (args.length < 2) {
      System.out.println("Usage:");
      System.out.println("  <duration in seconds> <working dir path> [report]");
      System.exit(0);
    }
  }

  protected final File resultsDir() {
    return new File(workingDir + File.separator + RESULTS_DIR);
  }

  protected static void addWorkItems(LinkedBlockingQueue q, WorkItem[] items) throws InterruptedException {
    for (int i = 0; i < items.length; i++) {
      q.put(items[i]);
    }
  }

  private static class LoadTestThreadFactory implements ThreadFactory {

    private final HttpConnectionManager connMgr;

    public LoadTestThreadFactory(HttpConnectionManager connMgr) {
      this.connMgr = connMgr;
    }

    public Thread newThread(Runnable run) {
      return new LoadTestThread(run, connMgr);
    }

  }

  static class LoadTestThread extends Thread {

    private final HttpClient httpClient;

    LoadTestThread(Runnable run, HttpConnectionManager connMgr) {
      super(run);
      httpClient = new HttpClient(connMgr);
    }

    HttpClient getHttpClient() {
      return httpClient;
    }

  }

}

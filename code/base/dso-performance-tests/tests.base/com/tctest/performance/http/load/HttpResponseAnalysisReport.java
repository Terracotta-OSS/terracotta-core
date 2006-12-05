/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.performance.http.load;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.URL;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

final class HttpResponseAnalysisReport {

  static final String RESULTS_FILE_PREFIX = "response-statistics";
  static final String PAD                 = " ";

  private HttpResponseAnalysisReport() {
    // cannot instantiate
  }

  private static class StatsIterator implements Iterator {

    private final File[]    files;
    private final Pattern   pattern = Pattern.compile("^" + RESULTS_FILE_PREFIX + "\\.(.+)\\.(\\d+)\\.gz$");
    private final int[]     counts;
    private int             index   = 0;
    private GZIPInputStream in;

    public StatsIterator(File resultsDir) {
      files = resultsDir.listFiles(new FileFilter() {
        public boolean accept(File pathname) {
          return pattern.matcher(pathname.getName()).matches();
        }
      });

      counts = new int[files.length];

      for (int i = 0; i < files.length; i++) {
        File file = files[i];
        Matcher m = pattern.matcher(file.getName());
        if (!m.matches()) { throw new RuntimeException(file + " doesn't match"); }
        int count = Integer.parseInt(m.group(2));
        System.err.println("Going to read " + count + " stats from host " + m.group(1));
        counts[i] = count;
      }
    }

    public boolean hasNext() {
      for (int i = 0; i < counts.length; i++) {
        if (counts[i] > 0) { return true; }
      }
      return false;
    }

    public Object next() {
      try {
        return next0();
      } catch (Exception e) {
        if (e instanceof RuntimeException) { throw (RuntimeException) e; }
        throw new RuntimeException(e);
      }
    }

    private Object next0() throws Exception {
      if (index >= counts.length) { throw new IllegalStateException("no more data: index = " + index); }

      if (in == null) {
        in = inputFor(files[index]);
      }

      if (counts[index] == 0) {
        index++;
        in.close();
        in = inputFor(files[index]);
      }

      counts[index]--;
      return new ObjectInputStream(in).readObject();
    }

    public void remove() {
      throw new UnsupportedOperationException();
    }

    public int numClients() {
      return files.length;
    }

    private static GZIPInputStream inputFor(File file) throws FileNotFoundException, IOException {
      return new GZIPInputStream(new BufferedInputStream(new FileInputStream(file)));
    }
  }

  public static void printReport(File resultsDir, String testName, int duration) throws IOException {
    StatsIterator statsIterator = new StatsIterator(resultsDir);

    Map hostGroup = new TreeMap();
    Map fullUrlGroup = new TreeMap();
    List fullUrlList;
    Map urlGroup;
    List urlList;
    URL url;
    String hostKey;
    String urlKey;

    long minStart = Long.MAX_VALUE;
    long maxEnd = -1;

    while (statsIterator.hasNext()) {
      ResponseStatistic stat = (ResponseStatistic) statsIterator.next();
      minStart = Math.min(stat.startTime(), minStart);
      maxEnd = Math.max(stat.endTime(), maxEnd);

      // split url parts
      url = new URL(stat.url());
      hostKey = url.getHost() + ":" + url.getPort();
      urlKey = url.getPath() + url.getQuery();

      // populate full url group
      if (!fullUrlGroup.containsKey(urlKey)) {
        fullUrlList = new ArrayList();
        fullUrlGroup.put(urlKey, fullUrlList);
      } else {
        fullUrlList = (List) fullUrlGroup.get(urlKey);
      }

      if (!hostGroup.containsKey(hostKey)) { // group hosts
        urlGroup = new HashMap(); // create new url group
        urlList = new ArrayList(); // create new url list
        urlGroup.put(urlKey, urlList);
        hostGroup.put(hostKey, urlGroup);

      } else {
        urlGroup = (Map) hostGroup.get(hostKey);
        if (!urlGroup.containsKey(urlKey)) { // group urls in host group
          urlList = new ArrayList();
          urlGroup.put(urlKey, urlList);
        } else {
          urlList = (List) urlGroup.get(urlKey);
        }
      }
      fullUrlList.add(stat); // add stats to full url list
      urlList.add(stat); // add stats to host/url list
    }

    int realDuration = (int) ((maxEnd - minStart) / 1000);

    printHeader(resultsDir, testName, realDuration, statsIterator.numClients());
    out("Throughput Analyze:");
    out(repeat('_', "Throughput Analyze:".length()));

    Iterator iter = hostGroup.entrySet().iterator();
    while (iter.hasNext()) {
      Map.Entry entry = (Map.Entry) iter.next();
      Map value = (Map) entry.getValue();
      String key = (String) entry.getKey();
      throughputAnalysis(key, value, realDuration);
    }
    nl();
    nl();
    out("Response Analyze:");
    out(repeat('_', "Response Analyze:".length()));
    responseAnalysis(fullUrlGroup);
  }

  private static void throughputAnalysis(String key, Map value, long duration) {
    int colWidth = 16;
    nl();
    write("(" + key + ")", (42 + key.length()) - ((key.length() + 2) / 2));
    nl();
    nl();
    write("Ave.", colWidth);
    write("tps", colWidth);
    write("total", colWidth);
    write("success", colWidth);
    write("error", colWidth);
    nl();
    out(repeat('-', 80));

    double totalAve = 0;
    double totalTps = 0;
    int totalTotal = 0;
    int totalSuccess = 0;
    int totalError = 0;

    String urlKey;
    List values;
    Map.Entry entry;
    Iterator iter = value.entrySet().iterator();
    while (iter.hasNext()) {

      double ave = 0;
      double tps = 0;
      int success = 0;
      int error = 0;
      double size = 0;
      long sum = 0;

      entry = (Map.Entry) iter.next();
      values = (List) entry.getValue();
      urlKey = (String) entry.getKey();
      out(urlKey);
      size = values.size();

      for (int i = 0; i < size; i++) {
        ResponseStatistic stat = (ResponseStatistic) values.get(i);
        sum += stat.duration();
        if (stat.statusCode() == 200) success++;
        else error++;
      }
      ave = sum / size;
      totalAve += ave;
      tps = size / duration;
      totalTps += tps;
      totalTotal += size;
      totalSuccess += success;
      totalError += error;
      writeNum(Math.floor(ave), colWidth);
      writeNum(tps, colWidth);
      writeNum(size, colWidth);
      writeNum(success, colWidth);
      writeNum(error, colWidth);
      nl();
    }
    out(repeat('=', 80));
    write("total", 5);
    writeNum(Math.floor(totalAve), 11);
    writeNum(totalTps, colWidth);
    writeNum(totalTotal, colWidth);
    writeNum(totalSuccess, colWidth);
    writeNum(totalError, colWidth);
    nl();
    nl();
  }

  private static void responseAnalysis(Map urlGroup) {
    int colWidth = 9;
    nl();
    write("Ave.", 8);
    write("S.D.", colWidth);
    write("min.", colWidth);
    write("50%", colWidth);
    write("60%", colWidth);
    write("70%", colWidth);
    write("80%", colWidth);
    write("90%", colWidth);
    write("max.", colWidth);
    nl();
    out(repeat('-', 80));

    String urlKey;
    List values;
    Map.Entry entry;
    Iterator iter = urlGroup.entrySet().iterator();
    while (iter.hasNext()) {

      double ave = 0;
      double size = 0;
      double squareSum = 0;
      long sum = 0;
      long duration;

      entry = (Map.Entry) iter.next();
      values = (List) entry.getValue();
      urlKey = (String) entry.getKey();
      out(urlKey);
      size = values.size();
      long[] durationSpread = new long[values.size()];

      for (int i = 0; i < size; i++) {
        ResponseStatistic stat = (ResponseStatistic) values.get(i);
        duration = stat.duration();
        sum += duration;
        squareSum += duration * duration;
        durationSpread[i] = duration;
      }

      Arrays.sort(durationSpread);
      ave = sum / size;
      writeNum(Math.floor(ave), 8);
      writeNum(Math.floor(Math.sqrt((squareSum / size) - (ave * ave))), colWidth);
      writeNum(durationSpread[0], colWidth);
      writeNum(durationSpread[new Float(durationSpread.length * .5f).intValue() - 1], colWidth);
      writeNum(durationSpread[new Float(durationSpread.length * .6f).intValue() - 1], colWidth);
      writeNum(durationSpread[new Float(durationSpread.length * .7f).intValue() - 1], colWidth);
      writeNum(durationSpread[new Float(durationSpread.length * .8f).intValue() - 1], colWidth);
      writeNum(durationSpread[new Float(durationSpread.length * .9f).intValue() - 1], colWidth);
      writeNum(durationSpread[durationSpread.length - 1], colWidth);
      nl();
    }
  }

  private static void printHeader(File resultsDir, String testName, int realDuration, int loadClients) {
    TestProperties props = new TestProperties(resultsDir.getParentFile());
    String threads = "" + props.getThreadCount();
    String[] hosts = props.getHosts();
    String stickyRatio = "" + props.getStickyRatio();

    out("HTTP RESPONSE ANALYSIS REPORT -- " + testName);
    nl();
    write("THREADS:", 74);
    out("" + (Integer.parseInt(threads) * loadClients), 6);
    write("HOSTS:", 74);
    out(Integer.toString(hosts.length), 6);
    write("STICKY-RATIO:", 74);
    out(stickyRatio, 6);
    write("DURATION (real):", 74);
    out("" + realDuration, 6);
    write("LOAD CLIENTS:", 74);
    out("" + loadClients, 6);
  }

  private static void write(String str, int width) {
    if (str.length() > width) str = str.substring(0, width);
    System.out.print(pad(width - str.length()) + str);
  }

  private static void writeNum(double num, int width) {
    NumberFormat nf = NumberFormat.getInstance();
    nf.setMaximumFractionDigits(2);
    String str = nf.format(num);
    System.out.print(pad(width - str.length()) + str);
  }

  private static void out(String str) {
    System.out.println(str);
  }

  private static void out(String str, int width) {
    System.out.println(pad(width - str.length()) + str);
  }

  private static String pad(int width) {
    String pad = "";
    for (int i = 0; i < width; i++) {
      pad += PAD;
    }
    return pad;
  }

  private static String repeat(char chr, int width) {
    String str = "";
    for (int i = 0; i < width; i++) {
      str += chr;
    }
    return str;
  }

  private static void nl() {
    System.out.print("\n");
  }
}

/**
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.util;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.zip.GZIPInputStream;

public class CruiseControlLogParser {

  private static final String MARKER    = "...]]></message>";

  private static final String TAG       = "        <message priority=\"info\"><![CDATA[";
  private static final String CLOSE_TAG = "]]></message>";

  public static void main(String[] args) throws Exception {
    if (args.length != 2) {
      usage();
      System.exit(1);
    }

    String urlStr = args[0];
    String testName = args[1];

    String failTestEnd = TAG + "ERROR    Test " + testName + " FAILED" + CLOSE_TAG;

    int linenum = 0;
    boolean inTest = false;
    URL url = new URL(urlStr);
    URLConnection connect = url.openConnection();
    String encoding = connect.getContentEncoding();
    InputStream response = connect.getInputStream();
    if ("x-gzip".equals(encoding) || urlStr.toLowerCase().endsWith(".gz")) {
      response = new GZIPInputStream(response);
    }

    try {
      BufferedReader reader = new BufferedReader(new InputStreamReader(response));
      String line;

      while ((line = reader.readLine()) != null) {
        linenum++;
        if (inTest) {
          if (line.endsWith("Test" + MARKER)) break; // start of another test

          if (line.startsWith(TAG)) {
            output(line);
            if (line.startsWith(failTestEnd)) {
              break;
            }
          }
        } else if (line.endsWith(testName + MARKER)) {
          output(line);
          inTest = true;
        }
      }
    } catch (Exception e) {
      System.err.println("***************** PARSER ERROR (line: " + linenum + ") *****************");
      e.printStackTrace();
    }
  }

  private static void output(String line) {
    boolean err = (line.indexOf("ERROR") != -1 || line.indexOf("WARN") != -1); 
    line = line.substring(TAG.length(), line.length() - CLOSE_TAG.length());
    line = line.replaceAll("^WARN(     )?", "");
    line = line.replaceAll("^INFO(     )?", "");
    line = line.replaceAll("^ERROR(    )?", "");
    line = line.replaceAll("&lt;", "<");
    line = line.replaceAll("&gt;", ">");
    if (err) System.err.println(line);
    else System.out.println(line);
  }

  private static void usage() {
    System.err.println("usage: " + CruiseControlLogParser.class.getName() + " <log URL> <test-name>");
    System.err.println();
    System.err.println("   example args: http://sand/long-strange-path-to-monkeylog.xml TwelveByteIntegerTest");
  }
}
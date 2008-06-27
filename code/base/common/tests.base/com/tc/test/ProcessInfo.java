/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.test;

import com.tc.util.runtime.Os;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class ProcessInfo {
  public static String ps_grep_java() {
    String[] args = new String[] { "sh", "-c", "ps auxwww | grep java | grep -v grep" };
    String execPath = TestConfigObject.getInstance().executableSearchPath();
    if (execPath == null) return "";
    try {
      if (Os.isWindows()) {
        args = new String[] { execPath + "\\pv.exe", "-l", "java.exe" };
      } else if (Os.isSolaris()) {
        args = new String[] { "sh", "-c", "/usr/ucb/ps auxwww | grep java | grep -v grep" };
      }

      Process p = Runtime.getRuntime().exec(args);
      StreamGobbler out = new StreamGobbler(p.getInputStream());
      StreamGobbler err = new StreamGobbler(p.getErrorStream());

      out.start();
      err.start();

      p.waitFor();
      String result = out.getOutput().trim() + "\n" + err.getOutput();

      return result.trim();

    } catch (Exception e) {
      e.printStackTrace();
    }
    return "";
  }
}

class StreamGobbler extends Thread {
  private final InputStream  is;
  private final StringBuffer output = new StringBuffer(1024);

  StreamGobbler(InputStream is) {
    this.is = is;
  }

  public void run() {
    try {
      InputStreamReader isr = new InputStreamReader(is);
      BufferedReader br = new BufferedReader(isr);
      String line = null;
      while ((line = br.readLine()) != null) {
        output.append(line).append("\n");
      }
    } catch (IOException ioe) {
      ioe.printStackTrace();
    }
  }

  public String getOutput() {
    return output.toString();
  }
}

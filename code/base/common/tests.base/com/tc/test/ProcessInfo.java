/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
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
    try {
      String[] args = new String[] { "sh", "-c", "ps auxwww | grep java | grep -v grep" };

      if (Os.isWindows()) {
        String execPath = TestConfigObject.getInstance().executableSearchPath();
        args = new String[] { execPath + "\\pv.exe", "-l", "java.exe" };
      } else if (Os.isSolaris()) {
        args = new String[] { "sh", "-c", "/usr/ucb/ps auxwww | grep java | grep -v grep" };
      }

      Process p = Runtime.getRuntime().exec(args);
      StreamGobbler out = new StreamGobbler(p.getInputStream(), "stdout");
      StreamGobbler err = new StreamGobbler(p.getErrorStream(), "stderr");
      
      out.start();
      err.start();
      
      p.waitFor();
      String result = out.getOutput().trim() + "\n" + err.getOutput();
      
      return result.trim();

    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}

class StreamGobbler extends Thread {
  private InputStream  is;
  private String       type;
  private StringBuffer output = new StringBuffer(1024);

  StreamGobbler(InputStream is, String type) {
    this.is = is;
    this.type = type;
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

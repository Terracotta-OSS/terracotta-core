/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.test;

import com.tc.util.runtime.Os;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class ProcessInfo {
  public static String ps_grep_java() {
    try {
      String cmd = "ps auxwww | grep java | grep -v grep";
      if (Os.isWindows()) {
        String execPath = TestConfigObject.getInstance().executableSearchPath();
        cmd = execPath + "\\pv.exe -l java.exe";
      } else if (Os.isSolaris()) {
        cmd = "/usr/ucb/ps auxwww | grep java | grep -v grep";
      }
      
      System.out.println("cmd: " + cmd);
      Process p = Runtime.getRuntime().exec(cmd);
      BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
      String line;
      StringBuffer buffer = new StringBuffer();
      while ((line = reader.readLine()) != null) {
        buffer.append(line + "\n");
      }
      reader.close();
      
      System.out.println("Executing: " + cmd + " -- Exit code: " + p.exitValue());
      return buffer.toString();
      
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}

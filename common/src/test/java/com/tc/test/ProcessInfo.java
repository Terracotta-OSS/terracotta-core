/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.test;

import com.tc.process.Exec;
import com.tc.util.runtime.Os;

import java.io.File;

public class ProcessInfo {
  public static String ps_grep_java() {
    File jps = getProgram("jps");
    if (jps.isFile()) {
      try {
        Exec.Result result = Exec.execute(new String[] {jps.getAbsolutePath(), "-mlv"});

        return result.getStdout().trim() + "\n" + result.getStderr().trim();
      } catch (Exception e) {
        e.printStackTrace();
        return "jps failure";
      }
    } else {
      return "jps not found";
    }
  }

  private static File getProgram(String prog) {
    File javaHome = new File(System.getProperty("java.home"));
    if (javaHome.getName().equals("jre")) {
      javaHome = javaHome.getParentFile();
    }

    if (Os.isWindows()) {
      return new File(new File(javaHome, "bin"), prog + ".exe");
    } else {
      return new File(new File(javaHome, "bin"), prog);
    }
  }
}
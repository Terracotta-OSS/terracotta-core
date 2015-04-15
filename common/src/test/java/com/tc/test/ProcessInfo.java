/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
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
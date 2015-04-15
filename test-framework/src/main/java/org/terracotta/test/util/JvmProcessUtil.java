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
package org.terracotta.test.util;

import org.junit.Assert;

import com.tc.util.runtime.Os;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class JvmProcessUtil {

  public static enum Signal {
    SIGSTOP, SIGCONT
  }

  public static int[] getJavaPIDS(String mainClassName) throws IOException, InterruptedException {
    int[] l2serverPids = new int[2];
    int count = 0;
    String cutCommand = "";
    if (Os.isLinux()) {
      cutCommand = "cut -d' ' -f2";
    }
    if (Os.isMac()) {
      cutCommand = "cut -d' ' -f3";
    }
    Assert.assertFalse(cutCommand.isEmpty());
    String[] commands = getCommands(mainClassName, cutCommand);
    Process process = Runtime.getRuntime().exec(commands);
    process.waitFor();
    BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
    String line = reader.readLine();
    while (line != null && !line.isEmpty()) {
      if (!line.trim().isEmpty()) {
        // debug("server pid " + line);
        l2serverPids[count++] = Integer.parseInt(line);
        line = reader.readLine();
      }
    }
    Assert.assertFalse("pid1 " + l2serverPids[0] + " pid2 " + l2serverPids[1], l2serverPids[0] == l2serverPids[1]);
    return l2serverPids;
  }

  private static String[] getCommands(String mainClassName, String cutCommand) {
    if (isNotBlank(mainClassName)) {
      String[] commands = { "/bin/sh", "-c",
          "ps -ef | grep -v 'grep' | grep java | grep " + mainClassName + " | " + cutCommand };
      return commands;
    }
    String[] commands = { "/bin/sh", "-c", "ps -ef | grep -v 'grep' | grep java |" + cutCommand };
    return commands;
  }

  private static boolean isNotBlank(String mainClassName) {
    if (mainClassName != null && !mainClassName.trim().equals("")) { return true; }
    return false;
  }

  public static void sendSignal(Signal signal, int pid) throws InterruptedException, IOException {
    int status = 0;
    switch (signal) {
      case SIGCONT:
        status = Runtime.getRuntime().exec("kill -SIGCONT " + pid).waitFor();
        LogUtil.debug(JvmProcessUtil.class, "PID :" + pid + " Status of Signal SIGCONT:  " + status);
        break;
      case SIGSTOP:
        status = Runtime.getRuntime().exec("kill -SIGSTOP " + pid).waitFor();
        LogUtil.debug(JvmProcessUtil.class, "PID :" + pid + "Status of Signal SIGSTOP : " + status);
        break;
    }
  }

  public static void main(String[] args) throws IOException, InterruptedException {
    int pids[] = JvmProcessUtil.getJavaPIDS("Hang");
    if (pids != null) {
      for (int pid : pids) {
        if (pid != 0) {
          System.out.println(pid);
          sendSignal(Signal.SIGSTOP, pid);
          System.out.println("Send Signal");
        }
      }
    }
  }

}

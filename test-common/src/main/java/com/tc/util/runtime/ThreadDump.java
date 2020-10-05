/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Terracotta Core.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
 */
package com.tc.util.runtime;

import com.tc.process.Exec;
import com.tc.process.Exec.Result;
import com.tc.util.Banner;
import com.tc.util.ThreadUtil;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.lang.management.ManagementFactory;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ThreadDump {

  private static final long TIMEOUT = 30000L;

  public static void main(String args[]) {
    dumpThreadsOnce();

    // This flush()'ing and sleeping is a (perhaps poor) attempt at making ThreadDumpTest pass on Jrockit
    System.err.flush();
    System.out.flush();
    try {
      Thread.sleep(5000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

  public static void dumpThreadsOnce() {
    dumpThreadsMany(1, 0L);
  }

  public static int dumpThreadsMany(int iterations, long delay) {
    PID pid = getPID();
    dumpThreadsMany(iterations, delay, Collections.singleton(pid));
    return pid.getPid();
  }

  static PID getPID() {
    String vmName = ManagementFactory.getRuntimeMXBean().getName();
    int index = vmName.indexOf('@');

    if (index < 0) { throw new RuntimeException("unexpected format: " + vmName); }

    return new PID(Integer.parseInt(vmName.substring(0, index)), "not available");
  }

  private static void dumpThreadsMany(int iterations, long delay, Set<PID> pids) {
    echoProcesses(pids);

    boolean multiple = pids.size() > 1;

    for (int i = 0; i < iterations; i++) {
      for (PID pid : pids) {
        System.err.println("Requesting dump for PID " + pid.getPid());
        doDump(pid);
        if (multiple) {
          // delay a bit to help prevent overlapped output
          ThreadUtil.reallySleep(50);
        }
      }
      ThreadUtil.reallySleep(delay);
    }
  }

  private static void echoProcesses(Set<PID> pids) {
    StringBuilder sb = new StringBuilder("Thread dumping these processes:\n");
    for (PID pid : pids) {
      sb.append("  ");
      sb.append(pid.getPid());
      sb.append("\t");
      sb.append(pid.getCmdLine());
      sb.append("\n");
    }

    System.err.println(sb.toString());

  }

  public static void dumpAllJavaProcesses() {
    dumpAllJavaProcesses(1, 0L);
  }

  public static void dumpAllJavaProcesses(int iterations, long delay) {
    dumpThreadsMany(iterations, delay, findAllJavaPIDs());
  }

  private static void doDump(PID pid) {
    doJstack(pid);
  }

//  private static void doJrcmd(PID pid) {
//    File jrcmd = getProgram("jrcmd");
//    if (jrcmd.isFile()) {
//      try {
//        Result result = Exec.execute(new String[] { jrcmd.getAbsolutePath(), String.valueOf(pid.getPid()),
//            "print_threads" }, TIMEOUT);
//        System.err.println(result.getStdout() + result.getStderr());
//      } catch (Exception e) {
//        e.printStackTrace();
//      }
//    } else {
//      Banner.warnBanner("jrcmd not found");
//    }
//  }

  private static void doJstack(PID pid) {
    File jstack = getProgram("jstack");
    if (jstack.isFile()) {
      boolean success = false;
      int count = 0;
      String absolutePath = jstack.getAbsolutePath();
      String valueOfPid = String.valueOf(pid.getPid());
      do {
        doJps();
        try {
          String[] cmd = null;
          if (count > 5) {
            cmd = new String[] { absolutePath, "-F", "-l", valueOfPid };
          } else {
            cmd = new String[] { absolutePath, "-l", valueOfPid };
          }
          Result result = Exec.execute(cmd, TIMEOUT);
          String output = result.getStdout() + result.getStderr();
          System.err.println(output);
          success = !(output.contains("Connection refused") || output.contains("Error attaching to process"));
        } catch (Exception e) {
          e.printStackTrace();
        }
        ++count;
        System.err.println("tried jstack count " + count + " success " + success);
        ThreadUtil.reallySleep(TimeUnit.SECONDS.toMillis(1L));
      } while (!success && count < 10);

    } else {
      Banner.warnBanner("jstack not found");
    }
  }

  private static void doJps() {
    File jps = getProgram("jps");
    if (jps.isFile()) {
      try {
        Result result = Exec.execute(new String[] { jps.getAbsolutePath(), "-q" }, TIMEOUT);
        System.err.println(result.getStdout() + result.getStderr());
      } catch (Exception e) {
        e.printStackTrace();
      }
    } else {
      Banner.warnBanner("jps not found in " + jps.getPath());
    }
  }

  private static File getProgram(String prog) {
    File javaHome = new File(System.getProperty("java.home"));
    if (javaHome.getName().equals("jre")) {
      javaHome = javaHome.getParentFile();
    }

    if (System.getProperty("os.name").contains("win")) {
      return new File(new File(javaHome, "bin"), prog + ".exe");
    } else {
      return new File(new File(javaHome, "bin"), prog);
    }
  }

  static Set<PID> findAllJavaPIDs() {
    Set<PID> pids = new HashSet<PID>();

    File jpsCmd = getProgram("jps");
    if (!jpsCmd.isFile()) {
      Banner.warnBanner("jps not found");
      return Collections.emptySet();
    }

    Result result;
    try {
      result = Exec.execute(new String[] { jpsCmd.getAbsolutePath(), "-lmv" }, TIMEOUT);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

    Pattern pattern = Pattern.compile("^(\\d+)\\s+(.*)$");
    String stdout = result.getStdout();
    BufferedReader reader = new BufferedReader(new StringReader(stdout));
    try {
      String line;
      while ((line = reader.readLine()) != null) {
        line = line.trim();

        Matcher matcher = pattern.matcher(line);

        if (!matcher.matches()) {
          System.err.println("\nNON-MATCH: [" + line + "]\n");
          continue;
        }

        String pid = matcher.group(1);
        String cmd = matcher.group(2);

        if (!skip(cmd)) {
          if (!pids.add(new PID(Integer.parseInt(pid), line))) {
            Banner.warnBanner("Found duplicate PID? " + line);
          }
        }
      }
    } catch (IOException e) {
      throw new RuntimeException(result.toString(), e);
    }

    return pids;
  }

  private static boolean skip(String cmd) {
    // try to filter out VMs we don't want to thread dump
    return cmd.contains("-jar slave.jar") || cmd.contains("hudson.maven.agent.Main")
           || cmd.contains("cruisecontrol-launcher.jar ")
           || (cmd.contains(" org.jruby.Main ") && cmd.contains("build-tc.rb"))
           || (cmd.contains(" org.codehaus.classworlds.Launcher ") && cmd.contains("-Dmaven.home="))
           || (cmd.contains(" org.codehaus.plexus.classworlds.launcher.Launcher ") && cmd.contains("-Dmaven.home="));
  }

  static class PID {
    private final int    pid;
    private final String cmdLine;

    PID(int pid, String cmdLine) {
      this.pid = pid;
      this.cmdLine = cmdLine;
    }

    String getCmdLine() {
      return cmdLine;
    }

    int getPid() {
      return pid;
    }

    @Override
    public String toString() {
      return String.valueOf(pid) + " [" + cmdLine + "]";
    }

    @Override
    public boolean equals(Object obj) {
      if (!(obj instanceof PID)) { return false; }
      return ((PID) obj).pid == this.pid;
    }

    @Override
    public int hashCode() {
      return this.pid;
    }
  }
}

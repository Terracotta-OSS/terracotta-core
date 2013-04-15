/**
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.util.runtime;

import com.tc.process.Exec;
import com.tc.process.Exec.Result;
import com.tc.text.Banner;
import com.tc.util.concurrent.ThreadUtil;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.lang.management.ManagementFactory;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
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
    if (Vm.isJRockit()) {
      doJrcmd(pid);
    } else {
      doJstack(pid);
    }
  }

  private static void doJrcmd(PID pid) {
    File jrcmd = getProgram("jrcmd");
    if (jrcmd.isFile()) {
      try {
        Result result = Exec.execute(new String[] { jrcmd.getAbsolutePath(), String.valueOf(pid.getPid()), "print_threads" }, TIMEOUT);
        System.err.println(result.getStdout() + result.getStderr());
      } catch (Exception e) {
        e.printStackTrace();
      }
    } else {
      Banner.warnBanner("jrcmd not found");
    }
  }

  private static void doJstack(PID pid) {
    File jstack = getProgram("jstack");
    if (jstack.isFile()) {
      try {
        Result result = Exec.execute(new String[] { jstack.getAbsolutePath(), "-l", String.valueOf(pid.getPid()) }, TIMEOUT);
        System.err.println(result.getStdout() + result.getStderr());
      } catch (Exception e) {
        e.printStackTrace();
      }
    } else {
      Banner.warnBanner("jstack not found");
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

  static Set<PID> findAllJavaPIDs() {
    Set<PID> pids = new HashSet<PID>();

    File jpsCmd = getProgram("jps");
    if (!jpsCmd.isFile()) {
      Banner.warnBanner("jps not found");
      return Collections.emptySet();
    }
    
    Result result;
    try {
      result = Exec.execute(new String[] { jpsCmd.getAbsolutePath(), "-lmv"}, TIMEOUT);
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

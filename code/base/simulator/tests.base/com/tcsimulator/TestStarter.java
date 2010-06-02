/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tcsimulator;

import com.tc.lcp.LinkedJavaProcess;
import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOApplicationConfig;
import com.tc.simulator.distrunner.ArgParser;
import com.tcsimulator.distrunner.ServerSpec;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

public class TestStarter {

  private TestSpec      testSpec;
  private final boolean debug = false;

  public TestStarter() {
    testSpec = new TestSpec();
  }

  public static void main(String[] args) throws IOException, InterruptedException {
    TestStarter runner = new TestStarter();
    runner.execute();
  }

  public static void visitDSOApplicationConfig(ConfigVisitor visitor, DSOApplicationConfig cfg) {
    String classname = TestStarter.class.getName();
    cfg.addRoot("testSpec", classname + ".testSpec");
    cfg.addIncludePattern(classname);
    cfg.addWriteAutolock("* " + classname + ".*(..)");
  }

  private void println(String msg) {
    System.out.println("TestStarter: " + msg);
  }

  private void execute() throws IOException, InterruptedException {
    InetAddress host = InetAddress.getLocalHost();
    String hostName = host.getHostName();

    Collection procList = new ArrayList();

    int debugPortOffset = 0;

    synchronized (testSpec) {

      Collection containerSpecs = testSpec.getContainerSpecsFor(hostName);

      if (containerSpecs.size() == 0) {
        System.err.println("No containers setup for this host: " + hostName + ", " + testSpec);
        return;
      }

      for (Iterator i = containerSpecs.iterator(); i.hasNext();) {
        ContainerSpec containerSpec = (ContainerSpec) i.next();
        println("Found containerSpec: " + containerSpec);
        LinkedJavaProcess proc = new LinkedJavaProcess(ContainerBuilder.class.getName(), Arrays.asList(containerSpec
            .getVmName()));
        String fileSeparator = System.getProperty("file.separator");

        File terracottaDist = new File(new File(containerSpec.getTestHome()).getAbsoluteFile(), "terracotta");
        File dsoJava = new File(terracottaDist.getAbsolutePath() + fileSeparator + "bin" + fileSeparator
                                + "dso-java.sh");

        List<String> jvmArgs = new ArrayList<String>();
        for (Iterator iter = containerSpec.getJvmOpts().iterator(); iter.hasNext();) {
          String next = (String) iter.next();
          if (!next.equals(ArgParser.getUndefinedString())) {
            jvmArgs.add(next);
          }
        }
        ServerSpec sSpec = (ServerSpec) testSpec.getServerSpecsFor(ServerSpec.TEST_SERVER).iterator().next();
        jvmArgs.add("-Dtc.config=" + sSpec.getHostName() + ":" + sSpec.getDsoPort());
        jvmArgs.add("-Dtc.classloader.writeToDisk=true");
        if (debug) {
          jvmArgs.add("-Xdebug");
          jvmArgs.add("-Xrunjdwp:transport=dt_socket,address=" + (8000 + debugPortOffset) + ",server=y,suspend=y");
          debugPortOffset++;
        }
        List<String> environment = new ArrayList<String>();
        if ("Mac OS X".equals(System.getProperty("os.name")) || "Linux".equals(System.getProperty("os.name"))) {
          environment.add("TC_JAVA_HOME=" + System.getProperty("java.home"));
        }
        proc.setJavaExecutable(dsoJava);
        File dir = new File(containerSpec.getTestHome(), containerSpec.getVmName());
        dir.mkdir();
        proc.setDirectory(dir);
        proc.addAllJvmArgs(jvmArgs);
        proc.setEnvironment(environment);
        proc.start();
        proc.mergeSTDERR();
        proc.mergeSTDOUT();
        procList.add(proc);

      }

    }

    for (Iterator i = procList.iterator(); i.hasNext();) {
      ((LinkedJavaProcess) i.next()).waitFor();
    }

  }
}
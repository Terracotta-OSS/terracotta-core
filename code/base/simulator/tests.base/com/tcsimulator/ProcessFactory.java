/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tcsimulator;

import com.tc.lcp.LinkedJavaProcess;
import com.tcsimulator.distrunner.ServerSpec;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public final class ProcessFactory {

  private final Sandbox    sandbox;
  private final ServerSpec serverSpec;

  public ProcessFactory(Sandbox sandbox, ServerSpec serverSpec) {
    this.sandbox = sandbox;
    this.serverSpec = serverSpec;
  }

  public LinkedJavaProcess newDSOJavaProcessInstance(String className, List<String> args, boolean debug) {
    LinkedJavaProcess newProcess = new LinkedJavaProcess(className, args);
    String fileSeparator = System.getProperty("file.separator");
    File terracottaDist = new File(sandbox.getTestHome().getAbsoluteFile(), sandbox.getDistributionName());
    File dsoJava = new File(terracottaDist.getAbsolutePath() + fileSeparator + "bin" + fileSeparator + "dso-java.sh");

    List<String> jvmArgs = new ArrayList<String>();
    jvmArgs.add("-Dtc.config=" + serverSpec.getHostName() + ":" + serverSpec.getDsoPort());
    // jvmArgs.add("-Dtc.classloader.writeToDisk=true");
    if (debug) {
      jvmArgs.add("-Xdebug");
      jvmArgs.add("-Xrunjdwp:transport=dt_socket,address=8000,server=y,suspend=y");
    }

    List<String> environment = new ArrayList<String>();
    if ("Mac OS X".equals(System.getProperty("os.name")) || "Linux".equals(System.getProperty("os.name"))) {
      environment.add("TC_JAVA_HOME=" + System.getProperty("java.home"));
    }
    newProcess.setJavaExecutable(dsoJava);
    File dir = new File(sandbox.getServerHome(), className);
    dir.mkdir();
    newProcess.setDirectory(dir);
    newProcess.addAllJvmArgs(jvmArgs);
    newProcess.setEnvironment(environment);

    return newProcess;
  }

  public LinkedJavaProcess newJavaProcessInstance(String className, List<String> args, boolean debug, String javaHome) {
    LinkedJavaProcess newProcess = new LinkedJavaProcess(className, args);
    File java = new File(javaHome);

    List<String> jvmArgs = new ArrayList<String>();
    if (debug) {
      jvmArgs.add("-Xdebug");
      jvmArgs.add("-Xrunjdwp:transport=dt_socket,address=8000,server=y,suspend=y");
    }

    newProcess.setJavaExecutable(java);
    newProcess.addAllJvmArgs(jvmArgs);

    return newProcess;
  }

}

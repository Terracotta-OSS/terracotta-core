/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tcsimulator;

import com.tc.process.LinkedJavaProcess;
import com.tcsimulator.distrunner.ServerSpec;

import java.io.File;
import java.util.Collection;
import java.util.HashSet;

public final class ProcessFactory {

  private final Sandbox    sandbox;
  private final ServerSpec serverSpec;

  public ProcessFactory(Sandbox sandbox, ServerSpec serverSpec) {
    this.sandbox = sandbox;
    this.serverSpec = serverSpec;
  }

  public LinkedJavaProcess newDSOJavaProcessInstance(String className, String[] args, boolean debug) {
    String[] argsToPass = args;
    if (argsToPass == null) {
      argsToPass = new String[] {};
    }
    LinkedJavaProcess newProcess = new LinkedJavaProcess(className, argsToPass);
    String fileSeparator = System.getProperty("file.separator");
    File terracottaDist = new File(sandbox.getTestHome().getAbsoluteFile(), sandbox.getDistributionName());
    File dsoJava = new File(terracottaDist.getAbsolutePath() + fileSeparator + "bin" + fileSeparator + "dso-java.sh");

    Collection jvmArgs = new HashSet();
    jvmArgs.add("-Dtc.config=" + serverSpec.getHostName() + ":" + serverSpec.getDsoPort());
    //jvmArgs.add("-Dtc.classloader.writeToDisk=true");
    if (debug) {
      jvmArgs.add("-Xdebug");
      jvmArgs.add("-Xrunjdwp:transport=dt_socket,address=8000,server=y,suspend=y");
    }
    Collection environment = new HashSet();
    if ("Mac OS X".equals(System.getProperty("os.name")) || "Linux".equals(System.getProperty("os.name"))) {
      environment.add("TC_JAVA_HOME=" + System.getProperty("java.home"));
    }
    newProcess.setJavaExecutable(dsoJava);
    File dir = new File(sandbox.getServerHome(), className);
    dir.mkdir();
    newProcess.setDirectory(dir);
    newProcess.setJavaArguments((String[]) jvmArgs.toArray(new String[jvmArgs.size()]));
    newProcess.setEnvironment((String[]) environment.toArray(new String[environment.size()]));

    return newProcess;
  }

  public LinkedJavaProcess newJavaProcessInstance(String className, String[] args, boolean debug, String javaHome) {
    String[] argsToPass = args;
    if (argsToPass == null) {
      argsToPass = new String[] {};
    }
    LinkedJavaProcess newProcess = new LinkedJavaProcess(className, argsToPass);
    File java = new File(javaHome);

    Collection jvmArgs = new HashSet();
    if (debug) {
      jvmArgs.add("-Xdebug");
      jvmArgs.add("-Xrunjdwp:transport=dt_socket,address=8000,server=y,suspend=y");
    }

    newProcess.setJavaExecutable(java);
    newProcess.setJavaArguments((String[]) jvmArgs.toArray(new String[jvmArgs.size()]));

    return newProcess;
  }

}

/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.control;

import org.apache.commons.io.IOUtils;

import com.tc.lcp.LinkedJavaProcess;
import com.tc.test.TestConfigObject;
import com.tc.util.Assert;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Collections;
import java.util.List;

public class ExtraL1ProcessControl extends ExtraProcessServerControl {

  private final Class        mainClass;
  private final List<String> mainArgs;
  private final File         directory;

  public ExtraL1ProcessControl(String l2Host, int dsoPort, Class mainClass, String configFileLoc,
                               List<String> mainArgs, File directory, List extraJvmArgs) {
    super(new DebugParams(), l2Host, dsoPort, 0, configFileLoc, true, extraJvmArgs);
    this.mainClass = mainClass;
    this.mainArgs = mainArgs == null ? Collections.EMPTY_LIST : mainArgs;
    this.directory = directory;

    setJVMArgs();
  }

  public ExtraL1ProcessControl(String l2Host, int dsoPort, Class mainClass, String configFileLoc,
                               List<String> mainArgs, File directory, List extraJvmArgs, boolean mergeOutput) {
    super(new DebugParams(), l2Host, dsoPort, 0, configFileLoc, mergeOutput, extraJvmArgs);
    this.mainClass = mainClass;
    this.mainArgs = mainArgs == null ? Collections.EMPTY_LIST : mainArgs;
    this.directory = directory;

    setJVMArgs();
  }

  @Override
  public File getJavaHome() {
    return javaHome;
  }

  @Override
  protected LinkedJavaProcess createLinkedJavaProcess() {
    LinkedJavaProcess out = super.createLinkedJavaProcess();
    out.setDirectory(this.directory);
    return out;
  }

  private void setJVMArgs() {
    try {
      String bootclasspath = "-Xbootclasspath/p:" + TestConfigObject.getInstance().normalBootJar();
      System.err.println("Bootclasspath:" + bootclasspath);
      this.jvmArgs.add("-Dtc.classpath=" + createTcClassPath());
      this.jvmArgs.add(bootclasspath);
      this.jvmArgs.add("-Dtc.config=" + super.configFileLoc);
    } catch (Exception e) {
      throw Assert.failure("Can't set JVM args", e);
    }
  }

  private String createTcClassPath() {
    File tcClassPathFile = new File(directory, "tc.classpath." + this.hashCode() + ".txt");
    FileOutputStream fos = null;
    try {
      fos = new FileOutputStream(tcClassPathFile);
      IOUtils.write(System.getProperty("java.class.path"), fos);
    } catch (Exception e) {
      throw new RuntimeException(e);
    } finally {
      IOUtils.closeQuietly(fos);
    }
    return tcClassPathFile.toURI().toString();
  }

  @Override
  protected String getMainClassName() {
    return mainClass.getName();
  }

  @Override
  protected List<String> getMainClassArguments() {
    return mainArgs;
  }

  @Override
  public boolean isRunning() {
    try {
      process.exitValue();
      return false;
    } catch (IllegalThreadStateException e) {
      System.out.println("Expected " + e.getMessage());
      return true;
    }
  }

  @Override
  public void attemptShutdown() throws Exception {
    // TODO:: comeback
    process.destroy();
  }
}

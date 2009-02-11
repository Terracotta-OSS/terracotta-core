/*
 * All content copyright (c) 2003-2009 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.process;

import org.apache.commons.io.IOUtils;

import com.tc.lcp.LinkedJavaProcess;
import com.tc.process.StreamCopier;
import com.tc.test.TestConfigObject;
import com.tc.util.Assert;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * This class will start a DSO client out of process
 * 
 * @author hhuynh
 */
public class ExternalDsoClient {
  private static final String CLIENT_CONFIG_FILENAME = "client-config.xml";

  private final File          clientLog;
  private final File          configFile;
  private final List          jvmArgs                = new ArrayList();
  private final List          args                   = new ArrayList();
  private final File          workingDir;
  private final Class         clientClass;
  private String              clientName;
  private FileOutputStream    logOutputStream;
  private LinkedJavaProcess   process;

  public ExternalDsoClient(File workingDir, InputStream configInput, Class clientClass) {
    this.workingDir = workingDir;
    this.clientLog = new File(workingDir, "dso-client.log");
    this.clientClass = clientClass;
    try {
      this.configFile = saveToFile(configInput);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    prepareTCJvmArgs();
  }

  private File saveToFile(InputStream configInput) throws IOException {
    File config = new File(workingDir, CLIENT_CONFIG_FILENAME);
    FileOutputStream out = new FileOutputStream(config);
    IOUtils.copy(configInput, out);
    out.close();
    return config;
  }

  public int startAndWait() throws IOException, InterruptedException {
    start();
    return process.waitFor();
  }

  public void start() throws IOException {
    logOutputStream = new FileOutputStream(clientLog);
    process = createLinkedJavaProcess();
    process.setJavaArguments((String[]) jvmArgs.toArray(new String[jvmArgs.size()]));
    process.start();
    StreamCopier outCopier = new StreamCopier(process.STDOUT(), logOutputStream);
    StreamCopier errCopier = new StreamCopier(process.STDERR(), logOutputStream);
    outCopier.start();
    errCopier.start();
    System.out.println(toString() + " started");
  }

  private LinkedJavaProcess createLinkedJavaProcess() {
    LinkedJavaProcess p = new LinkedJavaProcess(clientClass.getName(), (String[]) args.toArray(new String[0]));
    p.setDirectory(workingDir);
    return p;
  }

  public void stop() throws Exception {
    Assert.assertNotNull(process);
    Assert.assertNotNull(logOutputStream);
    if (isRunning()) {
      process.destroy();
    }
    if (isRunning()) {
      System.err.println(" WARNING: Dso client " + toString() + " process is still running after calling destroy()");
    }
    System.err.println("DSO client " + toString() + " stopped");
    IOUtils.closeQuietly(logOutputStream);
  }

  public boolean isRunning() {
    try {
      process.exitValue();
      return false;
    } catch (IllegalThreadStateException e) {
      return true;
    }
  }

  public int exitValue() {
    return process.exitValue();
  }

  public File getClientLog() {
    return clientLog;
  }

  public File getConfigFile() {
    return configFile;
  }

  public String toString() {
    return "DSO client " + (clientName != null ? clientName : clientClass.getName());
  }

  public List getJvmArgs() {
    return jvmArgs;
  }

  public List getArgs() {
    return args;
  }

  public void addJvmArg(String jvmarg) {
    jvmArgs.add(jvmarg);
  }

  public void addArg(String arg) {
    args.add(arg);
  }

  public void setClientName(String clientName) {
    this.clientName = clientName;
  }

  private void prepareTCJvmArgs() {
    try {
      String bootclasspath = "-Xbootclasspath/p:" + TestConfigObject.getInstance().normalBootJar();
      this.jvmArgs.add("-Dtc.classpath=" + createTcClassPath());
      this.jvmArgs.add(bootclasspath);
      this.jvmArgs.add("-Dtc.config=" + configFile);
    } catch (Exception e) {
      throw Assert.failure("Can't set JVM args", e);
    }
  }

  private String createTcClassPath() {
    File tcClassPathFile = new File(workingDir, "tc.classpath." + this.hashCode() + ".txt");
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
}
